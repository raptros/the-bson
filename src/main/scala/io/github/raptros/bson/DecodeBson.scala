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
}

object DecodeBson extends DecodeBsons {
  def apply[A](f: DBObject => DecodeResult[A]): DecodeBson[A] = new DecodeBson[A] {
    def decode(dbo: DBObject): DecodeResult[A] = f(dbo)
  }
}

trait DecodeBsons extends GeneratedDecodeBsons {
  def tryCast[A: ClassTag](dbo: DBObject): DecodeError \/ A = try {
    dbo.asInstanceOf[A].right
  } catch {
    case e: ClassCastException => WrongType(classTag[A].runtimeClass, dbo.getClass).left
  }

  implicit val dboDecodeBson: DecodeBson[DBObject] = DecodeBson { _.successNel }

  implicit val dbListDecodeBson: DecodeBson[BasicDBList] = DecodeBson { dbo =>
    tryCast[BasicDBList](dbo).validation.toValidationNel
  }

  implicit def listDecodeBson[A](implicit d: DecodeBsonField[A]) = DecodeBson { dbo =>
    dbListDecodeBson(dbo) flatMap { dbl =>
      dbl.keySet().asScala map { k => d(k, dbl) map { List(_) } } reduce { _ +++ _ }
    }
  }

}
