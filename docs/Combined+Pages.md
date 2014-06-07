# the-bson

Documentation on how to use this library.

[API Docs can be found here][api-docs].

[api-docs]: latest/api/#io.github.raptros.bson.package 

[Builders]: latest/api/#io.github.raptros.bson.Builders
[DBO]: latest/api/#io.github.raptros.bson.Builders$DBO$
[DBOKV]: latest/api/#io.github.raptros.bson.Builders$DBO
[StringToDBOKV]: latest/api/#io.github.raptros.bson.Builders$StringToDBOKV
[DBOBuilder]: latest/api/#io.github.raptros.bson.Builders$DBOBuilder
[ValueToBson]: latest/api/#io.github.raptros.bson.Builders$ValueToBson
[EncodeBson]: latest/api/#io.github.raptros.bson.EncodeBson
[EncodeBsons]: latest/api/#io.github.raptros.bson.EncodeBsons
[EncodeBsonField]: latest/api/#io.github.raptros.bson.EncodeBsonField
[EncodeBsonFields]: latest/api/#io.github.raptros.bson.EncodeBsonFields
[DecodeBson]: latest/api/#io.github.raptros.bson.DecodeBson
[DecodeBsons]: latest/api/#io.github.raptros.bson.DecodeBsons
[DecodeBsonField]: latest/api/#io.github.raptros.bson.DecodeBsonField
[DecodeBsonFields]: latest/api/#io.github.raptros.bson.DecodeBsonFields

## Installation

pending
## How to use

The entire functionality of the-bson can be imported in one go:

```scala
import io.github.raptros.bson._
import Bson._
```

if you find this too cumbersome, functionality and implicits can also be imported in various objects, e.g.

* implicits for encoding basic types as object fields

    ```scala
    import EncodeBsonField._ 
    ```

* additional implicits and several methods for constructing EncodeBson instances using extraction functions and named fields

    ```scala
    import EncodeBson._ 
    ```

* implicits for decoding basic types from DBObjects

    ```scala
    import DecodeBsonField._ 
    ```
    
* additional implicit decoders (that work at the DBObject level), 
    along with several methods for building DecodeBson instances by extracting named fields and applying constructor functions (or making tuples)

    ```scala
    import DecodeBson._
    ```
    
* implicit classes that give syntax for encoding entire objects or building DBObjects from key-value pairs
    
    ```scala
    import Builders._
    ```
    
* to import implicit classes that wrap DBObjects to allow you to decode entire objects or decode particular fields

    ```scala
    import Extractors._
    ```



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

### Building DBObjects

this library provides several methods for building DBObject from key-value pairs in the [Builders][] trait.
these methods rely on having [EncodeBsonField][] instances available for the value types.

[Builders]: latest/api/#io.github.raptros.bson.Builders
[EncodeBsonField]: latest/api/#io.github.raptros.bson.EncodeBsonField


#### key-value pair construction

the tools to build DBObjects rely on [DBOKV][] to write the key-value pairs into the objects.
the implicit class [StringToDBOKV][] provides methods for obtaining `DBOKV`s

```scala
val dbo = new BasicDBObject() //from the mongo-java-driver
val kv = "keyname" :> 25 //produces DBOKV[Int]("keyname", 25)
kv.write(dbo) //writes 25 into db at the key "keyname"
```

note that `:>` only produces a `DBOKV` if an implicit [EncodeBsonField][] can be found for the type of the argument.

----

there are two different ways to write optional values.

* the first way relies on the instance for `EncodeBsonField[Option[A]]` in [EncodeBsonFields][]

    ```scala
    val kv1: DBOKV[Option[Int]] = "keyname1" :> Some(25)
    kv1 === DBOKV("keyname1", Some(25))
    
    val kv2: DBOKV[Option[Int]] = "keyname2" :> (None: Option[Int])
    kv2 === DBOKV("keyname2", None)
    
    val dbo = DBO.empty
    kv1.write(dbo)
    kv2.write(dbo)
    
    kv1.keySet === Set("keyname1")
    kv1.get("keyname1") === 25
    ```

* the second uses a method in [StringToDBOKV][] and uses the encoder for A directly:

    ```scala
    val kv1: Option[DBOKV[Int]] = "keyname1" :?> Some(25)
    kv1 === Some(DBOKV("keyname1", 25)
    
    val kv2: Option[DBOKV[Int]] = "keyname2" :?> None
    kv2 === None
    ```
    
    the utility of this method will be explained later


[DBOKV]: latest/api/#io.github.raptros.bson.Builders$DBO$
[StringToDBOKV]: latest/api/#io.github.raptros.bson.Builders$StringToDBOKV
[EncodeBsonField]: latest/api/#io.github.raptros.bson.EncodeBsonField
[EncodeBsonFields]: latest/api/#io.github.raptros.bson.EncodeBsonFields

#### DBO and DBOBuilder

there are two ways to use these [DBOKV][]s:

* you can use the [DBO][] object and the apply method it contains:

```scala
    val dbo = DBO(
      "key1" :> 33,
      "key2" :> "another value",
      "booleanKey" :> true)
```

* you can use the methods provided by [DBOBuilder][], an implicit class which wraps around DBObjects:

    ```scala
    //this just creates a new DBObject
    val dbo = DBO.empty
    
    //the method +@+ writes the DBOKV into dbo and then returns dbo;
    //this permits chaining
    dbo +@+ ("key1" :> 33) 
    dbo +@+ ("bool1" :> true) +@+ ("bool2" :> false)
    
    //you can also append a sequenece of keys using the ++@++ method
    dbo ++@++ Seq("k3" :> "a string", "k4" :> 3.14)
    
    //there is a method +?+ which takes an Optional[DBOKV],
    //writes it it is Some, and returns the dbo either way
    dbo +?+ ("optionWritten" :?> Some(55)) +?+ 
        ("optionNotWritten" :?> None) +@+ 
        ("alwaysWritten" :> true)
        
    //finally, there is a Unitary write method.
    dbo.write("datetime", DateTime.now()) //joda-time DateTime
    ```
    
    note that all of these methods mutate dbo! this means they should be used with caution.
    in fact, it might be best not to allow DBOBjects to leak out of the scope they are instantiated in.



[DBO]: latest/api/#io.github.raptros.bson.Builders$DBO$
[DBOKV]: latest/api/#io.github.raptros.bson.Builders$DBO
[DBOBuilder]: latest/api/#io.github.raptros.bson.Builders$DBOBuilder

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

### Decoding DBObjects

all decode operations in the-bson produce [DecodeResult][], defined as

```scala
// \/ and NonEmptyList come from scalaz
type DecodeResult[+A] = \/[NonEmptyList[DecodeError], A]
```

if any decode operation fails, all the errors involved will be reported.

----
[DecodeError][] has fixed set of subtypes:

* [NoSuchField][]: a field that was expected to be present in an object is not
* [WrongType][WrongType] and
* [WrongFieldType][]: both result from casting failures, which means the latter is more likely
* [WrongFieldCount][]: see [validateFields in DecodeBson][validateFields]
* [CustomError][]: if you need to report an error that doesn't fit into one of the above ones, you can use this.

[DecodeResult]: latest/api/#io.github.raptros.bson.DecodeResult
[DecodeError]: latest/api/#io.github.raptros.bson.DecodeError
[NoSuchField]: latest/api/#io.github.raptros.bson.NoSuchField
[WrongType]: latest/api/#io.github.raptros.bson.WrongType
[WrongFieldType]: latest/api/#io.github.raptros.bson.WrongFieldType
[WrongFieldCount]: latest/api/#io.github.raptros.bson.WrongFieldCount
[CustomError]: latest/api/#io.github.raptros.bson.CustomError

[validateFields]: latest/api/#io.github.raptros.bson.DecodeBson#validateFields



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

### Case Classes, Codecs, etc.

a [CodecBson][] instance is both an encoder and a decoder - it implements both [EncodeBson][] and [DecodeBson][].
the main use for them is to define encoding and decoding of a case class simultaneously.

```scala
case class ThreePart(x: String, y: List[Int], z: DateTime)
implicit val codecThreePart =
  bsonCaseCodec3(ThreePart.apply, ThreePart.unapply)("x", "y", "z")
val example = ThreePart("hi", List(4, 3), z = DateTime.now())
val dbo = example.asBson
dbo.keySet === Set("x", "y", "z")
dbo.decode[ThreePart] === \/-(example)
```  

[CodecBson]: latest/api/#io.github.raptros.bson.CodecBson
[EncodeBson]: latest/api/#io.github.raptros.bson.EncodeBson
[DecodeBson]: latest/api/#io.github.raptros.bson.DecodeBson

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
