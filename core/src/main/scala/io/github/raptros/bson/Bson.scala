package io.github.raptros.bson

/** instead of importing, you could mix in Bson. if anyone finds a good use case for that, let me know. */
trait Bson
  extends Extractors
  with Builders
  with EncodeBsonFields
  with EncodeBsons
  with DecodeBsonFields
  with DecodeBsons
  with CodecBsons

/** all of the features in `core` can be imported by importing Bson._; generally speaking that is what you want.
  *
  */
object Bson extends Bson