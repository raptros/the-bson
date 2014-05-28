package io.github.raptros.bson

object BsonMacros {

//  import scala.reflect.macros.Context
  import scala.reflect.macros.blackbox.Context
  import scala.language.experimental.macros


  def deriveCaseCodecBsonImpl[C: c.WeakTypeTag](c: Context): c.Expr[CodecBson[C]] = {
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
    val codecDecls = weakTypeOf[CodecBsons]
    val paramTypes = params map { p => tq"${p.typeSignature}"}

    val targetMethod = codecDecls.members.find { s =>
      s.isMethod && (s.name.decodedName.toString contains s"bsonCaseCodec$count")
    } getOrElse {
      throw new IllegalArgumentException(s"could not find a bsonCaseCodec method for args count $count")
    }

    val companion = tpe.companion
    val apply = companion.decls find { d => d.name.decodedName.toString == "apply"} getOrElse {
      throw new IllegalArgumentException(s"could not find apply in $companion!")
    }

    val unapply = companion.decls find { d => d.name.decodedName.toString == "unapply"} getOrElse {
      throw new IllegalArgumentException(s"could not find unapply in $companion!")
    }
//    companion.asModule.moduleClass

    val paramsAsTerms = keyNames map { kn => TermName(kn)}
    val typedParams = (paramsAsTerms zip paramTypes) map {
      case (trm, tp) => q"val $trm: $tp"
    }

    val typeSym = tq"${tpe.typeSymbol}"
    val applier = q"(..$typedParams) => ${companion.typeSymbol}.apply(..$paramsAsTerms)"
//
    val unapplier = q"(x: $typeSym) => ${companion.typeSymbol}.unapply(x)"
//        val args = List(applier, unapplier) :: ( keyNames map { k => q"$k" } ) :: Nil

//    val args = List(q"$companion.apply", q"$companion.unapply") :: (keyNames map { k => q"$k"}) :: Nil
//    val targetType = tq"${tpe.typeSymbol}"


    c.Expr[CodecBson[C]] {
      q"$targetMethod[..$paramTypes, $typeSym]($applier, $unapplier)(..$keyNames)"
    }
    /*
    c.Expr[CodecBson[C]] {
      Apply(
        Apply(
          Ident(targetMethod),
          List(
            Select(Ident(companion), TermName("apply")),
            Select(Ident(companion), TermName("unapply")
            )
          )
        ),
        keyNames map { k => Literal(Constant(k))}
      )
    }*/
  }


  def deriveCaseCodecBson[C]: CodecBson[C] = macro deriveCaseCodecBsonImpl[C]

}
