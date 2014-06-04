package io.github.raptros

/** root package of the-bson.
  */
package object bson {
  import scalaz._

  /** decode result is either a non-empty list of errors, or some value.
    */
  type DecodeResult[+A] = NonEmptyList[DecodeError] \/ A

}
