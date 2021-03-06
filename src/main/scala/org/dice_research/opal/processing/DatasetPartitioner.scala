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
import org.apache.spark.SparkContext
import java.nio.file.Files

/**
 *
 * Spark Job to split a dataportal file into individual files for each dataset
 *
 * @author Geraldo de Souza Junior
 *
 */
object DatasetPartitioner {

  var sc: SparkContext = null

  def main(args: Array[String]): Unit = {

    val spark = SparkSession.builder().appName("DatasetPartitioner")
      .master("local[*]")
      .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      .config("SPARK_LOCAL_DIRS","/tmp").getOrCreate()

    sc = spark.sparkContext

    spark.sparkContext.setLogLevel("ERROR")
    
    val inputFile = args(0)
    val tempDest = Files.createTempDirectory("convertDir").toFile

    val input = tempDest.getAbsolutePath

    val dest = args(1)
    
    val ntc = new NTripleConverter()  
    ntc.run(spark,inputFile, tempDest.getAbsolutePath)

    val lang = Lang.NT
    val files = getListOfFiles(input)

    for (f <- files) {

      val graphRdd = spark.rdf(lang)(f.getAbsolutePath).persist

      val datasets = graphRdd.filter(f => f.getObject.toString.equals("http://www.w3.org/ns/dcat#Dataset")).collect

      for (d <- datasets) {
        var dsname = d.getSubject.toString.substring(d.getSubject.toString.lastIndexOf("/"), d.getSubject.toString.size)
        println(" >> Processing Dataset: " + dsname)
        
        if(dsname.length > 100)
            dsname = dsname.substring(0,100)

        if (!new File(dest + "/" + dsname + ".nt").exists()) {

          var datasetDataRDD = graphRdd.filterSubjects(n => n.toString.equals(d.getSubject.toString))
          val objects = datasetDataRDD.map(f => f.getObject).filter(n => n.isURI() || n.isBlank()).collect().distinct
          val visited = objects.map(f => f.toString()).distinct
          datasetDataRDD = datasetDataRDD.union(getGraph(objects, graphRdd, visited))

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
      graphRdd.unpersist(true)
    }
    tempDest.delete()
    spark.stop

  }

  def getGraph(objects: Array[Node], graphRdd: RDD[Triple], visited: Array[String]): RDD[Triple] = {
    var result: RDD[Triple] = graphRdd.filterSubjects(n => objects.contains(n));

    val newObjects = result.map(f => f.getObject).collect().distinct.filter(p => !visited.contains(p.toString()))

    val n_visited = visited ++ newObjects.map(n => n.toString()).distinct

    if (newObjects.size > 0)
      sc.union(result, getGraph(newObjects.filter(n => n.isURI() || n.isBlank()), graphRdd, n_visited))
    else
      result
  }

  def getListOfFiles(dir: String): List[File] = {
    val d = new File(dir)
    if (d.exists && d.isDirectory) {
      d.listFiles.filter(_.isFile).toList
    } else {
      List[File]()
    }
  }

}