package io.github.raptros

package object bson {
  import scalaz._

  type DecodeResult[+A] = NonEmptyList[DecodeError] \/ A

}
