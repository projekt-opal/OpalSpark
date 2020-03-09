package org.dice_research.opal.processing

import org.apache.spark.sql.SparkSession
import net.sansa_stack.rdf.spark.io._
import org.apache.jena.riot.Lang
import org.apache.spark.rdd.RDD
import org.apache.jena.graph.Triple

object NTripleConverter {

  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("NtripleConverter")
      //    .master("local[*]")
      .getOrCreate()

    val lang = Lang.TTL

    val triples = spark.rdf(lang)(args(0))

    triples.saveAsNTriplesFile(args(1), SaveMode.Overwrite, false)

  }

}