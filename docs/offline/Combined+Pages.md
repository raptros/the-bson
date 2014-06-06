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

DecodeResult
#### Decoding Fields


explain
#### Decoding objects


explain
### Case Classes, Codecs, etc.

a [CodecBson][] instance is both an encoder and a decoder - it implements both [EncodeBson][] and [DecodeBson][]. 

[CodecBson]: latest/api/#io.github.raptros.bson.CodecBson
[EncodeBson]: latest/api/#io.github.raptros.bson.EncodeBson
[DecodeBson]: latest/api/#io.github.raptros.bson.DecodeBson

### Macros

we have them. they are good for deriving encoders, decoders, and codecs for case classes. see [BsonMacros][]

[BsonMacros]: latest/api/#io.github.raptros.bson.BsonMacros$
