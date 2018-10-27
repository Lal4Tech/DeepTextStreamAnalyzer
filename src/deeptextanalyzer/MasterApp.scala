package deeptextanalyzer

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.streaming.kafka._
import kafka.serializer.{DefaultDecoder, StringDecoder}
import org.apache.spark.SparkConf
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka._
import org.apache.spark.storage.StorageLevel
import org.apache.spark.sql.SQLContext
import java.util.{Date, Properties}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord, ProducerConfig}
import scala.util.Random
import org.apache.spark.sql.functions.to_timestamp

import org.apache.spark.sql.cassandra._
import com.datastax.spark.connector._
import com.datastax.driver.core.{Session, Cluster, Host, Metadata}
import com.datastax.spark.connector.streaming._

object MasterApp {
	def main(args: Array[String]) {
		// connect to Cassandra and make a keyspace and table as explained in the document
		val cluster = Cluster.builder().addContactPoint("127.0.0.1").build()
		val session = cluster.connect()
		session.execute("CREATE KEYSPACE IF NOT EXISTS textanlyz_space WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };")
		session.execute("CREATE TABLE IF NOT EXISTS textanlyz_space.txt_anlyz_stats (id text PRIMARY KEY, date timestamp, text_string text, sentiment text)")
		
		//spark
		val sparkConf = new SparkConf().setAppName("TextAnalyzer").setMaster("local[2]")
		val ssc = new StreamingContext(sparkConf, Seconds(1))
		ssc.checkpoint(".")
	
		// make a connection to Kafka and read (key, value) pairs from it
		val kafkaConf = Map(
			"metadata.broker.list" -> "localhost:9092", 
			"zookeeper.connect" -> "localhost:2181", 
			"group.id" -> "kafka-spark-streaming", 
			"zookeeper.connection.timeout.ms" -> "1000"
		)
		
		val messages = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](ssc, kafkaConf, Set("text_anlyz"))

		val sqlContext = new SQLContext(ssc)

		messages.foreachRDD (rdd => {
  			val df = sqlContext.read.json(rdd.map(x => x._2))
  			df.show()
		})
		
		
		//val ts = to_timestamp($"dts", "MM/dd/yyyy HH:mm:ss")
		//println(SentimentAnalyzer.getSentiment(input))

		ssc.start()
		ssc.awaitTermination()
	}
}
