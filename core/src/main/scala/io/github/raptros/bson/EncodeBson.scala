package io.github.raptros.bson

import com.mongodb.{BasicDBList, DBObject}

trait EncodeBson[-A] {
  def encode(a: A): DBObject

  def apply(a: A): DBObject = encode(a)

  def contramap[B](f: B => A): EncodeBson[B] = EncodeBson(b => apply(f(b)))

}

object EncodeBson extends EncodeBsons {
  def apply[A](f: A => DBObject): EncodeBson[A] = new EncodeBson[A] {
    def encode(a: A) = f(a)
  }
}

trait EncodeBsons extends GeneratedEncodeBsons {

  implicit def SeqEncodeBson[A](implicit e: EncodeBsonField[A]): EncodeBson[Seq[A]] =  EncodeBson { as =>
    val mutableDBList = new BasicDBList
    for ((a, idx) <- as.zipWithIndex) {
      e.writeTo(mutableDBList, idx.toString, a)
    }
    mutableDBList
  }
}
