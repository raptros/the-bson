package io.github.raptros.bson

import org.scalatest.{GivenWhenThen, Matchers, FlatSpec}
import com.mongodb.{BasicDBList, DBObject, BasicDBObject}
import scalaz._

class DecodeBsonSpec extends FlatSpec with Matchers with GivenWhenThen {

  import DecodeBson._
  import DecodeBsonField._
  import Extractor._
  import EncodeBson._
  import EncodeBsonField._
  import Builders._

  behavior of "the provided DecodeBson instances"

  they should "decode a db object to itself" in {
    Given("a basic db object with some fields")
    val dbo: DBObject = DBO("one" :> 1, "two" :> 2, "three" :> "three")

    When("decode is applied to it")
    val decodedResult = dbo.decode[DBObject]

    Then("it should be successfully decoded and have the right values")
    decodedResult shouldBe 'right
    val decodedDbo = decodedResult getOrElse fail("wtf")
    decodedDbo.get("one") shouldBe 1
    decodedDbo.get("two") shouldBe 2
    decodedDbo.get("three") shouldBe "three"
  }

  they should "decode a list" in {
    Given("a basic db object list of ints")
    val dboL: DBObject = List(1, 3, 5).asBson

    When("it is decoded as a list of ints")
    val decodeResult = dboL.decode[List[Int]]

    Then("it should decode successfully and have the right values")
    val decodedList = decodeResult valueOr { errs => fail(s"had decode errors: $errs") }
    decodedList should contain inOrderOnly (1, 3, 5)

  }

  behavior of "the provided DecodeBson construction methods"

  they should "enable decoding a tuple" in {
    implicit val dec = bdecodeTuple3[String, Int, List[Int]]("one", "two", "three")

    val dbo = DBO("one" :> "thing", "two" :> 333, "three" :> List(1, 2, 3))
    val decoded = dbo.decode[(String, Int, List[Int])] valueOr { err => fail(s"decode errors $err") }
    decoded._1 shouldBe "thing"
    decoded._2 shouldBe 333
    decoded._3 should contain inOrderOnly (1, 2, 3)
  }

  case class S5(a: String, b: String, c: List[Int], d: Double, e: Option[String])
  they should "enable decoding a case class" in {
    implicit val dec = bdecode5f(S5.apply)("a", "b", "c", "d", "e")
    val dbo1 = DBO("a" :> "a1", "b" :> "b1", "c" :> List(1, 2, 5, 4, 5, 4), "d" :> 44.33, "e" :> "55555")
    val dbo2 = DBO("a" :> "a2", "b" :> "b2", "c" :> List.empty[Int], "d" :> 44.33, "e" :> "55555")
    val dbo3 = DBO("a" :> "a3", "b" :> "b3", "d" :> 44.33)
    val dbo4 = DBO("a" :> 33, "b" :> "b4")

    dbo1.decode[S5] shouldBe \/-(S5("a1", "b1", List(1,2,5,4,5,4), 44.33, Some("55555")))
    dbo2.decode[S5] shouldBe \/-(S5("a2", "b2", Nil, 44.33, Some("55555")))
    dbo3.decode[S5] shouldBe \/-(S5("a3", "b3", Nil, 44.33, None))
    val d4res = dbo4.decode[S5].swap.valueOr { v => fail(s"somehow decoded dbo4 as $v") }
    d4res.list should contain only (WrongFieldType("a", classOf[String], classOf[Integer]), NoSuchField("d"))
  }

  they should "collect up all the errors" in {
    implicit val dec = bdecodeTuple5[String, Int, Boolean, Long, String]("a", "b", "c", "d", "e")
    val dbo = DBO("a" :> true, "b" :> 5, "d" :> "fake")
    val failure = dbo.decode[(String, Int, Boolean, Long, String)].swap valueOr { x => fail(s"somehow decoded dbo to $x") }
    failure.list should contain only (WrongFieldType("a", classOf[String], classOf[java.lang.Boolean]), NoSuchField("c"), WrongFieldType("d", classOf[java.lang.Long], classOf[String]), NoSuchField("e"))
  }


  behavior of "the DecodeBson methods"
  they should "allow deriving via map" in {
    val dec = bdecodeTuple5[String, Int, Boolean, Long, String]("a", "b", "c", "d", "e")
    class Pairs(n1: String, v1: Int, n2: String, v2: Long) {
      def get1 = n1 -> v1
      def get2 = n2 -> v2
    }
    implicit val decP: DecodeBson[Pairs] = dec map {
      case (n1, v1, negate, v2, n2) => if (negate) new Pairs(n1, -v1, n2, -v2) else new Pairs(n1, v1, n2, v2)
    }
    val dbo1 = DBO("a" :> "p1", "b" :> 3, "c" :> false, "d" :> 44l, "e" :> "p2")
    val dbo2 = DBO("a" :> "p1", "b" :> 4, "c" :> true, "d" :> 22l, "e" :> "p2")
    val ps1 = dbo1.decode[Pairs] valueOr { err => fail(s"error: $err") }
    val ps2 = dbo2.decode[Pairs] valueOr { err => fail(s"error: $err") }
    ps1.get1 shouldBe ("p1" -> 3)
    ps1.get2 shouldBe ("p2" -> 44l)
    ps2.get1 shouldBe ("p1" -> -4)
    ps2.get2 shouldBe ("p2" -> -22l)
  }


  sealed trait Something
  case class X1(l: String, r: Boolean) extends Something
  case class X2(p: List[Int]) extends Something

  they should "allow deriving via orElse" in {
    implicit val dec: DecodeBson[Something] = bdecode2f(X1.apply)("l", "r") orElse bdecode1f(X2.apply)("p")
    val dbo1 = DBO("l" :> "hi", "r" :> true)
    val dbo2 = DBO("p" :> List(1, 7, 3))
    val dbo3 = DBO("l" :> "hi", "p" :> List(1, 7, 3))
    val dbo4 = DBO("l" :> "hi", "p" :> List("2", "9"))

    dbo1.decode[Something] shouldBe \/-(X1("hi", true))
    dbo2.decode[Something] shouldBe \/-(X2(List(1, 7, 3)))
    info("this is an odd case, but")
    info(dbo3.decode[Something].toString)
    dbo3.decode[Something] shouldBe \/-(X2(List(1, 7, 3)))
    info(dbo4.decode[Something].toString)
    dbo4.decode[Something] shouldBe -\/(NonEmptyList(WrongFieldType("0", classOf[Integer], classOf[String]), WrongFieldType("1", classOf[Integer], classOf[String])))
  }

  they should "allow requiring field counts" in {
    val dec1 = bdecode2f(X1.apply)("l", "r")
    val dec2 = bdecode1f(X2.apply)("p").validateFields(1)
    implicit val dec: DecodeBson[Something] = dec1 ||| dec2

    val dbo3 = DBO("l" :> "hi", "p" :> List(1, 7, 3))
    dbo3.decode[Something] shouldBe  -\/(NonEmptyList(WrongFieldCount(1, 2)))

  }
}
