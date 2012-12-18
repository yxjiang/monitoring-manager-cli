import scala.util.Random

import org.apache.activemq.ActiveMQConnectionFactory

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive

import javax.jms.DeliveryMode
import javax.jms.Message
import javax.jms.MessageListener
import javax.jms.Session
import javax.jms.TextMessage

class MonitoringManagerCLI(val managerIP: String) extends MessageListener {
  val brokerAddress = "tcp://" + managerIP + ":32097"
  val parser = new JsonParser()
  val gson = new GsonBuilder().setPrettyPrinting().create()

  val connectionFactory = new ActiveMQConnectionFactory(brokerAddress)
  val connection = connectionFactory.createConnection
  connection.start
  println("connect to " + brokerAddress)
  val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
  val sendTopic = session.createTopic("command")
  val replyQueue = session.createQueue("ReplyQueue")

  val producer = session.createProducer(sendTopic)
  producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT)

  val replyConsumer = session.createConsumer(replyQueue)
  replyConsumer.setMessageListener(this)

  def run() = {
    while (true) {
      print("cmd> ")
      val line = readLine()
      parse(line)
    }
  }

  private def parse(cmd: String) = {
    if (cmd.equals("quit")) {
      connection.close
      System.exit(1)
    }

    val correlationId = new Random().nextString(32)
    val textMessage = session.createTextMessage
    textMessage.setJMSCorrelationID(correlationId)
    textMessage.setJMSReplyTo(replyQueue)
    cmd match {
      case "retrieve-collectors" => { //	retrieve all the collectors
        val retrieveCollectorJsonObject = new JsonObject
        retrieveCollectorJsonObject.addProperty("type", "retrieve-collectors")
        textMessage setText retrieveCollectorJsonObject.toString
        producer send textMessage
      }
      case "retrieve-monitors" => {
        val retrieveMonitorsJsonObject = new JsonObject
        retrieveMonitorsJsonObject.addProperty("type", "retrieve-monitors")
        textMessage setText retrieveMonitorsJsonObject.toString
        producer send textMessage
      }
      case s if s.startsWith("retrieve-monitors-by-collector") => {
        val collectorIP = s.stripPrefix("retrieve-monitors-by-collector").trim
        if (collectorIP.length == 0) {
          println("usage: retrieve-monitors-by-collector collectorIP")
        } else {
          val retrieveMonitorsByCollectors = new JsonObject
          retrieveMonitorsByCollectors.addProperty("type", "retrieve-monitors-by-collector")
          retrieveMonitorsByCollectors.addProperty("collector", collectorIP)
          textMessage setText retrieveMonitorsByCollectors.toString
          producer send textMessage
        }

      }
      case _ => {
        println("unidentified command")
      }
    }
  }

  def onMessage(message: Message) = {
    message match {
      case text: TextMessage => {
        val content = text.getText
        val jsonObj = parser.parse(content).getAsJsonObject
        printResponse(jsonObj)
        print("cmd> ")
      }
      case _ => {
        println("unidentified message")
      }
    }
  }

  private def printResponse(jsonObj: JsonObject) = {
    val messageType = jsonObj.get("type").getAsString
    messageType match {
      case "retrieve-collectors-response" => {
        val collectorsArray = jsonObj.get("collectors").getAsJsonArray
        println("There are totally [" + collectorsArray.size() + "] collectors.")
        val collectorsItr = collectorsArray.iterator
        while (collectorsItr.hasNext) {
          val collectorIP = collectorsItr.next.asInstanceOf[JsonPrimitive].getAsString
          println("\t" + collectorIP)
        }
      }
      case "retrieve-monitors-response" => {
        val monitorsArray = jsonObj.get("monitors").getAsJsonArray
        println("There are totally [" + monitorsArray.size() + "] monitors.")
        val monitorsItr = monitorsArray.iterator
        while (monitorsItr.hasNext) {
          val monitorIP = monitorsItr.next.asInstanceOf[JsonPrimitive].getAsString
          println("\t" + monitorIP)
        }
      }
      case "retrieve-monitors-by-collector-response" => {
        val monitorsArray = jsonObj.get("monitors").getAsJsonArray
        println("There are totally [" + monitorsArray.size() + "] monitors.")
        val monitorsItr = monitorsArray.iterator
        while (monitorsItr.hasNext) {
          val monitorIP = monitorsItr.next.asInstanceOf[JsonPrimitive].getAsString
          println("\t" + monitorIP)
        }
      }
      case _ => {
        println("unidentified response")
      }
    }
  }

}

object cli extends App {
  if (args.length < 1) {
    println("Please input manager's IP")
    System.exit(1)
  }

  val managerIP = args(0)

  val managerCLI = new MonitoringManagerCLI(managerIP);
  println(managerCLI.managerIP)
  managerCLI.run

}