#### Decoding objects

instances of [DecodeBson][] allow you to decode entire DBObjects.
this example demonstrates several decoding utilities:

```scala
def bdecode4f[A, B, C, D, X](
  fxn: (A, B, C, D) => X)(ak: String, bk: String, ck: String,
  dk: String)(implicit decodea: DecodeBsonField[A], 
  decodeb: DecodeBsonField[B], decodec: DecodeBsonField[C],
  decoded: DecodeBsonField[D]): DecodeBson[X] = DecodeBson { dbo =>
  ApV.apply4(
    decodea(ak, dbo).validation,
    decodeb(bk, dbo).validation,
    decodec(ck, dbo).validation,
    decoded(dk, dbo).validation
  )(fxn).disjunction
}
```

* It uses [the DecodeBson object's apply method][apply] to construct a decoder that takes apart a DBObject.
* It shows how one might use the `Applicative` instance, [ApV in DecodeBsons][ApV], to collect up multiple decoding errors.
* It is actually one of the methods available in [DecodeBsons][] for conveniently defining a decoder for e.g. a case class
    
    ```scala
    case class FourThings(str: String, anInt: Int, b: Boolean, d: Double)
    implicit def decodeFourThings: DecodeBson[FourThings] = 
      bdecode4f(FourThings.apply)("str", "anInt", "b", "d")
    ```
    
of course, there are other ways to define new implementations of [DecodeBson][], which you can find in the Scaladocs.

##### Extractor syntax
[DBOWrapper][] defines a method `decode`, which looks up a `DecodeBson[A]`.

```scala
val dbo = DBO("str" :> "some string", "anInt" :> 23, "b" :> false, "d" :> 55.3)
val v = dbo.decode[FourThings]
v === \/-(FourThings("some string", 23, false, 55.3))
```

##### decoders and fields
any `DecodeBson` instance can also be used as a `DecodeBsonField`

```scala
val dbo = DBO("4things" :> DBO("str" :> "some string",
  "anInt" :> 23, "b" :> false, "d" :> 55.3))
val v = dbo.field[FourThings]("4things")
v === \/-(FourThings("some string", 23, false, 55.3))
```

[DecodeBson]: latest/api/#io.github.raptros.bson.DecodeBson
[DecodeBsons]: latest/api/#io.github.raptros.bson.DecodeBsons
[ApV]: latest/api/index.html#io.github.raptros.bson.DecodeBsons@ApV:scalaz.Applicative[DecodeBsons.this.DecodeResultV] 
[DecodeBsonField]: latest/api/#io.github.raptros.bson.DecodeBsonField
[apply]: latest/api/#io.github.raptros.bson.DecodeBson$apply
[DecodeBsonFields]: latest/api/#io.github.raptros.bson.DecodeBsonFields
[Extractors]: latest/api/#io.github.raptros.bson.Extractors
[DBOWrapper]: latest/api/#io.github.raptros.bson.Extractors$DBOWrapper
