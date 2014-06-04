package io.github.raptros.bson

import org.scalatest.{FlatSpec, Matchers, GivenWhenThen}
import Bson._
import scalaz._
import org.joda.time.DateTime

class CodecBsonSpec extends FlatSpec with Matchers with GivenWhenThen {


  case class ThreePart(x: String, y: List[Int], z: DateTime)

  behavior of "a codec for threepart"

  implicit val codecThreePart = bsonCaseCodec3(ThreePart.apply, ThreePart.unapply)("x", "y", "z")

  it should "encode and decode a standard one" in {
    val example = ThreePart("hi", List(4, 3), z = DateTime.now())
    val dbo = example.asBson
    dbo.keySet should contain only ("x", "y", "z")
    dbo.decode[ThreePart] shouldBe \/-(example)
  }

  it should "encode and decode one with an empty list" in {
    val example = ThreePart("hi", Nil, z = DateTime.now())
    val dbo = example.asBson
    dbo.keySet should contain only ("x", "y", "z")
    dbo.decode[ThreePart] shouldBe \/-(example)
  }

  case class WithOpt(x: Option[String])

  behavior of "a codec for withopt"
  implicit val codecWithOpt = bsonCaseCodec1(WithOpt.apply, WithOpt.unapply)("x")
  it should "encode an empty one" in {
    val dbo = WithOpt(None).asBson
    dbo.keySet shouldBe empty
  }

  it should "encode a non empty one" in {
    val dbo = WithOpt(Some("hi")).asBson
    dbo.keySet should contain only "x"
    dbo.get("x") shouldBe "hi"
  }

  it should "decode an empty DBO" in {
    DBO().decode[WithOpt] shouldBe \/-(WithOpt(None))
  }

  it should "decode a non-empty one" in {
    DBO("x" :> "hi").decode[WithOpt] shouldBe \/-(WithOpt(Some("hi")))
  }

}
