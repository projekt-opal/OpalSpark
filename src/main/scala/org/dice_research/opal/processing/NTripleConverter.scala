package org.dice_research.opal.processing

import org.apache.spark.sql.SparkSession
import net.sansa_stack.rdf.spark.io._
import org.apache.jena.riot.Lang
import org.apache.spark.rdd.RDD
import org.apache.jena.graph.Triple
import java.io.File

class NTripleConverter {

  def run(spark: SparkSession, file: String, dest: String): Unit = {
    //    val spark = SparkSession.builder()
    //      .appName("NtripleConverter")
    //          .master("local[*]")
    //      .getOrCreate()

    val lang = Lang.TTL

    val triples = spark.rdf(lang)(file)

    triples.saveAsNTriplesFile(dest, SaveMode.Overwrite, false)

    val success_file = new File(dest + "/_SUCCESS")
    success_file.delete()
    val folder = new File(dest)

    val fList = folder.listFiles()
    // Searchs .crc

    for (i <- fList) {
      val pes = i.getAbsolutePath
      if (pes.endsWith(".crc")) {
        // and deletes
        val success = (new File(pes).delete());
      }
    }

  }

}