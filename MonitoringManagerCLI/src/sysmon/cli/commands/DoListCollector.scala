package sysmon.cli.commands

import javax.jms.MessageProducer
import com.google.gson.JsonObject
import javax.jms.TextMessage
import com.google.gson.JsonPrimitive

/**
 * Conduct the monitor retrieve task.
 */
class DoListCollector(override val successor: Command) extends Command(successor) {

  def handleRequest(input: String, producer: MessageProducer, textMessage: TextMessage) = input match {
    case input if input.startsWith("list ") => {
      input.stripPrefix("list ") match {
        case "collector" | "collectors" => {
          val retrieveCollectorJsonObject = new JsonObject
          retrieveCollectorJsonObject.addProperty("type", "retrieve-collectors")
          textMessage setText retrieveCollectorJsonObject.toString
          producer send textMessage
        }
        case _ => if (successor != null) successor.handleRequest(input, producer, textMessage) else unidentified
      }
    }
    case _ => if (successor != null) successor.handleRequest(input, producer, textMessage) else unidentified
  }

  def handleResponse(response: JsonObject) = {
    response.get("type").getAsString match {
      case "retrieve-monitors-by-collector-response" => {
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