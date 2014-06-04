package io.github.raptros.bson

import org.scalatest.{GivenWhenThen, FlatSpec, Matchers}
import com.mongodb.{DBObject, BasicDBList, BasicDBObject}

import EncodeBson._
import EncodeBsonField._
import Builders._

class EncodeBsonSpec extends FlatSpec with Matchers with GivenWhenThen {


  behavior of "a handwritten encode bson"
  class Pants(val x: String, val y: Int, val z: List[String])
  implicit val pantsEncodeBson = EncodeBson[Pants] { pants =>
    DBO("x" :> pants.x, "y" :> pants.y, "z" :> pants.z)
  }

  it should "successfully write to a dbo object" in {
    val pants = new Pants("test-x", 344, List("one", "two"))

    val dbo = pants.asBson

    dbo.keySet() should contain only ("x", "y", "z")

    dbo.get("x") shouldBe pants.x
    dbo.get("y") shouldBe pants.y

    dbo.get("z") shouldBe a [BasicDBList]
    val z = dbo.get("z").asInstanceOf[BasicDBList]
    z.keySet() should contain only ("0", "1")
    z.get(0) shouldBe pants.z(0)
    z.get(1) shouldBe pants.z(1)
  }

  it should "be usable for writing to a field" in {
    val pants = new Pants("test-x", 344, List("one", "two"))
    val dbo: DBObject = new BasicDBObject()

    dbo.write("pants", pants)

    dbo.get("pants") shouldBe a [DBObject]
    val pantsDbo = dbo.get("pants").asInstanceOf[DBObject]


    pantsDbo.keySet() should contain only ("x", "y", "z")

    pantsDbo.get("x") shouldBe pants.x
    pantsDbo.get("y") shouldBe pants.y

    pantsDbo.get("z") shouldBe a [BasicDBList]
    val z = pantsDbo.get("z").asInstanceOf[BasicDBList]
    z.keySet() should contain only ("0", "1")
    z.get(0) shouldBe pants.z(0)
    z.get(1) shouldBe pants.z(1)
  }


  behavior of "a bencode_f derived EncodeBson"
  case class S3(x: Boolean, y: String, z: Double)

  implicit val encodeS3 = bencode3f(S3.unapply _ andThen (_.get))("x", "y", "z")

  it should "encode" in {
    val s = S3(false, "howdy", 55.4)
    val dbo = s.asBson
    dbo.keySet should contain only ("x", "y", "z")
    dbo.get("x") shouldBe s.x
    dbo.get("y") shouldBe s.y
    dbo.get("z") shouldBe s.z
  }


}
