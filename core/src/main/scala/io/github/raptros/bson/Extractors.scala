package io.github.raptros.bson

import com.mongodb.DBObject
import scalaz.std.option._
import scalaz.syntax.std.option._
import scalaz.syntax.std.boolean._
import scalaz.syntax.validation._
import scalaz.syntax.id._
import scalaz.syntax._

/**
  */
trait Extractors {

  implicit class DBOWrapper(dbo: DBObject) {
    def decode[A](implicit d: DecodeBson[A]): DecodeResult[A] = d(dbo)

    def field[A](k: String)(implicit d: DecodeBsonField[A]): DecodeResult[A] = d(k, dbo)

    def fieldOpt[A](k: String)(implicit d: DecodeBsonField[A]): DecodeResult[Option[A]] =
      if (dbo containsField k) d(k, dbo) map { _.some } else none[A].right
  }

}

object Extractors extends Extractors
