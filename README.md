# OpalSpark
## Spark Job for dataset processing

The goal of this project is to extract dcat:dataset graphs from a large RDF file and store each one on it's own file.

To build the project, certify that you have scala installed and run:

```
	$ mvn clean install
```

under /target, you will have the builded Jar and the lib folder, containing all the necessary libs to run this job.

Before running, certify that you have ***Apache Spark 2.4.X*** installed on your system.
Check this tutorial for a Standalone installation:
https://www.tutorialspoint.com/apache_spark/apache_spark_installation.htm

Or you can use this docker image:
https://github.com/big-data-europe/docker-spark

To run the Job, use the command through command line:
```
spark-submit --class org.dice_research.opal.processing.DatasetPartitioner --jars $(echo lib/*.jar | tr ' ' ',') --driver-memory 100G --master local[*] OpalSpark-1.0.jar <path_to_*.ttl_file> <destination_folder>

```
The driver-memory parameter represents the ammount of physicial memory that will be allocated to run the job. Larger the file, larger the ammount that should be allocated.

In the given command line example, the masteris set to local, considering running the job locally. For other configurations like running on a cluster, check Spark documentation:

https://spark.apache.org/docs/latest/submitting-applications.html




