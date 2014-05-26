package io.github.raptros.bson

import com.mongodb.{BasicDBObject, DBObject}

trait Builders {
  object DBO {
    def empty: DBObject= new BasicDBObject

    def apply(tuples: DBOKV[_]*): DBObject = (tuples foldRight empty) { _ write _ }
  }

  implicit class StringToDBOKV(k: String) {
    def ->[V: EncodeBsonField](v: V): DBOKV[V] = DBOKV(k, v)
  }

  case class DBOKV[V](k: String, v: V)(implicit encode: EncodeBsonField[V]) {
    def write(dbo: DBObject): DBObject = encode(dbo, k, v)
  }

  implicit class ValueToBson[A](a: A) {
    def asBson(implicit e: EncodeBson[A]): DBObject = e(a)
  }
}
