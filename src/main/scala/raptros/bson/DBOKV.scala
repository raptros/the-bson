package raptros.bson

import com.mongodb.{DBObject, BasicDBObjectBuilder}
import scalaz.syntax.id._

case class DBOKV[A](k: String, v: A)(implicit encode: EncodeBsonField[A]) {
  def write(dbo: DBObject): DBObject = encode(dbo, k, v)
}
