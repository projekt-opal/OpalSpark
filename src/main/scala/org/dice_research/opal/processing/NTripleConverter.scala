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

    var triples: RDD[Triple] = null

    val files = args.dropRight(1)

    for (path <- files) {

      val lang = Lang.TTL
      if (triples == null) {
        triples = spark.rdf(lang)(path)

      } else {
        val temp = spark.rdf(lang)(path)
        triples.union(temp)
      }

    }
    triples.saveAsNTriplesFile(args(args.size - 1), SaveMode.Overwrite, false)

  }

}