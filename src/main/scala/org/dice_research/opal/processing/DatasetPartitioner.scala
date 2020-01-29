package org.dice_research.opal.processing

import net.sansa_stack.rdf.spark.io._
import net.sansa_stack.rdf.spark.model._
import org.apache.jena.riot.Lang
import org.apache.jena.graph._
import org.apache.spark.rdd.RDD
import org.apache.spark.graphx.Graph
import org.apache.spark.sql.SparkSession
import org.apache.jena.graph.NodeFactory


/**
 * 
 * Spark Job to split a dataportal file into individual files for each dataset
 * 
 * @author Geraldo de Souza Junior
 * 
 */
object DatasetPartitioner {

  def main(args: Array[String]): Unit = {

    val spark = SparkSession.builder().appName("DatasetPartitioner").getOrCreate()

    val input = args(0)
    val dest = args(1)

    val lang = Lang.NTRIPLES

    val graphRdd = spark.rdf(lang)(input)
    val sparqlQuery = "select ?s where {?s a <http://www.w3.org/ns/dcat#Dataset>.} "

    import net.sansa_stack.query.spark.query._

    val datasets = graphRdd.sparql(sparqlQuery).rdd.map(x => x(1).toString).collect

    for (d <- datasets) {
      var datasetDataRDD = graphRdd.find(Some(NodeFactory.createURI(d)), None, None)
      val objects = datasetDataRDD.map(f => f.getObject).collect()
      datasetDataRDD = datasetDataRDD.union(getGraph(objects ,graphRdd))
      val dsname = d.substring(d.lastIndexOf("/"),d.size)
      datasetDataRDD.coalesce(1,true).saveAsNTriplesFile(dest + "/"+ dsname,
          SaveMode.Overwrite, false)

    }
    
    spark.stop

  }

  def getGraph(objects: Array[Node], graphRdd: RDD[Triple]): RDD[Triple] = {
    var result: RDD[Triple] = null;
    for (o <- objects) {
      if (result == null)
        result = graphRdd.find(Some(o), None, None)
      else
        result = result.union(graphRdd.find(Some(o), None, None))
    }
    val res = result.collect
    val newObjects = result.map(f => f.getObject).collect()
    val tm = result.size()
    if (newObjects.size > 0)
      result.union(getGraph(newObjects, graphRdd))
    else
      result
  }

}