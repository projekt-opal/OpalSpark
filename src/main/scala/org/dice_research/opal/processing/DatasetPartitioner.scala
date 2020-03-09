package org.dice_research.opal.processing

import net.sansa_stack.rdf.spark.io._
import net.sansa_stack.rdf.spark.model._
import org.apache.jena.riot.Lang
import org.apache.jena.graph._
import org.apache.spark.rdd.RDD
import org.apache.spark.graphx.Graph
import org.apache.spark.sql.SparkSession
import org.apache.jena.graph.NodeFactory

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import java.io.PrintWriter
import java.net.URI
import java.io.FileOutputStream
import java.io.File
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.ResourceFactory
import org.apache.jena.riot.RDFDataMgr

/**
 *
 * Spark Job to split a dataportal file into individual files for each dataset
 *
 * @author Geraldo de Souza Junior
 *
 */
object DatasetPartitioner {

  def main(args: Array[String]): Unit = {

    val spark = SparkSession.builder().appName("DatasetPartitioner")
      .master("local[*]")
      .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer").getOrCreate()

    spark.sparkContext.setLogLevel("ERROR")

    val input = args(0)
    val dest = args(1)
    val lType = args(2).toLowerCase()

    val lang = if (lType.equals("ttl")) Lang.TTL else Lang.NT

    val graphRdd = spark.rdf(lang)(input).persist

    val datasets = graphRdd.filter(f => f.getObject.toString.equals("http://www.w3.org/ns/dcat#Dataset")).collect

    for (d <- datasets) {
      val dsname = d.getSubject.toString.substring(d.getSubject.toString.lastIndexOf("/"), d.getSubject.toString.size)
      println(" >> Processing Dataset: " + dsname)

      if (!new File(dest + "/" + dsname + ".nt").exists()) {

        var datasetDataRDD = graphRdd.filterSubjects(n => n.toString.equals(d.getSubject.toString))
        val objects = datasetDataRDD.map(f => f.getObject).filter(n => n.isURI() || n.isBlank()).collect()
        datasetDataRDD = datasetDataRDD.union(getGraph(objects, graphRdd))

        val model = ModelFactory.createDefaultModel()

        datasetDataRDD.collect.foreach { t =>
          if (t.getObject.isURI() || t.getObject.isBlank())
            model.add(
              ResourceFactory.createResource(t.getSubject.toString()),
              ResourceFactory.createProperty(t.getPredicate.toString()),
              ResourceFactory.createResource(t.getObject.toString()))

          else
            model.add(
              ResourceFactory.createResource(t.getSubject.toString()),
              ResourceFactory.createProperty(t.getPredicate.toString()),
              ResourceFactory.createPlainLiteral(t.getObject.toString()))
        }

        val fos = new FileOutputStream(new File(dest + "/" + dsname + ".nt"))

        RDFDataMgr.write(fos, model, Lang.NT)

        println(" >> " + dsname + " dataset saved.")

      } else {
        println("Dataset " + dest + "/" + dsname + ".nt" + " already exists")
      }

    }

    spark.stop

  }

  def getGraph(objects: Array[Node], graphRdd: RDD[Triple]): RDD[Triple] = {
    var result: RDD[Triple] = graphRdd.filterSubjects(n => objects.contains(n));

    val newObjects = result.map(f => f.getObject).collect()
    if (newObjects.size > 0)
      result.union(getGraph(newObjects.filter(n => n.isURI() || n.isBlank()), graphRdd))
    else
      result
  }

}