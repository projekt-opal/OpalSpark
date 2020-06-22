# OpalSpark
## Spark Job for dataset processing

The goal of this project is to extract dcat:dataset graphs from a large RDF file and store each one on it's own file.

To build the project, certify that you have scala installed and run:

```
	$ mvn clean install
```

under /target, you will have the builded Jar and the lib folder, containing all the necessary libs to run this job.

```
spark-submit --class org.dice_research.opal.processing.DatasetPartitioner --jars $(echo lib/*.jar | tr ' ' ',') --driver-memory 115G --master local[*] OpalSpark-1.0.jar /home/spark-adm/Documents/data/raw/govdata/govdata_16-06-2020.ttl /home/spark-adm/Documents/processed/govdata/govdata_16-06-2020/

```


