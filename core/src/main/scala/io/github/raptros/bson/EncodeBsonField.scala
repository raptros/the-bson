package io.github.raptros.bson

import com.mongodb.DBObject
import java.util.Date
import org.bson.types.ObjectId
import org.joda.time.DateTime

trait EncodeBsonField[-A] {
  def apply(dbo: DBObject, k: String, v: A): DBObject = {
    writeTo(dbo, k, v)
    dbo
  }

  def writeTo(dbo: DBObject, k: String, v: A): Unit

  def contramap[B](f: B => A): EncodeBsonField[B] = EncodeBsonField { (dbo, k, b) =>
    writeTo(dbo, k, f(b))
  }
}


object EncodeBsonField extends EncodeBsonFields {
  def apply[A](f: (DBObject, String, A) => Unit): EncodeBsonField[A] = new EncodeBsonField[A] {
    def writeTo(dbo: DBObject, k: String, a: A) = f(dbo, k, a)
  }
}

trait EncodeBsonFields {

  implicit def EncodeBsonEncodeBsonField[A](implicit eb: EncodeBson[A]) = new EncodeBsonField[A] {
    def writeTo(dbo: DBObject, k: String, v: A): Unit = dbo.put(k, eb(v))
  }

  def directWritable[A]: EncodeBsonField[A] = new EncodeBsonField[A] {
    def writeTo(dbo: DBObject, k: String, v: A): Unit = dbo.put(k, v)
  }

  implicit val booleanEncodeField: EncodeBsonField[Boolean] = directWritable[Boolean]
  implicit val intEncodeField: EncodeBsonField[Int] = directWritable[Int]
  implicit val longEncodeField: EncodeBsonField[Long] = directWritable[Long]
  implicit val floatEncodeField: EncodeBsonField[Float] = directWritable[Float]
  implicit val doubleEncodeField: EncodeBsonField[Double] = directWritable[Double]
  implicit val dateEncodeField: EncodeBsonField[Date] = directWritable[Date]
  implicit val stringEncodeField: EncodeBsonField[String] = directWritable[String]
  implicit val objectIdEncodeField: EncodeBsonField[ObjectId] = directWritable[ObjectId]

  implicit val dateTimeEncodeField: EncodeBsonField[DateTime] = dateEncodeField contramap { _.toDate }

  implicit def optionEncodeField[A](implicit e: EncodeBsonField[A]): EncodeBsonField[Option[A]] = EncodeBsonField { (dbo, k, optA) =>
    for (a <- optA) {
      e(dbo, k, a)
    }
  }
}
