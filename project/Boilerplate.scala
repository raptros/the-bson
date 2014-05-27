import sbt._
import scala.collection.immutable.IndexedSeq

object Boilerplate {
  val arities = 1 to 12
  val aritiesExceptOne = 2 to 12
  val arityChars: Map[Int, String] = {
    val seq = arities map { n => (n, ('A' + n - 1).toChar.toString)}
    seq.toMap
  }

  def write(path: File, fileContents: String): File = {
    IO.write(path, fileContents)
    path
  }

  def gen(dir : File) = {
    val packageDir = dir / "io" / "github" / "raptros" / "bson"
    val generatedDecode = write(packageDir / "GeneratedDecodeBsons.scala", genDecodeBsons)

    val generatedEncode = write(packageDir / "GeneratedEncodeBsons.scala", genEncodeBsons)

    val generatedCodec = write(packageDir / "GeneratedCodecBsons.scala", genCodecs)

//    Seq(generatedDecodeJson, generatedEncodeJson, generatedCodecJson)
    Seq(generatedDecode, generatedEncode, generatedCodec)
  }

  def header =
    """
      |package io.github.raptros.bson
      |
      |import scalaz.syntax.id._
      |import com.mongodb.{BasicDBObject, BasicDBList, DBObject}
      |import scalaz._
      |
    """.stripMargin

  def functionTypeParameters(arity: Int): String = (1 to arity) map { arityChars } mkString ", "

  def tupleFields(arity: Int): String = 1 to arity map { "x._".+ } mkString ", "

  def listPatternMatch(arity: Int): String = ((1 to arity).map(n => "c" + arityChars(n).toLowerCase).toList ::: "Nil" :: Nil).mkString(" :: ")

  def bsonStringParams(arity: Int): String = 1 to arity map { n => "%sk: String".format(arityChars(n).toLowerCase) } mkString ", "

  def bsonStringParamNames(arity: Int): String = 1 to arity map { n => "%sk".format(arityChars(n).toLowerCase) } mkString ", "

  def encodeBsonParams(n: Int): String = (1 to n) map { n =>
    val char = arityChars(n)
    s"encode${char.toLowerCase}: EncodeBsonField[$char]"
  } mkString ", "

  def encodePuts(arity: Int): String = (1 to arity) map { n =>
    val char = arityChars(n).toLowerCase
    s"encode$char.writeTo(dbo, ${char}k, $char)"
  } mkString "\n" + (" " * 8)

  def bEncodeFBody(arity: Int): String = {
    val tparams = functionTypeParameters(arity)
    val stringParams = bsonStringParams(arity)
    val encodeParams = encodeBsonParams(arity)
    val puts = encodePuts(arity)
    s"""
       |  def bencode${arity}f[X, $tparams](fxn: X => ($tparams))($stringParams)(implicit $encodeParams): EncodeBson[X] =
       |    EncodeBson { x =>
       |      val (${tparams.toLowerCase}) = fxn(x)
       |      new BasicDBObject <| { dbo =>
       |        $puts
       |      }
       |    }
     """.stripMargin
  }

  def genEncodeBsons = {
    val content = arities map { bEncodeFBody } mkString ""
    s"""
       |$header
       |
       |trait GeneratedEncodeBsons { this: EncodeBsons =>
       |  $content
       |}
     """.stripMargin
  }

  def decodeBsonParams(n: Int): String = (1 to n) map { n =>
    val char = arityChars(n)
    s"decode${char.toLowerCase}: DecodeBsonField[$char]"
  } mkString ", "

  def decodeTargets(arity: Int): String = (1 to arity) map { n =>
    s"${arityChars(n).toLowerCase}Decode"
  } mkString ", "

  def decodeValidators(arity: Int): String = (1 to arity) map { n =>
    val char = arityChars(n).toLowerCase
    s"decode$char(${char}k, dbo)"
  } mkString ", "

  def bDecodeFBody(arity: Int): String = {
    val tparams = functionTypeParameters(arity)
    val stringParams = bsonStringParams(arity)
    val decodeParams = decodeBsonParams(arity)
    val validators = decodeValidators(arity)
    s"""
       |  def bdecode${arity}f[$tparams, X](fxn: ($tparams) => X)($stringParams)(implicit $decodeParams): DecodeBson[X] =
       |    DecodeBson { dbo =>
       |      ApD.apply$arity($validators)(fxn)
       |    }
     """.stripMargin
  }

  def genDecodeBsons = {
    //why except 1? because bdecode1f can't use the Applicative; it is instead implemented directly
    val content = aritiesExceptOne map { bDecodeFBody } mkString ""
    s"""
       |$header
       |
       |trait GeneratedDecodeBsons { this: DecodeBsons =>
       |
       |  $content
       |}
     """.stripMargin
  }

  def codecTParams(arity: Int): String = (1 to arity) map { n =>
    val char = arityChars(n)
    s"$char: EncodeBsonField: DecodeBsonField"
  } mkString ", "

  def untypedStringParams(arity: Int): String = (1 to arity) map { n =>
    s"${arityChars(n).toLowerCase}k"
  } mkString ", "

  def codecBody(arity: Int): String = {
    val tparams = functionTypeParameters(arity)
    val typeSig = codecTParams(arity)
    val stringParams = bsonStringParams(arity)
    val stringParamsList = untypedStringParams(arity)
    s"""
       |  def bsonCaseCodec$arity[$typeSig, X](f: ($tparams) => X, g: X => Option[($tparams)])($stringParams): CodecBson[X] =
       |    CodecBson(
       |      bencode${arity}f(g andThen { _.get })($stringParamsList).encode,
       |      bdecode${arity}f(f)($stringParamsList).decode
       |    )
     """.stripMargin
  }

  def genCodecs = {
    val content = arities map { codecBody } mkString ""
    s"""
       |$header
       |
       |import EncodeBson._
       |import DecodeBson._
       |
       |trait GeneratedCodecBsons { this: CodecBsons =>
       |  $content
       |}
     """.stripMargin
  }

}