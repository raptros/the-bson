package io.github.raptros.bson

import scala.reflect.macros.blackbox.Context
import scala.language.experimental.macros

/** this object provides several macro implementations for automatically deriving encode and decode type classes for case classes.
  * they all work about the same way:
  *  - extract the main constructor for the class
  *  - find the generated encode/decode/codec method that has the same arity as that constructor (note that the macros fail if they can't find one)
  *  - find the companion object and refer to the apply or unapply method (note that case classes that use unapplySeq won't work)
  *  - construct the call to the coding method, using the names of the constructor parameters as the strings for the fields
  *  - use the types of the constructor params to pass in implicitly available [[EncodeBson]] and [[DecodeBson]] instances for the fields
  */
object BsonMacros {

  /** Derives a [[CodecBson]] for a case class by building a call to bsonCaseCodec`N`. Make sure to have Bson._ imported wherever you use this.
    * @tparam C a case class - i.e. it has a companion object with an apply and unapply method that each match the primary constructor's signature.
    * @return if C is a case class, a working CodecBson. if not, who knows.
    */
  def deriveCaseCodecBson[C]: CodecBson[C] = macro deriveCaseCodecBsonImpl[C]

  def deriveCaseCodecBsonImpl[C: c.WeakTypeTag](c: Context): c.Tree = {
    import c.universe._
    val tpe = weakTypeOf[C]
    val decls = tpe.decls
    val ctor = decls collectFirst {
      case m: MethodSymbol if m.isPrimaryConstructor => m
    } getOrElse {
      throw new IllegalArgumentException(s"can't find primary constructor for ${tpe}!")
    }
    val params = ctor.paramLists.head
    val keyNames = params map { p => p.name.decodedName.toString}
    val count = params.length
    val paramTypes = params map { p => tq"${p.typeSignature}"}

    val codecDecls = weakTypeOf[CodecBsons]
    val targetMethod = codecDecls.members.find { s =>
      s.isMethod && (s.name.decodedName.toString == s"bsonCaseCodec$count")
    } getOrElse {
      throw new IllegalArgumentException(s"could not find a bsonCaseCodec method for args count $count")
    }

    val typeSym = tq"${tpe.typeSymbol}"
    val companion = tpe.typeSymbol.companion
    val implicitlies = paramTypes flatMap { t =>
      List(q"scala.Predef.implicitly[EncodeBsonField[$t]]", q"scala.Predef.implicitly[DecodeBsonField[$t]]")
    }

    q"""
        io.github.raptros.bson.Bson.$targetMethod[..$paramTypes, $typeSym]($companion.apply, $companion.unapply)(..$keyNames)(..$implicitlies)
     """
  }

  /** Derives a [[EncodeBson]] for a case class by building a call to bencode`N`f. Make sure to have Bson._ imported wherever you use this.
    * @tparam C a case class - i.e. it has a companion object with an unapply method that is compatible with the primary constructor's signature.
    * @return if C is a case class, a working CodecBson. if not, who knows.
    */
  def deriveCaseEncodeBson[C]: EncodeBson[C] = macro deriveCaseEncodeBsonImpl[C]

  def deriveCaseEncodeBsonImpl[C: c.WeakTypeTag](c: Context): c.Tree = {
    import c.universe._

    val tpe = weakTypeOf[C]
    val decls = tpe.decls
    val ctor = decls collectFirst {
      case m: MethodSymbol if m.isPrimaryConstructor => m
    } getOrElse {
      throw new IllegalArgumentException(s"can't find primary constructor for ${tpe}!")
    }
    val params = ctor.paramLists.head
    val keyNames = params map { p => p.name.decodedName.toString}
    val count = params.length
    val paramTypes = params map { p => tq"${p.typeSignature}"}

    val encodeDecls = weakTypeOf[EncodeBsons]
    val targetMethod = encodeDecls.members.find { s =>
      s.isMethod && (s.name.decodedName.toString == s"bencode${count}f")
    } getOrElse {
      throw new IllegalArgumentException(s"could not find a bencode_f method for args count $count")
    }

    val typeSym = tq"${tpe.typeSymbol}"

    val extractor = q"(${tpe.typeSymbol.companion}.unapply _) andThen { _.get }"
    val implicitlies = paramTypes map { t =>
      q"scala.Predef.implicitly[EncodeBsonField[$t]]"
    }

    q"""
        io.github.raptros.bson.Bson.$targetMethod[$typeSym, ..$paramTypes]($extractor)(..$keyNames)(..$implicitlies)
     """
  }

  /** Derives a [[DecodeBson]] for a case class by building a call to bdecode`N`f. Make sure to have Bson._ imported wherever you use this.
    * @tparam C a case class - i.e. it has a companion object with an apply method that matches the primary constructor's signature.
    * @return if C is a case class, a working CodecBson. if not, who knows.
    */
  def deriveCaseDecodeBson[C]: DecodeBson[C] = macro deriveCaseDecodeBsonImpl[C]

  def deriveCaseDecodeBsonImpl[C: c.WeakTypeTag](c: Context): c.Tree = {
    import c.universe._
    val tpe = weakTypeOf[C]
    val decls = tpe.decls
    val ctor = decls collectFirst {
      case m: MethodSymbol if m.isPrimaryConstructor => m
    } getOrElse {
      throw new IllegalArgumentException(s"can't find primary constructor for ${tpe}!")
    }
    val typeSym = tq"${tpe.typeSymbol}"

    val params = ctor.paramLists.head
    val keyNames = params map { p => p.name.decodedName.toString}
    val count = params.length
    val paramTypes = params map { p => tq"${p.typeSignature}"}

    val decodeDecls = weakTypeOf[DecodeBsons]
    val targetMethod = decodeDecls.members.find { s =>
      s.isMethod && (s.name.decodedName.toString == s"bdecode${count}f")
    } getOrElse {
      throw new IllegalArgumentException(s"could not find a bdecode_f method for args count $count")
    }
    val implicitlies = paramTypes map { t =>
      q"scala.Predef.implicitly[DecodeBsonField[$t]]"
    }

    q"""
        io.github.raptros.bson.Bson.$targetMethod[..$paramTypes, $typeSym](${tpe.typeSymbol.companion}.apply)(..$keyNames)(..$implicitlies)
     """
  }
}
