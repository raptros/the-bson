---
layout: index
---
# the-bson


this library is an attempt to recreate some of the utilities of the (excellent) [Argonaut](http://argonaut.io/) library for manipulation Mongo DB objects;
the structure and interfaces of this library were made to be familiar to a user of Argonaut.
(also various things, especially how to use SBT to generate a bunch of typeclass instance building methods, were figured out by looking at the prior library.)
in particular, this library provides typeclasses for encoding and decoding DBObjects (and the fields of DBObjects).

[API documentation for the latest version][api-docs] is available.

## Installation
(pending)

## How to use
use this by implementing instances of [EncodeBson][] and [DecodeBson][], primarily. 

### Building DBObjects


### Encoding
```scala
import io.github.raptros.bson._
import Bson._
```


### Decoding


[api-docs]: http://raptros.github.io/the-bson/latest/api/#io.github.raptros.bson.package 
[EncodeBson]: http://raptros.github.io/the-bson/latest/api/#io.github.raptros.bson.EncodeBson
[DecodeBson]: http://raptros.github.io/the-bson/latest/api/#io.github.raptros.bson.DecodeBson
