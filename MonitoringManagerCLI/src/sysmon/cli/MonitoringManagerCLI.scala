package sysmon.cli

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
import sysmon.cli.commands.DoListMonitor
import sysmon.cli.commands.DoListCollector

class MonitoringManagerCLI(val managerIP: String) extends MessageListener {
  val brokerAddress = "tcp://" + managerIP + ":32097"
  val parser = new JsonParser()

  val connectionFactory = new ActiveMQConnectionFactory(brokerAddress)
  val connection = connectionFactory.createConnection
  connection.start
  val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
  val sendTopic = session.createTopic("command")
  val replyQueue = session.createQueue("ReplyQueue")

  val producer = session.createProducer(sendTopic)
  producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT)

  val replyConsumer = session.createConsumer(replyQueue)
  replyConsumer.setMessageListener(this)
  
  val command = new DoListMonitor(new DoListCollector(null))
  
  def run = {
    print("cmd> ")
    while (true) {
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
    command.handleRequest(cmd, producer, textMessage)
  }

  def onMessage(message: Message) = {
    message match {
      case text: TextMessage => {
        val content = text.getText
        val responseJson = parser.parse(content).getAsJsonObject
        command handleResponse responseJson
        print("cmd> ")
      }
      case _ => {
        println("unidentified message")
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