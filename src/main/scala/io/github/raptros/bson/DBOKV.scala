package io.github.raptros.bson

import com.mongodb.DBObject

case class DBOKV[A](k: String, v: A)(implicit encode: EncodeBsonField[A]) {
  def write(dbo: DBObject): DBObject = encode(dbo, k, v)
}
