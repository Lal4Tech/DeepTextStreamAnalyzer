# Deep Text Stream Analysis

### Introduction
Extracting valid, novel, useful/actionable, understandable information from large amount of data is always having significance in various domains. The data can come from various sources, can have various forms, structured or unstructured and can be either static or stream. Collecting, storing, pre-processing, analyzing and communicating the results bring lot of challenges. It’s observed that the methods used at each stages of processing vary based on the behaviour of data. 

In this project we analyze stream of text data. We provide emphasis for both pre processing and analysis stage. Also, possibly implement visualization showcasing different statistics and analysis results.

### System Architecture
The architecture of system is given below
![alt text](https://github.com/HarilalOP/DeepTextStreamAnalyzer/blob/master/src/main/misc/architecture_diagram.jpg)

The system contains following main components.
<ul>
  <li>Stream generator</li>
  <li>Message broker</li>
  <li>Master application</li> 
  <li>Sentiment analyzer</li>
  <li>Data storage</li>
</ul>

The stream generator reads text data from dataset[1] and create a JSON string in the form of following sample format.
<code>
  {
	  id = "1467815924",
	  date = "Mon Apr 06 22:19:49 PDT 2009",
	  text = "@alielayus I want to go to promote GEAR AND GROOVE but unfortunately no ride there..."
  }
</code>

Then this JSON will send to the message broker - Kafka. The master application receives this message and convert the underlying RDDs to Dataframes. As part of preprocessing we remove URLs from text as it’s seldom contribute to analysis. In this step a new column named formatted_text is added to the dataframe which is then passed to sentiment analysis stage. For sentiment estimation, we are using Stanford NLP[2] which use Recursive Neural Tensor Networks and the Sentiment Treebank. Using user defined function[UDF], we integrate the sentiment analysis to this spark streaming application. The sentiment is get added to the dataframe and get pushed to Cassandra database.

For analytics purpose, Python notebook is used. In which basic analysis is shown. It reads data stored from Cassandra database and transform it into pandas data frame to carryout operation. 

### Tools
<ul>
  <li>Big Data: Apache Spark, Cassandra, Kafka</li>
  <li>Development tools: Scala, Python</li>
  <li>Natural Language Processing Algorithms : Stanford NLP</li>
</ul>

### Data Set 

Source: <a href = "https://www.kaggle.com/kazanova/sentiment140">Sentiment140 dataset with 1.6 million tweets</a> [3]

### Running the application

#### Kafka

<code> //export environment variables </code>

<code>export KAFKA_HOME="/usr/local/kafka"</code>

<code>export PATH=$KAFKA_HOME/bin:$PATH</code>

<code>//start zookeeper</code>
  
<code>$KAFKA_HOME/bin/zookeeper-server-start.sh $KAFKA_HOME/config/zookeeper.properties </code>

<code>//start kafka server</code>

<code>$KAFKA_HOME/bin/kafka-server-start.sh $KAFKA_HOME/config/server.properties</code>

<code>//create Kafka topic</code>
  
<code>$KAFKA_HOME/bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic text_anlyz</code>

#### Cassandra

<code>//export environment variables</code>
  
<code>export CASSANDRA_HOME="/usr/local/cassandra"</code>

<code>export PYTHONPATH="/home/harilal/anaconda2/bin/python"</code>

<code>export PATH=$PYTHONPATH/bin:$CASSANDRA_HOME/bin:$PATH</code>

<code>//Start Cassandra in the foreground</code>
  
<code>$CASSANDRA_HOME/bin/cassandra -f</code>

<code>//Start the cqlsh prompt</code>

<code>$CASSANDRA_HOME/bin/cqlsh</code>

<code>//Create keyspace</code>
  
<code>create keyspace textanlyz_space with replication = {‘class’: ‘SimpleStrategy’, ‘replication_factor’: 1};</code>

<code>//Create table</code>
  
<code>use textanlyz_space;</code>

<code>CREATE TABLE IF NOT EXISTS textanlyz_space.txt_anlyz_stats (id text PRIMARY KEY, date text, text_data text, formatted_text text, sentiment text);</code>

<code>desc txt_anlyz_stats;</code>

<code>//Check table  content</code>
  
<code>select * from txt_anlyz_stats;</code>

<code>select id, date, text_data, sentiment from txt_anlyz_stats limit 10;</code>

#### Stream

<code>//Generate streaming input</code>

<code>cd Project/DeepTextStreamAnalyzer/src/main/generator</code>

<code>sbt run</code>

#### Application

<code>//Run the application</code>
  
<code>cd Project/DeepTextStreamAnalyzer/src/main/analyzer</code>
  
<code>sbt run</code>

#### Analytics Notebook

Install Cassandra Python driver before running notebook commands. 


### References

[1].“Sentiment140’ - A Twitter Sentiment Analysis Tool, Sentiment140, help.sentiment140.com/for-students.

[2]. Socher, Richard, et al. "Recursive deep models for semantic compositionality over a sentiment treebank." Proceedings of the 2013 conference on empirical methods in natural language processing. 2013.

[3]. Go, A., Bhayani, R. and Huang, L., 2009. “Twitter sentiment classification using distant supervision. CS224N Project Report”, Stanford, 1(2009), p.12.
