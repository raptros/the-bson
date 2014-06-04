package io.github.raptros.bson

import org.scalatest.{GivenWhenThen, Matchers, FlatSpec}
import com.mongodb.DBObject
import org.joda.time.DateTime
import scalaz._
import java.util.Date
import org.scalatest.matchers.{HavePropertyMatchResult, HavePropertyMatcher, MatchResult, Matcher}

import DecodeBsonField._
import Extractors._
import EncodeBsonField._
import Builders._


class DecodeBsonFieldSpec extends FlatSpec with Matchers with GivenWhenThen {
  behavior of "the provided DecodeBsonField instances"

  they should "decode the basic types" in {
    val dbo: DBObject = DBO("string" :> "example", "int" :> 34, "double":> 52.6d, "long" :> 8888l, "boolean" :> true)
    dbo.field[String]("string") shouldBe \/-("example")
    dbo.field[Int]("int") shouldBe \/-(34)
    dbo.field[Double]("double") shouldBe \/-(52.6d)
    dbo.field[Long]("long") shouldBe \/-(8888l)
    dbo.field[Boolean]("boolean") shouldBe \/-(true)
  }
  they should "not extract a field that is not present" in {
    val dbo: DBObject = DBO("string" :> "example", "int" :> 34, "double":> 52.6d, "long" :> 8888l, "boolean" :> true)
    dbo.field[Date]("date") shouldBe -\/(NonEmptyList(NoSuchField("date")))
  }

  they should "not decode to a wrong type" in {
    val dbo: DBObject = DBO("string" :> "example", "int" :> 34, "double":> 52.6d, "long" :> 8888l, "boolean" :> true)
    val errs1 = dbo.field[Long]("string").swap valueOr {v => fail(s"somehow successfully decoded $v ")}
    errs1.tail shouldBe empty
    errs1.head shouldBe a [WrongFieldType]
    errs1.head.asInstanceOf[WrongFieldType] should have (
      field("string"),
      wanted(classOf[java.lang.Long]),
      got(classOf[String])
    )

    val errs2 = dbo.field[Int]("double").swap valueOr {v => fail(s"somehow successfully decoded $v ")}
    errs2.tail shouldBe empty
    errs2.head shouldBe a [WrongFieldType]
    errs2.head.asInstanceOf[WrongFieldType] should have (
      field("double"),
      wanted(classOf[java.lang.Integer]),
      got(classOf[java.lang.Double])
    )
  }

  def field(expected: String) = HavePropertyMatcher[WrongFieldType, String] { wft =>
    HavePropertyMatchResult(expected === wft.field, "field", expected, wft.field)
  }

  def wanted(expected: Class[_]) = HavePropertyMatcher[WrongFieldType, Class[_]] { wft =>
    HavePropertyMatchResult(expected === wft.wanted, "wanted", expected, wft.wanted)
  }

  def got(expected: Class[_]) = HavePropertyMatcher[WrongFieldType, Class[_]] { wft =>
    HavePropertyMatchResult(expected === wft.got, "got", expected, wft.got)
  }

  they should "be able to handle an optional field" in {
    val dbo1: DBObject = DBO("this" :> 33)
    val dbo2: DBObject = DBO("this" :> 33, "that" :> 34)

    val d1 = dbo1.field[Option[Int]]("that")
    d1 shouldBe \/-(None)

    val d2 = dbo2.field[Option[Int]]("that")
    d2 shouldBe \/-(Some(34))
  }

  they should "allow fieldOpt[A] to have the same effect as field[Option[A]]" in {
    val dbo1: DBObject = DBO("this" :> 33)
    val dbo2: DBObject = DBO("this" :> 33, "that" :> 34)

    val d1 = dbo1.fieldOpt[Int]("that")
    d1 shouldBe \/-(None)

    val d2 = dbo2.fieldOpt[Int]("that")
    d2 shouldBe \/-(Some(34))
  }
}
