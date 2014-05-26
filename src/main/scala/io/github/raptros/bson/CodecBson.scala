package io.github.raptros.bson

import com.mongodb.DBObject

sealed trait CodecBson[A] extends EncodeBson[A] with DecodeBson[A] {
  val encoder: EncodeBson[A]
  val decoder: DecodeBson[A]

  def encode(a: A): DBObject = encoder.encode(a)
  def decode(c: DBObject): DecodeResult[A] = decoder.decode(c)
}

object CodecBson extends CodecBsons {
  def apply[A](encoder: A => DBObject, decoder: DBObject => DecodeResult[A]): CodecBson[A] =
    derived(EncodeBson(encoder), DecodeBson(decoder))

  //no idea
//  def withReattempt[A](encoder: A => DBObject, decoder: DBObject => DecodeResult[A]): CodecBson[A] =
//    derived(EncodeBson(encoder), DecodeBson.withReattempt(decoder))

  def derived[A](implicit e: EncodeBson[A], d: DecodeBson[A]) =
    new CodecBson[A] {
      val encoder = e
      val decoder = d
    }
}

trait CodecBsons extends GeneratedCodecBsons


