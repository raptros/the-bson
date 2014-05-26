package io.github.raptros.bson

import com.mongodb.{BasicDBObject, DBObject}

trait Builders {
  object DBO {
    def empty: DBObject= new BasicDBObject

    def apply(tuples: DBOKV[_]*): DBObject = (tuples foldRight empty) { _ write _ }
  }

  implicit class StringToDBOKV(k: String) {
    def :>[V: EncodeBsonField](v: V): DBOKV[V] = DBOKV(k, v)
    def :?>[V: EncodeBsonField](v: Option[V]): Option[DBOKV[V]] = v map { DBOKV(k, _) }
  }

  case class DBOKV[V](k: String, v: V)(implicit encode: EncodeBsonField[V]) {
    def write(dbo: DBObject): DBObject = encode(dbo, k, v)
  }

  implicit class ValueToBson[A](a: A) {
    def asBson(implicit e: EncodeBson[A]): DBObject = e(a)
  }

  implicit class DBOBuilder(dbo: DBObject) {
    def +@+[A](kv: DBOKV[A]): DBObject = kv.write(dbo)

    def +?+[A](okv: Option[DBOKV[A]]): DBObject = (okv fold dbo) { _.write(dbo) }
  }
}
