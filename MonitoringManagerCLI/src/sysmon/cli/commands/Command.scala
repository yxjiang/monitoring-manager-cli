package sysmon.cli.commands

import javax.jms.MessageProducer
import javax.jms.TextMessage
import com.google.gson.JsonObject

abstract class Command(val successor: Command) {
  def handleRequest(input: String, producer: MessageProducer, textMessage: TextMessage)
  def handleResponse(response: JsonObject)
  def unidentified = println("unidentified command")
}