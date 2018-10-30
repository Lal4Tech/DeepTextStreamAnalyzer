package deeptextanalyzer

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord, ProducerConfig}
import kafka.serializer.{DefaultDecoder, StringDecoder}

import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka._

import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel

import org.apache.spark.sql.cassandra._
import org.apache.spark.sql.functions.udf
import org.apache.spark.sql.{Row, SparkSession, DataFrame, SQLContext}
import org.apache.spark.sql.types.{DoubleType, StringType, StructField, StructType}

//import scala.util.Random
import com.datastax.spark.connector._
import com.datastax.driver.core.{Session, Cluster, Host, Metadata}
import com.datastax.spark.connector.streaming._

object MasterApp {
	def main(args: Array[String]) {
		//connect to Cassandra and make a keyspace and table if they not exist already
		val cluster = Cluster.builder().addContactPoint("127.0.0.1").build()
		val session = cluster.connect()
		session.execute("CREATE KEYSPACE IF NOT EXISTS textanlyz_space WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };")
		session.execute("CREATE TABLE IF NOT EXISTS textanlyz_space.txt_anlyz_stats (id text PRIMARY KEY, date text, text_data text, sentiment text)")
		
		//spark
		val sparkConf = new SparkConf().setAppName("TextAnalyzer").setMaster("local[2]")
		val sc = new SparkContext(sparkConf)
		val ssc = new StreamingContext(sc, Seconds(1))
	
		//make a connection to Kafka and read (key, value) pairs from it
		val kafkaConf = Map(
			"metadata.broker.list" -> "localhost:9092", 
			"zookeeper.connect" -> "localhost:2181", 
			"group.id" -> "kafka-spark-streaming", 
			"zookeeper.connection.timeout.ms" -> "1000"
		)
		
		//retrieve message from kafka topic : text_anlyz
		val messages = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](ssc, kafkaConf, Set("text_anlyz"))

		val sqlContext = new SQLContext(sc)

		//user defined function : getSentiment in SentimentAnalyzer class
		val myUDF = udf(SentimentAnalyzer.getSentiment _)

		//define the schema of the dataframe
		val schema = (new StructType).add("id", StringType).add("date", StringType).add("text_data", StringType)

		//accessing the underlying RDDs of the DStream, convert to dataframe and process
		messages.foreachRDD(rddRaw => {
			val rdd = rddRaw.map(_._2)
			val df = sqlContext.read.schema(schema).json(rdd).filter("id is not null")
			
			//add new column to dataframe and retrieve the sentiment using user defined function
			val df_new = df.withColumn("sentiment", myUDF(df("text_data")))

			//write dataframe to cassandra
			df_new.write.format("org.apache.spark.sql.cassandra").options(Map("table" -> "txt_anlyz_stats", "keyspace" -> "textanlyz_space")).save()
		})

		ssc.start()
		ssc.awaitTermination()
	}
}
