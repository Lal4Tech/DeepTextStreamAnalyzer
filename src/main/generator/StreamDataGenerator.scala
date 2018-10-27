import java.util.{Date, Properties}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord, ProducerConfig}
import kafka.producer.KeyedMessage

object StreamDataGenerator {
    def main(args: Array[String]) {
        //topic name
        val topic = "avg"
        //kafka server
        val brokers = "localhost:9092"
        //producer properties
        val props = new Properties()
            props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers)
            props.put(ProducerConfig.CLIENT_ID_CONFIG, "ScalaProducerExample")
            props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,                                             "org.apache.kafka.common.serialization.StringSerializer")
            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,                                           "org.apache.kafka.common.serialization.StringSerializer")
        val producer = new KafkaProducer[String, String](props)
        //Read File
        val bufferedSource = scala.io.Source.fromFile("data_1.csv","ISO-8859-1")
            for (line <- bufferedSource.getLines) {
                val cols = line.split(",").map(_.trim)
                val jsonString = """
                  {
                    "id": """+cols(1)+""",
                    "date":""" +cols(2)+""",
                    "text":""" +cols(5)+"""
                  }
                  """
                println(jsonString)
                val data = new ProducerRecord[String, String](topic, cols(1), jsonString)
                producer.send(data)
            }
        bufferedSource.close
        producer.close()
    }
}
