package io.github.raptros.bson

object BsonMacros {

//  import scala.reflect.macros.Context
  import scala.reflect.macros.blackbox.Context
  import scala.language.experimental.macros

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
