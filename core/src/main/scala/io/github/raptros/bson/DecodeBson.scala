package io.github.raptros.bson

import com.mongodb.{BasicDBList, DBObject}
import scalaz._
import scalaz.syntax.id._
import scalaz.syntax.std.boolean._
import scalaz.syntax.validation._
import scala.reflect._
import java.util.Date
import org.joda.time.DateTime
import scalaz.std.list._
import scala.collection.JavaConverters._
import scala.collection.mutable

trait DecodeBson[+A] {

  def decode(dbo: DBObject): DecodeResult[A]

  def apply(dbo: DBObject): DecodeResult[A] = decode(dbo)

  def map[B](f: A => B): DecodeBson[B] = DecodeBson {
    apply(_) map f
  }

  def flatMap[B](f: A => DecodeBson[B]): DecodeBson[B] = DecodeBson { dbo =>
    apply(dbo) flatMap { r => f(r)(dbo) }
  }

  def orElse[B >: A](x: => DecodeBson[B]): DecodeBson[B] = DecodeBson { dbo =>
    apply(dbo) orElse x(dbo)
  }

  def |||[B >: A](x: => DecodeBson[B]): DecodeBson[B] = orElse(x)

  def &&&[B](x: DecodeBson[B]): DecodeBson[(A, B)] = DecodeBson { dbo =>
    for {
      a <- apply(dbo)
      b <- x(dbo)
    } yield (a, b)
  }

  def validate(f: DBObject => Boolean, msg: DBObject => DecodeError) = DecodeBson { dbo =>
    if (f(dbo))
      decode(dbo)
    else
      msg(dbo).wrapNel.left
  }

  def validateFields(count: Int) = validate(_.keySet.size == count,
    dbo => WrongFieldCount(count, dbo.keySet.size))
}

object DecodeBson extends DecodeBsons {
  def apply[A](f: DBObject => DecodeResult[A]): DecodeBson[A] = new DecodeBson[A] {
    def decode(dbo: DBObject): DecodeResult[A] = f(dbo)
  }
}

trait DecodeBsons extends GeneratedDecodeBsons {
  val ApD = Applicative[DecodeResult]

  type DecodeResultV[A] = ValidationNel[DecodeError, A]
  val ApV = Applicative[DecodeResultV]

  import scalaz.syntax.traverse._

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
