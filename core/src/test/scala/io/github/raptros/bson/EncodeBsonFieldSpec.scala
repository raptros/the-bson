package io.github.raptros.bson

import org.scalatest.{GivenWhenThen, Matchers, FlatSpec}
import scalaz.syntax.std.option._
import scalaz.std.option._
import com.mongodb.{DBObject, BasicDBList, BasicDBObject}
import org.bson.types.ObjectId
import org.joda.time.DateTime

import EncodeBsonField._
import Builders._

class EncodeBsonFieldSpec extends FlatSpec with Matchers with GivenWhenThen {
  behavior of "the provided EncodeBsonField instances"

  they should "directly encode any basic values" in {
    Given("a DBObject")
    val dbo = new BasicDBObject()

    When("writing several values to it")
    dbo.write("boolean", true)
    dbo.write("double", 3.45: Double)
    dbo.write("string", "something")
    val objectId = new ObjectId()
    dbo.write("objectId", objectId)

    Then("those values should present and castable")
    dbo.keySet() should contain only ("boolean", "double", "string", "objectId")
    dbo.getBoolean("boolean") shouldBe true
    dbo.getDouble("double") shouldBe 3.45
    dbo.getString("string") shouldBe "something"
    dbo.getObjectId("objectId") shouldBe objectId
  }

  they should "encode a date time as a date" in {
    val dbo = new BasicDBObject()
    val now = DateTime.now()
    dbo.write("now", now)
    dbo.getDate("now") shouldBe now.toDate
  }

  they should "derive a list field encoder from EncodeBsons" in {
    val dbo = new BasicDBObject()
    val l = Vector("one", "two", "three")
    dbo.write("l", l)
    val dbl = dbo.get("l").asInstanceOf[BasicDBList]
    dbl.keySet() should contain only ("0", "1", "2")
    dbl.get(0) shouldBe "one"
    dbl.get(1) shouldBe "two"
    dbl.get(2) shouldBe "three"
  }

  they should "allow optional field encoding" in {
    val dbo = new BasicDBObject()
    val o1 = "howdily".some
    val o2 = none[String]
    dbo.write("k1", o1)
    dbo.write("k2", o2)

    dbo.keySet should contain only "k1"
    dbo.get("k1") shouldBe "howdily"
  }


  behavior of "EncodeBsonField in the DBOKV/DBOBuilder/DBO() context"

  it should "allow construction via DBO()" in {
    val dbo = DBO("one" :> 344, "zerg" :> true, "gl" :> List(2, 5, 6))

    dbo.keySet() should contain only ("one", "gl", "zerg")
    dbo.get("one") shouldBe 344
    dbo.get("zerg") shouldBe true
    val dbl = dbo.get("gl").asInstanceOf[BasicDBList]
    dbl.keySet() should contain only ("0", "1", "2")
    dbl.get(0) shouldBe 2
    dbl.get(1) shouldBe 5
    dbl.get(2) shouldBe 6
  }

  it should "allow construction via +@+" in {
    val dbo = DBO.empty +@+ ("yo" :> "dawg")
    dbo.keySet() should contain only "yo"
    dbo.get("yo") shouldBe "dawg"
  }

  it should "allow construction via ++@++" in {
    val dbo = DBO("yo" :> "dawg") ++@++ List("we" :> true, "heard" :> 3, "you" :> "like", "objects" :> List("so", "we"))
    dbo.keySet() should contain only ("yo", "we", "heard", "you", "objects")
    dbo.get("you") shouldBe "like"
    dbo.get("heard") shouldBe 3
  }

  it should "allow construction via +?+" in {
    val dbo = DBO.empty +?+ ("in" :?> 44.some) +?+ ("out" :?> none[String]) +?+ ("alsoIn" :?> true.some)
    dbo.keySet() should contain only ("in", "alsoIn")
    dbo.get("in") shouldBe 44
    dbo.get("alsoIn") shouldBe true

  }

  it should "allow embedding DBOs inside DBOs" in {
    val dbo = DBO("embed" :> DBO("embedded" :> true, "other" :> "something"))
    dbo.keySet() should contain only "embed"
    val embedded = dbo.get("embed").asInstanceOf[DBObject]
    embedded.keySet() should contain only ("embedded", "other")
    embedded.get("embedded") shouldBe true
    embedded.get("other") shouldBe "something"
  }
}
