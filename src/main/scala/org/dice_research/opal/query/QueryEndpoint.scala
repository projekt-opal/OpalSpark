package org.dice_research.opal.query

import java.awt.Desktop
import java.net.URI

import net.sansa_stack.query.spark.sparqlify.{QueryExecutionFactorySparqlifySpark, SparqlifyUtils3}
import net.sansa_stack.rdf.spark.io._
import net.sansa_stack.rdf.spark.partition.core.RdfPartitionUtilsSpark
import org.aksw.jena_sparql_api.server.utils.FactoryBeanSparqlServer
import org.apache.jena.riot.Lang
import org.apache.spark.sql.SparkSession

/**
  * Run SPARQL queries over Spark using Sparqlify approach.
  */
object QueryEndpoint {

  def main(args: Array[String]) {
    parser.parse(args, Config()) match {
      case Some(config) =>
        run(config.in, config.sparql, config.run, config.port)
      case None =>
        println(parser.usage)
    }
  }

  def run(input: String, sparqlQuery: String = "", run: String = "cli", port: String = "7531"): Unit = {


    val spark = SparkSession.builder
      .appName(s"Opal BigData Sparql ( $input )")
      .master("local[*]")
      .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      .config("spark.kryo.registrator", String.join(
        ", ",
        "net.sansa_stack.rdf.spark.io.JenaKryoRegistrator",
        "net.sansa_stack.query.spark.sparqlify.KryoRegistratorSparqlify"))
      .getOrCreate()

    val lang = Lang.NT
    val graphRdd = spark.rdf(lang)(input)

    
        val partitions = RdfPartitionUtilsSpark.partitionGraph(graphRdd)
        val rewriter = SparqlifyUtils3.createSparqlSqlRewriter(spark, partitions)

        val port = 7531

        val qef = new QueryExecutionFactorySparqlifySpark(spark, rewriter)
        val server = FactoryBeanSparqlServer.newInstance.setSparqlServiceFactory(qef).setPort(port).create()
        
        if (Desktop.isDesktopSupported) {
          Desktop.getDesktop.browse(URI.create("http://localhost:" + port + "/sparql"))
          println("server started")
        }
        server.join()
        
    

    spark.stop

  }

  case class Config(in: String = "", sparql: String = "SELECT * WHERE {?s ?p ?o} LIMIT 10", run: String = "cli", port: String = "7531")

  val parser = new scopt.OptionParser[Config]("Sparklify example") {

    head(" Opal Spark")

    opt[String]('i', "input").required().valueName("<path>").
      action((x, c) => c.copy(in = x)).
      text("path to file that contains the data (in TTL format)")

    opt[String]('q', "sparql").optional().valueName("<query>").
      action((x, c) => c.copy(sparql = x)).
      text("a SPARQL query")

    opt[String]('r', "run").optional().valueName("Runner").
      action((x, c) => c.copy(run = x)).
      text("Runner method, default:'cli'")

    opt[String]('p', "port").optional().valueName("port").
      action((x, c) => c.copy(port = x)).
      text("port that SPARQL endpoint will be exposed, default:'7531'")

    checkConfig(c =>
      if (c.run == "cli" && c.sparql.isEmpty) failure("Option --sparql must not be empty if cli is enabled")
      else success)

    help("help").text("prints this usage text")
  }

}