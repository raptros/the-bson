---
layout: index
---
the-bson
========
encoding and decoding Mongo DB Objects using typeclasses.

Inspiration
-----------
This library was inspired by and is modeled on the [Argonaut](http://argonaut.io/) library, which is a great way to create and decode Json in Scala.
The structure and interfaces of this library were made to be familiar to a user of Argonaut,
and 

this library is an attempt to recreate some of the utilities of the (excellent) for manipulation Mongo DB objects;
(also various things, especially how to use SBT to generate a bunch of typeclass instance building methods, were figured out by looking at the prior library.)
in particular, this library provides typeclasses for encoding and decoding DBObjects (and the fields of DBObjects).

Features
--------
* Utilities for building DBObjects
* Typeclasses for encoding and decoding
* Utilities for building encoders, decoders, and codecs (including macros for deriving them from case classes)

Documentation
-------------
* You can read [the manual here][manual].
* [API documentation for the latest version][api-docs] is available.

Installation
------------
(pending)

### Decoding Fields

### Decoding

### Codecs

### Using the macros

[manual]: http://raptros.github.io/the-bson/manual/the-bson.html
[api-docs]: http://raptros.github.io/the-bson/latest/api/#io.github.raptros.bson.package 
[Builders]: http://raptros.github.io/the-bson/latest/api/#io.github.raptros.bson.Builders
[DBO]: http://raptros.github.io/the-bson/latest/api/#io.github.raptros.bson.Builders$DBO$
[DBOKV]: http://raptros.github.io/the-bson/latest/api/#io.github.raptros.bson.Builders$DBO
[StringToDBOKV]: http://raptros.github.io/the-bson/latest/api/#io.github.raptros.bson.Builders$StringToDBOKV
[DBOBuilder]: http://raptros.github.io/the-bson/latest/api/#io.github.raptros.bson.Builders$DBOBuilder
[ValueToBson]: http://raptros.github.io/the-bson/latest/api/#io.github.raptros.bson.Builders$ValueToBson
[EncodeBson]: http://raptros.github.io/the-bson/latest/api/#io.github.raptros.bson.EncodeBson
[EncodeBsons]: http://raptros.github.io/the-bson/latest/api/#io.github.raptros.bson.EncodeBsons
[EncodeBsonField]: http://raptros.github.io/the-bson/latest/api/#io.github.raptros.bson.EncodeBsonField
[EncodeBsonFields]: http://raptros.github.io/the-bson/latest/api/#io.github.raptros.bson.EncodeBsonFields
[DecodeBson]: http://raptros.github.io/the-bson/latest/api/#io.github.raptros.bson.DecodeBson
[DecodeBsons]: http://raptros.github.io/the-bson/latest/api/#io.github.raptros.bson.DecodeBsons
[DecodeBsonField]: http://raptros.github.io/the-bson/latest/api/#io.github.raptros.bson.DecodeBsonField
[DecodeBsonFields]: http://raptros.github.io/the-bson/latest/api/#io.github.raptros.bson.DecodeBsonFields
