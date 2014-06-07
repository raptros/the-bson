package io.github.raptros.bson

import com.mongodb.DBObject
import java.util.Date
import org.bson.types.ObjectId
import org.joda.time.DateTime

/** these allow things of type `A` to be written into DBObjects as keyed values.
  * this is important because with BSON the only valid database objects are, well, objects.
  * @tparam A the type that can be encoded. contravariant for the same reason as [[EncodeBson]].
  */
trait EncodeBsonField[-A] {
  /** encodes and inserts `v` into `dbo` at key `k`.
    * @param dbo the DBObject to modify.
    * @param k the key to insert the value at.
    * @param v the value to encode.
    */
  def writeTo(dbo: DBObject, k: String, v: A): Unit

  /** alias for [[writeTo]] that also returns the modified object */
  def apply(dbo: DBObject, k: String, v: A): DBObject = {
    writeTo(dbo, k, v)
    dbo
  }

  /** derives a new field encoder by transforming other values into values this encoder can encode. */
  def contramap[B](f: B => A): EncodeBsonField[B] = EncodeBsonField { (dbo, k, b) =>
    writeTo(dbo, k, f(b))
  }
}


object EncodeBsonField extends EncodeBsonFields {
  /** constructs a new EncodeBsonField by implementing `writeTo` using the given function.
    * @tparam A the type to encode as a field
    * @param f a function that implements [[EncodeBsonField#writeTo]]
    * @return an [[EncodeBsonField]][A]
    */
  def apply[A](f: (DBObject, String, A) => Unit): EncodeBsonField[A] = new EncodeBsonField[A] {
    def writeTo(dbo: DBObject, k: String, a: A) = f(dbo, k, a)
  }
}

trait EncodeBsonFields {
  /** this allows anything that has an [[EncodeBson]] to also be encoded as a bson field. */
  implicit def EncodeBsonEncodeBsonField[A](implicit eb: EncodeBson[A]) = new EncodeBsonField[A] {
    def writeTo(dbo: DBObject, k: String, v: A): Unit = dbo.put(k, eb(v))
  }

  /** if something is [[directWritable]], the mongo java driver supports it as a directly inserted field.  */
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
  implicit val dboEncodeField: EncodeBsonField[DBObject] = directWritable[DBObject]

  implicit val dateTimeEncodeField: EncodeBsonField[DateTime] = dateEncodeField contramap { _.toDate }

  implicit def optionEncodeField[A](implicit e: EncodeBsonField[A]): EncodeBsonField[Option[A]] = EncodeBsonField { (dbo, k, optA) =>
    for (a <- optA) {
      e(dbo, k, a)
    }
  }
}
