package io.github.raptros.bson

import com.mongodb.DBObject
import scalaz.std.option._
import scalaz.syntax.std.option._
import scalaz.syntax.std.boolean._
import scalaz.syntax.validation._
import scalaz.syntax.id._
import scalaz.syntax._

/** contains any utilies for using decoders */
trait Extractors {

  /** wraps up DBObjects so that various decode methods can be used on them */
  implicit class DBOWrapper(dbo: DBObject) {
    /** decodes the dbo as type `A` using the in-scope [[DecodeBson]] instance for `A` */
    def decode[A](implicit d: DecodeBson[A]): DecodeResult[A] = d(dbo)

    /** extracts the field named by `k` and decodes it using a [[DecodeBsonField]] for type `A` */
    def field[A](k: String)(implicit d: DecodeBsonField[A]): DecodeResult[A] = d(k, dbo)

    /** if `k` is present in the dbo, extracts and decodes it as Some(a: A); returns None if not present.
      * {{{
      * fieldOpt[A](field)
      * }}}
      * should be equivalent to
      * {{{
      * field[Option[A]](key)
      * }}}
      */
    def fieldOpt[A](k: String)(implicit d: DecodeBsonField[A]): DecodeResult[Option[A]] =
      if (dbo containsField k) d(k, dbo) map { _.some } else none[A].right
  }

}

object Extractors extends Extractors