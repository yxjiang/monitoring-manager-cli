package sysmon.cli.commands

import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive

import javax.jms.MessageProducer
import javax.jms.TextMessage

/**
 * Conduct the monitor retrieve task.
 */
class DoListMonitor(override val successor: Command) extends Command(successor) {

  def handleRequest(input: String, producer: MessageProducer, textMessage: TextMessage) = input match {
    case command if command.startsWith("list monitor") => {
      val retrieveMonitorJsonObject = new JsonObject
      command.stripPrefix("list monitor").trim match {
        case collectorConstraint if collectorConstraint.length == 0 => retrieveMonitorJsonObject.addProperty("type", "retrieve-monitors")
        case collectorConstraint if collectorConstraint.length != 0 => {
          retrieveMonitorJsonObject.addProperty("type", "retrieve-monitors-by-collector")
          retrieveMonitorJsonObject.addProperty("collector", collectorConstraint)
        }
      }
      textMessage setText retrieveMonitorJsonObject.toString
      producer send textMessage
    }
    case _ => if (successor != null) successor.handleRequest(input, producer, textMessage) else unidentified
  }

  def handleResponse(response: JsonObject) = {
    response.get("type").getAsString() match {
      case "retrieve-collectors-response" => {
        val collectorsArray = response.get("collectors").getAsJsonArray
        println("There are totally [" + collectorsArray.size() + "] collectors.")
        val collectorsItr = collectorsArray.iterator
        while (collectorsItr.hasNext) {
          val collectorIP = collectorsItr.next.asInstanceOf[JsonPrimitive].getAsString
          println("\t" + collectorIP)
        }
      }
      case "retrieve-monitors-response" => {
        val monitorsArray = response.get("monitors").getAsJsonArray
        println("There are totally [" + monitorsArray.size() + "] monitors.")
        val monitorsItr = monitorsArray.iterator
        while (monitorsItr.hasNext) {
          val monitorIP = monitorsItr.next.asInstanceOf[JsonPrimitive].getAsString
          println("\t" + monitorIP)
        }
      }
      case _ => if (successor != null) successor.handleResponse(response) else unidentified
    }
  }

}