package io.github.raptros.bson

import com.mongodb.{BasicDBList, DBObject}
import scalaz._
import scalaz.syntax.id._
import scalaz.syntax.std.boolean._
import scalaz.syntax.validation._
import scalaz.syntax.traverse._
import scala.reflect._
import org.joda.time.DateTime
import scalaz.std.list._

/** the typeclass of things that can be decoded from DBObjects.
  * @tparam A not certain what effect the covariance on A has.
  */
trait DecodeBson[+A] {

  /** this is the method that needs to be implemented to decode.
    * @param dbo the object to attempt to decode an A from.
    * @return a [[DecodeResult]] with A as the success.
    */
  def decode(dbo: DBObject): DecodeResult[A]

  /** an alias for [[decode]] */
  def apply(dbo: DBObject): DecodeResult[A] = decode(dbo)

  /** derives a new DecodeBson from this one by transforming the values decoded from this one
    * @param f a function that transforms values produced by this DecodeBson
    * @tparam B the type that `f` returns
    * @return a DecodeBson for `B`
    */
  def map[B](f: A => B): DecodeBson[B] = DecodeBson {
    apply(_) map f
  }

  /** derives a new DecodeBson by transforming decoded values using a function that produces a new DecodeBson.
    * @param f a function that produces a DecodeBson using the values produced by this one.
    *          the decoder returned by f will be applied to to the same dbo that this decoder extracted the value it passed to `f` from.
    *          the value returned by applying that new decoder is the value that the new decoder will return.
    * @tparam B a type
    * @return a decooder for B
    */
  def flatMap[B](f: A => DecodeBson[B]): DecodeBson[B] = DecodeBson { dbo =>
    apply(dbo) flatMap { r => f(r)(dbo) }
  }

  /** creates a decoder that first uses this decoder and then another decoder. has an alias [[|||]].
    * @param x a decoder that will be tried if this one fails
    * @tparam B a supertype of A that is common to both this decoder and `x`. this is the return type of the new decoder.
    * @return a decoder that returns the first successfully decoded value
    */
  def orElse[B >: A](x: => DecodeBson[B]): DecodeBson[B] = DecodeBson { dbo =>
    apply(dbo) orElse x(dbo)
  }

  /** alias for [[orElse]] */
  def |||[B >: A](x: => DecodeBson[B]): DecodeBson[B] = orElse(x)

  /** creates a decoder that applies this decoder and `x` and returns both results together. */
  def &&&[B](x: DecodeBson[B]): DecodeBson[(A, B)] = DecodeBson { dbo =>
    for {
      a <- apply(dbo)
      b <- x(dbo)
    } yield (a, b)
  }

  /** creates a decoder that applies a predicate to a dbo before it tries to apply this decoder.
    * @param f a predicate on DBObjects.
    * @param msg a function that takes the DBObject that failed the predicate, and produces a [[DecodeError]] explaining what went wrong.
    */
  def validate(f: DBObject => Boolean, msg: DBObject => DecodeError) = DecodeBson { dbo =>
    if (f(dbo))
      decode(dbo)
    else
      msg(dbo).wrapNel.left
  }

  /** creates a decoder that only applies this decoder if the dbo has exactly `count` fields */
  def validateFields(count: Int) = validate(_.keySet.size == count,
    dbo => WrongFieldCount(count, dbo.keySet.size))
}

object DecodeBson extends DecodeBsons {
  /** constructs [[DecodeBson]][A] by using `f` as the implementation of `decode` */
  def apply[A](f: DBObject => DecodeResult[A]): DecodeBson[A] = new DecodeBson[A] {
    def decode(dbo: DBObject): DecodeResult[A] = f(dbo)
  }
}

/** this contains:
  *  - a couple DecodeBson instances
  *  - two Applicative implementations (one for DecodeResult, one for the [[scalaz.Validation]] equivalent)
  *  - a bunch of bdecode(n)f methods, which construct decoders from the combination of function that constructs a value from types that have
  *    [[DecodeBsonField]] instances, and a list of field names to extracted. n is the arity.
  *  - a bunch of bdecodeTuple(n) methods, which constructed decoders that extract the named fields into a tuple.
  */
trait DecodeBsons extends GeneratedDecodeBsons {
  val ApD = Applicative[DecodeResult]

  type DecodeResultV[A] = ValidationNel[DecodeError, A]
  val ApV = Applicative[DecodeResultV]


  import scala.reflect._
  protected def tryCast[A: ClassTag](v: Any): DecodeError \/ A =
    (v != null && !(classTag[A].runtimeClass isAssignableFrom v.getClass)) either WrongType(classTag[A].runtimeClass, v.getClass) or v.asInstanceOf[A]

  implicit val dboDecodeBson: DecodeBson[DBObject] = DecodeBson { _.right }

  implicit def listDecodeBson[A](implicit d: DecodeBsonField[A]) = DecodeBson { dbo =>
    tryCast[BasicDBList](dbo) leftMap { NonEmptyList(_) } flatMap { dbl =>
      //first: decode each item in the list
      val decodes = (0 until dbl.size()) map { idx => d(idx.toString, dbl).validation }
      //second: sequence the decode results - takes a list of decode results and makes it a decode result of a list
      decodes.toList.sequence[DecodeResultV, A].disjunction
    }
  }

  def bdecode1f[A, X](fxn: (A) => X)(ak: String)(implicit decodea: DecodeBsonField[A]): DecodeBson[X] =
    DecodeBson { dbo => decodea(ak, dbo) map fxn }
}
