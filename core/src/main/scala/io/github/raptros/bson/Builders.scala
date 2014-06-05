package io.github.raptros.bson

import com.mongodb.{BasicDBObject, DBObject}

/** Builders are all the things you need to build up DBObjects using available [[EncodeBson]]s and [[EncodeBsonField]]s. */
trait Builders {

  /** apply this to a bunch of [[DBOKV]]s and get a DBObject.
    * {{{
    * DBO("k0" :> true, "k1" :> List(1, 2, 3), k3 :> "a string")
    * }}}
    */
  object DBO {
    def empty: DBObject= new BasicDBObject

    def apply(tuples: DBOKV[_]*): DBObject = (tuples foldRight empty) { _ write _ }
  }

  /** this enables a syntax for constructing key-value pairs with values that can be written to a DBObject -
    * i.e. because the value type has a [[EncodeBsonField]] instance
    * {{{
    * "string" :> <some encodable>
    * }}}
    * also, you can get an Option of a DBOKV:
    * {{{
    *   "string" :> Some(<encodable>) //gives you Some(DBOKV("string", <encodable>))
    *   "string" :> None //gives you None
    * }}}
    */
  implicit class StringToDBOKV(k: String) {
    def :>[V: EncodeBsonField](v: V): DBOKV[V] = DBOKV(k, v)
    def :?>[V: EncodeBsonField](v: Option[V]): Option[DBOKV[V]] = v map { DBOKV(k, _) }
  }

  /** wraps up a key and a value that can be encoded as a field along with the [[EncodeBsonField]] that will encode it.
    * used by the syntax provided by [[StringToDBOKV]].
   */
  case class DBOKV[V](k: String, v: V)(implicit encode: EncodeBsonField[V]) {
    /** uses the encoder for V to write k and v to the passed in `DBObject`, returning the same instance. */
    def write(dbo: DBObject): DBObject = encode(dbo, k, v)
  }

  /** allows you to call asBson on any value that some [[EncodeBson]] instance applies to. */
  implicit class ValueToBson[A](a: A) {
    def asBson(implicit e: EncodeBson[A]): DBObject = e(a)
  }

  /** this permits a syntax for adding key-value pairs to `DBObject`s.
    * @note the methods given by this class mutate the underlying `DBObject`.
    *       you should really only construct a `DBObject` right near where you will use it -
    *       don't let those mutable, untyped things spread around your codebase.
    */
  implicit class DBOBuilder(dbo: DBObject) {
    /** directly appends a key and value to the dbo, with a Unit return type; no fancy syntax here. */
    def write[A](k: String, v: A)(implicit f: EncodeBsonField[A]): Unit = f.writeTo(dbo, k, v)

    /** this appends a [[io.github.raptros.bson.Builders.DBOKV]] to a dbo, and returns the same dbo.
      * {{{
      * dbo +@+ ("k0" :> <encodable1>) +@+ ("k1" :> <encodable2>)
      * }}}
      */
    def +@+[A](kv: DBOKV[A]): DBObject = kv.write(dbo)

    /** this appends multiple [[DBOKV]]s to a dbo.
      * {{{
      *   dbo ++@++ List("k0" :> <encodable1>, "k1" :> <encodable2>) +@+ ("k2" :> <encodable2>)
      * }}}
      */
    def ++@++(kvs: Seq[DBOKV[_]]): DBObject = {
      kvs foreach { _.write(dbo) }
      dbo
    }

    /** this optionally appends optional [[DBOKV]]s to a dbo.
      * {{{
      * //this will end up adding the keys k0 (with the encoded value of encodable0) and k1, but not the key k1.
      * dbo +?+ ("k0" :?> Some(<encodable0>)) +?+ ("k1" :?> None) +@+ ("k2" :> <encodable1>)
      * }}}
      */
    def +?+[A](okv: Option[DBOKV[A]]): DBObject = (okv fold dbo) { _.write(dbo) }
  }
}

object Builders extends Builders
