### Macros

[BsonMacros][] defines macros for deriving [EncodeBson][]s, [DecodeBson][]s and [CodecBson][]s for case classes. 

```scala
case class Simple(name: String, something: Boolean, amount: Int)
implicit val simpleCodec: CodecBson[Simple] =
  BsonMacros.deriveCaseCodecBson[Simple]
val t1 = Simple("test 2", true, 35)
val dbo = t1.asBson
dbo.decode[Simple] === \/-(t1)
```


[CodecBson]: latest/api/#io.github.raptros.bson.CodecBson
[EncodeBson]: latest/api/#io.github.raptros.bson.EncodeBson
[DecodeBson]: latest/api/#io.github.raptros.bson.DecodeBson
[BsonMacros]: latest/api/#io.github.raptros.bson.BsonMacros$
