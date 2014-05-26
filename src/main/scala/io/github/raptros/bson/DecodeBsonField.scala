package io.github.raptros.bson

import com.mongodb.{BasicDBList, DBObject}
import scalaz._
import scalaz.syntax.id._
import scalaz.syntax.std.boolean._
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

}

object DecodeBsonField extends DecodeBsonFields {
  def apply[A](f: (String, DBObject) => DecodeResult[A]): DecodeBsonField[A] = new DecodeBsonField[A] {
    def decode(k: String, dbo: DBObject): DecodeResult[A] = f(k, dbo)
  }
}

trait DecodeBsonFields {

  def tryCast[A: ClassTag](k: String, v: Any): DecodeError \/ A = try {
    v.asInstanceOf[A].right
  } catch {
    case e: ClassCastException => WrongFieldType(k, classTag[A].runtimeClass, v.getClass).left
  }

  def extractAndCast[A: ClassTag](k: String, dbo: DBObject): DecodeError \/ A = for {
    uncasted <- !(dbo containsField k) either NoSuchField(k) or dbo.get(k)
    casted <- tryCast[A](k, uncasted)
  } yield casted

  def castableDecoder[A: ClassTag]: DecodeBsonField[A] = DecodeBsonField { (k, dbo) =>
    extractAndCast[A](k, dbo) leftMap { NonEmptyList(_) }
  }

  implicit val booleanDecodeField: DecodeBsonField[Boolean] = castableDecoder[Boolean]
  implicit val intDecodeField: DecodeBsonField[Int] = castableDecoder[Int]
  implicit val stringDecodeField: DecodeBsonField[String] = castableDecoder[String]
  implicit val longDecodeField: DecodeBsonField[Long] = castableDecoder[Long]
  implicit val floatDecodeField: DecodeBsonField[Float] = castableDecoder[Float]
  implicit val doubleDecodeField: DecodeBsonField[Double] = castableDecoder[Double]
  implicit val dateDecodeField: DecodeBsonField[Date] = castableDecoder[Date]

  implicit val datetimeDecodeField = dateDecodeField map { d => new DateTime(d) }

  implicit val dboDecodeField: DecodeBsonField[DBObject] = castableDecoder[DBObject]

  implicit def decodeBsonDecodeField[A](implicit d: DecodeBson[A]): DecodeBsonField[A] = DecodeBsonField { (k, dbo) =>
    dboDecodeField(k, dbo) flatMap { d(_) }
  }

}