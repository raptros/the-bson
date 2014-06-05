package io.github.raptros.bson

import com.mongodb.{BasicDBList, DBObject}

/** instances of this trait encode things to DBObjects.
  * @tparam A the type that this can encode. contravariant so that this can be used in any situation where a subtype of A needs to be encoded.
  */
trait EncodeBson[-A] {
  /** encodes an A into a mongo DBObject */
  def encode(a: A): DBObject

  /** alias for encode */
  def apply(a: A): DBObject = encode(a)

  /** derives a new encoder by transforming them into things this encoder applies to.
    * @param f a function that transforms a `B` into an `A`
    * @tparam B something that can be transformed.
    * @return an encoder for `B`
    */
  def contramap[B](f: B => A): EncodeBson[B] = EncodeBson(b => apply(f(b)))
}

object EncodeBson extends EncodeBsons {
  /** constructs an EncodeBson by using the passed-in function to implement `encode`
    * @tparam A the type to encode
    * @param f a function that turns `A`s into DBObjects
    * @return a new encoder
    */
  def apply[A](f: A => DBObject): EncodeBson[A] = new EncodeBson[A] {
    def encode(a: A) = f(a)
  }
}

/** contains:
  *  - at least one encoder implementation
  *  - methods named bencode(n)f (of arity n) that take a function that converts some type `X` into an
  *    n-tuple of things that have [[EncodeBsonField]] instances, along with the
  *    n field names to insert the encoded values with
  */
trait EncodeBsons extends GeneratedEncodeBsons {

  implicit def SeqEncodeBson[A](implicit e: EncodeBsonField[A]): EncodeBson[Seq[A]] =  EncodeBson { as =>
    val mutableDBList = new BasicDBList
    for ((a, idx) <- as.zipWithIndex) {
      e.writeTo(mutableDBList, idx.toString, a)
    }
    mutableDBList
  }
}
