package raptros.bson

import com.mongodb.{BasicDBObject, DBObject}

object DBO {
  def empty: DBObject= new BasicDBObject

  def apply(tuples: DBOKV[_]*): DBObject = (tuples foldRight empty) { _ write _ }

}
