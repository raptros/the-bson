#### Encoding

you can encode entire objects by defining instances of [EncodeBson][]

```scala
//a junk trait
trait Junk {
  val x: Int
  val y: String
}
//define an encoder using the apply method in the EncodeBson object.
implicit def junkEncodeBson: EncodeBson[Junk] = EncodeBson { j =>
  DBO("x" :> j.x, "y" :> j.y)
}
val junk1 = new Junk {
  val x = 133
  val y = "some stuff"
}
val dbo: DBObject = junkEncodeBson(junk1)
//alternatively
val dbo: DBObject = junkEncodeBson.encode(junk1)
```

----
alternatively, using an implicit class in [Builders][] that has so far gone unmentioned - [ValueToBson][], you could replace the last line there with

```scala
val dbo: DBObject = junk1.asBson 
```

(asBson looks for an implicit encoder instance for the wrapped type, and uses its encode method)

-----
if you have an [EncodeBson][] trait for some type, it can also be used whenever an [EncodeBsonField][] is needed.

```scala
val dbo: DBObject = DBO("isJunk" :> true, "junk1" :> junk1)
```

----
you could replace the definition of junkEncodeBson using one of the bencode methods defined via [EncodeBsons][].

```scala
implicit def junkEncodeBson: EncodeBson[Junk] =
  bencode2f((j: Junk) => (j.x, j.y))("x", "y")
```

[Builders]: latest/api/#io.github.raptros.bson.Builders
[ValueToBson]: latest/api/#io.github.raptros.bson.Builders$ValueToBson
[EncodeBson]: latest/api/#io.github.raptros.bson.EncodeBson
[EncodeBsons]: latest/api/#io.github.raptros.bson.EncodeBsons
[EncodeBsonField]: latest/api/#io.github.raptros.bson.EncodeBsonField
