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


[DBOKV]: latest/api/#io.github.raptros.bson.Builders$DBO
[StringToDBOKV]: latest/api/#io.github.raptros.bson.Builders$StringToDBOKV
[EncodeBsonField]: latest/api/#io.github.raptros.bson.EncodeBsonField
[EncodeBsonFields]: latest/api/#io.github.raptros.bson.EncodeBsonFields
