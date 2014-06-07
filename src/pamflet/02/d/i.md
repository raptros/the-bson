#### Decoding Fields

instances of the trait [DecodeBsonField][] are used to extract values from DBObject. various instances are already provided in [DecodeBsonFields][]

```scala
import io.github.raptros.bson._
import scalaz.\/-
import Bson._ 

val dbo = DBO("int-key" :> 35)

val v = implicitly[DecodeBsonField[Int]].decode("int-key", dbo)
//alternatively
val v = implicitly[DecodeBsonField[Int]]("int-key", dbo)

v === \/-(35)
```

##### Extractor syntax
The implicit class [DBOWrapper][] is provided in [Extractors][]; among others methods, it provides `field`.

```scala
val dbo = DBO("doubleKey" :> 33.2)

dbo.field[Double]("doubleKey")
v === \/-(33.2)
```

it also provides `fieldOpt`:

```scala
val dbo = DBO("k1": true)

val v0 = dbo.fieldOpt[Boolean]("k1")
v0 === \/-(Some(true))

val v1 = dbo.fieldOpt[Boolean]("k2")
v1 === \/-(None)

val v2 = dbo.fieldOpt[Double]("k1)
v2 === -\/(NonEmptyList(WrongFieldType("k1",
  classOf[java.lang.Double],
  classOf[java.lang.Boolean])))
```


##### Deriving new instances
hopefully, you will not need to do this too often (for reasons explained in the next section),
but there are several ways to derive new instances.

* you can transform successfully decoded values
    
    ```scala
    implicit val urlDecodeField: DecodeBsonField[URI] = 
      stringDecodeField map { s =>
        new URI(s) //pretend this works
      }
    ```
    
* you can use `flatMap`
* you can use `orElse` (aka `|||`), as used in the library itself

    ```scala
    implicit val datetimeDecodeField = 
      castableDecoder[DateTime] ||| 
      dateDecodeField map { d => new DateTime(d) }
    ```

* you can define new ones using the [apply method in the DecodeBsonField object][apply]

    ```scala
    implicit def decodeOptionalField[A](implicit d: DecodeBsonField[A]): DecodeBsonField[Option[A]] = 
      DecodeBsonField { (k, dbo) =>
        if (dbo.containsField(k)) d(k, dbo) map { Some(_) } else none[A].right
      }
    ```


[DecodeBsonField]: latest/api/#io.github.raptros.bson.DecodeBsonField
[apply]: latest/api/#io.github.raptros.bson.DecodeBsonField$apply
[DecodeBsonFields]: latest/api/#io.github.raptros.bson.DecodeBsonFields
[Extractors]: latest/api/#io.github.raptros.bson.Extractors
[DBOWrapper]: latest/api/#io.github.raptros.bson.Extractors$DBOWrapper
