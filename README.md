the-bson
========
if you need to write any sorts of scala objects to mongoDB objects, and read any mongo DBObjects back to scala objects, the-bson can help you out!

Features
--------
* Utilities for building DBObjects easily.
* Typeclasses for encoding objects to Mongo DBObjects and decoding things from DBObjects
* Typeclasses, with several provided instances, for extracting primitives from DBObject fields, and writing primitives to DBObject fields.
* Utilities for building encoders, decoders, and codecs (including macros for deriving them from case classes)

Inspiration
-----------
This library was inspired by and is modeled on the [Argonaut](http://argonaut.io/) library, which is a great way to create and decode Json in Scala.
The structure and interfaces of this library were made to be familiar to a user of Argonaut.

Documentation
-------------
* You can read [the manual here][manual].
* [API documentation for the latest version][api-docs] is available.

Installation
------------
(pending)

[manual]: http://raptros.github.io/the-bson/docs/the-bson.html
[api-docs]: http://raptros.github.io/the-bson/docs/latest/api/#io.github.raptros.bson.package 
