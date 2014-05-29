package io.github.raptros.bson
import Bson._

import org.scalatest.{Matchers, FlatSpec}

class MacroTest extends FlatSpec with Matchers {

  case class Simple(name: String, something: Boolean, amount: Int)

  implicit val simpleCodec: CodecBson[Simple] = BsonMacros.deriveCaseCodecBson[Simple]

//  implicit def simpleEncode: EncodeBson[Simple] = BsonMacros.deriveCaseEncodeBson[Simple]
//  implicit def simpleDecode: DecodeBson[Simple] = BsonMacros.deriveCaseDecodeBson[Simple]

  behavior of "a macro derived bson codec"

  it should "encode and decode" in {
    val t1 = Simple("test 1", true, 35)
    val asDBO = t1.asBson
    val t1decoded = asDBO.decode[Simple] valueOr { nel => fail(s"got errors! ${nel.list}") }
    t1decoded shouldBe t1
  }
}
