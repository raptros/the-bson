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
