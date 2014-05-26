import sbt._
import scala.collection.immutable.IndexedSeq

object Boilerplate {
  val arities = 1 to 22
  val aritiesExceptOne = 2 to 22
  val arityChars: Map[Int, String] = {
    val seq = arities map { n => (n, ('A' + n - 1).toChar.toString)}
    seq.toMap
  }

  def write(path: File, fileContents: String): File = {
    IO.write(path, fileContents)
    path
  }

  def gen(dir : File) = {
//    val generatedDecodeJson = write(dir / "argonaut" / "GeneratedDecodeJsons.scala", genDecodeJsons)

    val generatedEncodeJson = write(dir / "raptros" / "bson" / "GeneratedEncodeBsons.scala", genEncodeBsons)

//    val generatedCodecJson = write(dir / "argonaut" / "GeneratedCodecJsons.scala", genCodecJsons)

//    Seq(generatedDecodeJson, generatedEncodeJson, generatedCodecJson)
    Seq(generatedEncodeJson)
  }

  def header =
    """|
      |package raptros.bson
      |
      |import scalaz.syntax.id._
      |import com.mongodb.{BasicDBObject, BasicDBList, DBObject}
      |
      |""".stripMargin

  def functionTypeParameters(arity: Int): String = (1 to arity) map { arityChars } mkString ", "

  def tupleFields(arity: Int): String = 1 to arity map { "x._".+ } mkString ", "

  def listPatternMatch(arity: Int): String = ((1 to arity).map(n => "c" + arityChars(n).toLowerCase).toList ::: "Nil" :: Nil).mkString(" :: ")

  def bsonStringParams(arity: Int): String = 1 to arity map { n => "%sk: String".format(arityChars(n).toLowerCase) } mkString ", "

  def bsonStringParamNames(arity: Int): String = 1 to arity map { n => "%sk".format(arityChars(n).toLowerCase) } mkString ", "


  def genEncodeBsons = {
    def encodeBsonParams(n: Int): String = (1 to n) map { n =>
      val char = arityChars(n)
      s"encode${char.toLowerCase}: EncodeBsonField[$char]"
    } mkString ", "

    def encodePuts(arity: Int): String = (1 to arity) map { n =>
      val char = arityChars(n).toLowerCase
      s"encode$char.writeTo(dbo, ${char}k, $char)"
    } mkString "\n        "

    def bencodefbody(arity: Int): String = {
      val tparams = functionTypeParameters(arity)
      val stringParams = bsonStringParams(arity)
      val encodeParams = encodeBsonParams(arity)
      val puts = encodePuts(arity)
      s"""|
          |  def bencode${arity}f[X, $tparams](fxn: X => ($tparams))($stringParams)(implicit $encodeParams): EncodeBson[X] =
          |    EncodeBson { x =>
          |      val (${tparams.toLowerCase}) = fxn(x)
          |      new BasicDBObject <| { dbo =>
          |        $puts
          |      }
          |    }
         """.stripMargin
    }

    def content = arities map { bencodefbody } mkString ""

    header + s"""|
        |trait GeneratedEncodeBsons {
        |  this: EncodeBsons =>
        |  $content
        |}
        |""".stripMargin
  }

}