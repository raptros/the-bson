package io.github.raptros.bson

object BsonMacros {

  import scala.reflect.macros.Context
  import scala.language.experimental.macros


  def deriveCaseCodecBsonImpl[C: c.WeakTypeTag](c: Context): c.Expr[CodecBson[C]] = {
    import c.universe._
    val tpe = weakTypeOf[C]
    val decls = tpe.declarations
    val ctor = decls collectFirst {
      case m: MethodSymbol if m.isPrimaryConstructor => m
    } getOrElse {
      throw new IllegalArgumentException(s"can't find primary constructor for ${tpe}!")
    }
    val params = ctor.paramss.head
    val keyNames = params map { p => p.name.decoded }
    val count = params.length
    val codecDecls = weakTypeOf[CodecBsons]
    val paramTypes = params map { p => tpe.declaration(p.name).typeSignature }

    val targetMethod = codecDecls.members.find { s =>
      s.isMethod && (s.name.decoded contains s"bsonCaseCodec$count")
    } getOrElse {
      throw new IllegalArgumentException(s"could not find a bsonCaseCodec method for args count $count")
    }

    val companion = tpe.typeSymbol.companionSymbol
    companion.asModule.moduleClass

    val paramsAsTerms = keyNames map { kn => newTermName(kn) }
    val typedParams = (paramsAsTerms zip paramTypes) map {
      case (trm, tp) => q"val $trm: $tp"
    }

    val applier = q"(..$typedParams) => $companion(..$paramsAsTerms)"

    val unapplier = q"(x: ${tpe}) => $companion.unapply(x)"
    val args = List(applier, unapplier) :: ( keyNames map { k => q"$k" } ) :: Nil

    c.Expr[CodecBson[C]] { q"$targetMethod(...$args)"}
    /*c.Expr[CodecBson[C]] {
      Apply(
        Apply(
          Ident(targetMethod),
          List(
            Select(Ident(companion), newTermName("apply")),
            Select(Ident(companion), newTermName("unapply"))
          )
        ),
        keyNames map { s => Literal(Constant(s)) }
      )
    }*/
  }

  def deriveCaseCodecBson[C]: CodecBson[C] = macro deriveCaseCodecBsonImpl[C]

}
