package io.github.raptros.bson

import scala.language.existentials

sealed trait DecodeError {
  protected def describe: String
  override def toString = s"decode error: $describe"
}

case class NoSuchField(field: String) extends DecodeError {
  lazy val describe = s"no such field: $field"
}

case class WrongFieldType(field: String, wanted: Class[_], got: Class[_]) extends DecodeError {
  lazy val describe = s"wrong type in field $field: expected ${wanted.getCanonicalName}, got ${got.getCanonicalName}"
}

case class WrongType(wanted: Class[_], got: Class[_]) extends DecodeError {
  lazy val describe = s"wrong type: expected ${wanted.getCanonicalName}, got ${got.getCanonicalName}"
}

case class CustomError(msg: String) extends DecodeError {
  lazy val describe = s"custom error: $msg"
}

case class WrongFieldCount(expected: Int) extends DecodeError {
  lazy val describe = s"wrong number of fields, expected $expected"
}
