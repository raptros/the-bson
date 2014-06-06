#### Field encoding

instances of the trait [EncodeBsonField][] are used to write key-value pairs to DBObjects.
for instance, one of the provided definition in [EncodeBsonFields][] is:

```scala
implicit val intEncodeField: EncodeBsonField[Int] = directWritable[Int]
```

you could use this like so:

```scala
val dbo = new BasicDBObject() //from the mongo-java-driver
implicitly[EncodeBsonField[Int]].writeTo(dbo, "someKey", 35)
```

athough, as the next section describes, there are easier ways.

[EncodeBsonField]: latest/api/#io.github.raptros.bson.EncodeBsonField
[EncodeBsonFields]: latest/api/#io.github.raptros.bson.EncodeBsonFields
