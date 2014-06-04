package io.github.raptros.bson
import Bson._

import scalaz._
import org.scalatest.{Matchers, FlatSpec}

class MacroTest extends FlatSpec with Matchers {

  case class Simple(name: String, something: Boolean, amount: Int)

  "a macro derived encode bson" should "encode" in {
    implicit def simpleEncode: EncodeBson[Simple] = BsonMacros.deriveCaseEncodeBson[Simple]

    val t1 = Simple("test 0", false, 12)
    val dbo = t1.asBson
    dbo.field[String]("name") shouldBe \/-("test 0")
    dbo.field[Boolean]("something") shouldBe \/-(false)
    dbo.field[Int]("amount") shouldBe \/-(12)
  }

  "a macro derived decode json" should "decode" in {
    implicit def simpleDecode: DecodeBson[Simple] = BsonMacros.deriveCaseDecodeBson[Simple]
    val dbo = DBO("name" :> "test 1", "something" :> true, "amount" :> 55)
    dbo.decode[Simple] shouldBe \/-(Simple("test 1", true, 55))
  }

  "a macro derived bson codec" should "encode and decode" in {
    implicit val simpleCodec: CodecBson[Simple] = BsonMacros.deriveCaseCodecBson[Simple]

    val t1 = Simple("test 2", true, 35)
    val asDBO = t1.asBson
    val t1decoded = asDBO.decode[Simple] valueOr { nel => fail(s"got errors! ${nel.list}")}
    t1decoded shouldBe t1
  }
}
