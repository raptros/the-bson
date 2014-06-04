package io.github.raptros.bson

trait Bson
  extends Extractors
  with Builders
  with EncodeBsonFields
  with EncodeBsons
  with DecodeBsonFields
  with DecodeBsons
  with CodecBsons

object Bson extends Bson