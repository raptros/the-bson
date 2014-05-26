package io.github.raptros

package object bson {
  import scalaz._

  type DecodeResult[+A] = ValidationNel[DecodeError, A]

}
