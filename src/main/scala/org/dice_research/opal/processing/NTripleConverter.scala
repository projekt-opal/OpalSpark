package org.dice_research.opal.processing

import org.apache.spark.sql.SparkSession
import net.sansa_stack.rdf.spark.io._
import org.apache.jena.riot.Lang

object NTripleConverter {
  
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
    .appName("NtripleConverter").master("local[*]").getOrCreate()
    
    val input = args(0)
    
    val lang = Lang.RDFXML
    
    val triples = spark.rdf(lang)(input)
    
//    println(triples.count)
    triples.saveAsNTriplesFile(args(1),SaveMode.Overwrite,false)
  }
  
}