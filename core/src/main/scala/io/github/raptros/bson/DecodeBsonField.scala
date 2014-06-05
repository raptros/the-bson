package io.github.raptros.bson

import com.mongodb.{BasicDBList, DBObject}
import scalaz._
import scalaz.syntax.id._
import scalaz.syntax.std.boolean._
import scalaz.std.option._
import scalaz.syntax.validation._
import scala.reflect._
import java.util.Date
import org.joda.time.DateTime


/** an instance of [[DecodeBsonField]] for a type A can extract an A from a field of a db object.
  */
trait DecodeBsonField[+A] {
  /** decodes an A from a field of a dbo.
    * @param k a key to look up in `dbo`
    * @param dbo a DBObject.
    * @return a DecodeResult with an A if successful.
    */
  def decode(k: String, dbo: DBObject): DecodeResult[A]

  /** alias for [[decode]] */
  def apply(k: String, dbo: DBObject): DecodeResult[A] = decode(k, dbo)

  /** derives a new [[DecodeBsonField]] by transforming decoded values */
  def map[B](f: A => B): DecodeBsonField[B] = DecodeBsonField {
    apply(_, _) map f
  }

  /** derives a new [[DecodeBsonField]] by passing a decoded value to a function that produces a DecodeBsonField which is then used to decode the field
    */
  def flatMap[B](f: A => DecodeBsonField[B]): DecodeBsonField[B] = DecodeBsonField { (k, dbo) =>
    apply(k, dbo) flatMap { r => f(r)(k, dbo) }
  }

  /** derives a new [[DecodeBsonField]] that first tries this one and then tries another one.
    * @param x the decoder to try after this one.
    * @tparam B a supertype of `A` that `x` produces
    * @return a new decoder.
    */
  def orElse[B >: A](x: => DecodeBsonField[B]): DecodeBsonField[B] = DecodeBsonField { (k, dbo) =>
    apply(k, dbo) orElse x(k, dbo)
  }

  /** alias for [[orElse]] */
  def |||[B >: A](x: => DecodeBsonField[B]): DecodeBsonField[B] = orElse(x)

  /** i'm not certain if this is useful. it produces a decoder that uses this decoder and a second one to produce a pair of values decoded from a single field.*/
  def &&&[B](x: DecodeBsonField[B]): DecodeBsonField[(A, B)] = DecodeBsonField { (k, dbo) =>
    DecodeBson.ApV.tuple2(apply(k, dbo).validation, x(k,dbo).validation).disjunction
  }
}

object DecodeBsonField extends DecodeBsonFields {
  /** constructs a [[DecodeBsonField]] from a function that implements [[DecodeBsonField#decode]]
    * @param f an implementation for decode
    * @tparam A what type it decodes
    * @return a DecodeBsonField for A
    */
  def apply[A](f: (String, DBObject) => DecodeResult[A]): DecodeBsonField[A] = new DecodeBsonField[A] {
    def decode(k: String, dbo: DBObject): DecodeResult[A] = f(k, dbo)
  }
}

trait DecodeBsonFields {

  protected def tryCast[A: ClassTag](k: String, v: Any): DecodeError \/ A =
    (v != null && !(classTag[A].runtimeClass isAssignableFrom v.getClass)) either WrongFieldType(k, classTag[A].runtimeClass, v.getClass) or v.asInstanceOf[A]

  protected def extractAndCast[A: ClassTag](k: String, dbo: DBObject): DecodeError \/ A = for {
    uncasted <- !(dbo containsField k) either NoSuchField(k) or dbo.get(k)
    casted <- tryCast[A](k, uncasted)
  } yield casted

  /** produces a decoder that attempts to cast the field to the target type. */
  def castableDecoder[A: ClassTag]: DecodeBsonField[A] = DecodeBsonField { (k, dbo) =>
    extractAndCast[A](k, dbo) leftMap { NonEmptyList(_) }
  }

  implicit val booleanDecodeField: DecodeBsonField[Boolean] = castableDecoder[java.lang.Boolean] map { _.booleanValue() }
  implicit val integerDecodeField: DecodeBsonField[Int] = castableDecoder[Integer] map { _.intValue() }
  implicit val stringDecodeField: DecodeBsonField[String] = castableDecoder[String]
  implicit val longDecodeField: DecodeBsonField[Long] = castableDecoder[java.lang.Long] map { _.intValue() }
  implicit val floatDecodeField: DecodeBsonField[Float] = castableDecoder[java.lang.Float] map { _.floatValue() }
  implicit val doubleDecodeField: DecodeBsonField[Double] = castableDecoder[java.lang.Double] map { _.doubleValue() }
  implicit val dateDecodeField: DecodeBsonField[Date] = castableDecoder[Date]

  implicit val datetimeDecodeField = castableDecoder[DateTime] ||| dateDecodeField map { d => new DateTime(d) }

  implicit val dboDecodeField: DecodeBsonField[DBObject] = castableDecoder[DBObject]

  /** this allows anything with a [[DecodeBson]] to be decoded from a field as well */
  implicit def decodeBsonDecodeField[A](implicit d: DecodeBson[A]): DecodeBsonField[A] = DecodeBsonField { (k, dbo) =>
    dboDecodeField(k, dbo) flatMap { d(_) }
  }

  import scalaz.std.list._
  implicit def listDecodeField[A](implicit d: DecodeBsonField[A]): DecodeBsonField[List[A]] = DecodeBsonField { (k, dbo) =>
    dbo.containsField(k) ?? (dboDecodeField(k, dbo) flatMap { DecodeBson.listDecodeBson[A].decode })
  }

  implicit def decodeOptionalField[A](implicit d: DecodeBsonField[A]): DecodeBsonField[Option[A]] = DecodeBsonField { (k, dbo) =>
    if (dbo.containsField(k)) d(k, dbo) map { Some(_) } else none[A].right
  }

}