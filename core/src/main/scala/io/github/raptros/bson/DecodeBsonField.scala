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


trait DecodeBsonField[+A] {
  def decode(k: String, dbo: DBObject): DecodeResult[A]

  def apply(k: String, dbo: DBObject): DecodeResult[A] = decode(k, dbo)

  def map[B](f: A => B): DecodeBsonField[B] = DecodeBsonField {
    apply(_, _) map f
  }

  def flatMap[B](f: A => DecodeBsonField[B]): DecodeBsonField[B] = DecodeBsonField { (k, dbo) =>
    apply(k, dbo) flatMap { r => f(r)(k, dbo) }
  }

  def orElse[B >: A](x: => DecodeBsonField[B]): DecodeBsonField[B] = DecodeBsonField { (k, dbo) =>
    apply(k, dbo) orElse x(k, dbo)
  }

  def |||[B >: A](x: => DecodeBsonField[B]): DecodeBsonField[B] = orElse(x)

  def &&&[B](x: DecodeBsonField[B]): DecodeBsonField[(A, B)] = DecodeBsonField { (k, dbo) =>
    for {
      a <- apply(k, dbo)
      b <- x(k, dbo)
    } yield (a, b)
  }
}

object DecodeBsonField extends DecodeBsonFields {
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