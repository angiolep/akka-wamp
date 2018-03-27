package akka.wamp.serialization

import akka.NotUsed
import akka.event.{LogSource, Logging}
import akka.http.scaladsl.model.{ws => websocket}
import akka.http.scaladsl.model.ws.{BinaryMessage, TextMessage}
import akka.stream.ActorAttributes.supervisionStrategy
import akka.stream.{ActorMaterializer, Materializer, Supervision}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.wamp.Validator
import akka.wamp.{messages => wamp}
import com.fasterxml.jackson.core.JsonFactory

/**
  * The JSON serialization flows
  * 
  * @param validateStrictUri is the boolean switch (default is false) to validate against strict URIs rather than loose URIs
  * @param tolerateProtocolViolations is the boolean switch to drop an offending message and resume to the next one
  * @param materializer is the Akka Stream materializer
  */
class JsonSerializationFlows(validateStrictUri: Boolean, tolerateProtocolViolations: Boolean)
                            (implicit materializer: ActorMaterializer)
  extends SerializationFlows 
{
  val log = Logging(materializer.system, this)

  /** The Jackson factory */
  private val jsonFactory = new JsonFactory()
  
  /** The WAMP types validator */
  implicit val validator = new Validator(validateStrictUri)
  
  /** The actor system dispatcher */
  implicit val executionContext = materializer.executionContext


  /**
    * Serialize from wamp.Message object to textual websocket.Message
    */
  val serialize: Flow[wamp.ProtocolMessage, websocket.Message, NotUsed] =
    Flow[wamp.ProtocolMessage].
      mapAsync(1) {
        case message: wamp.ProtocolMessage =>
          val s = new JsonSerialization(jsonFactory)
          val textStream = s.serialize(message)
          // TODO Couldn't we return the textStream itself rathter than its reduction?
          textStream.runReduce(_ + _).map(txt => TextMessage(txt))
      }



  /**
    * Deserialize textual websocket.Message to wamp.Message object
    */
  val deserialize: Flow[websocket.Message, wamp.ProtocolMessage, NotUsed] =
    Flow[websocket.Message]
      .map {
        case TextMessage.Strict(text) =>
          val s = new JsonSerialization(jsonFactory)
          s.deserialize(Source.single(text))

        case TextMessage.Streamed(source) =>
          val s = new JsonSerialization(jsonFactory)
          s.deserialize(source)

        case bm: BinaryMessage =>
          // ignore binary messages but drain content to avoid the stream being clogged
          bm.dataStream.runWith(Sink.ignore)
          throw new DeserializeException("Cannot deserialize binary message as JSON message was expected instead!")
      }
      .withAttributes(supervisionStrategy {
        case ex: DeserializeException =>
          if (!tolerateProtocolViolations) {
            // default
            log.error(ex.getMessage, ex)
            Supervision.Stop
          }
          else {
            log.warning("Resume from DeserializeException: {}", ex.getMessage)
            Supervision.Resume
          }
      })
}


object JsonSerializationFlows {
  implicit val logSource: LogSource[AnyRef] = new LogSource[AnyRef] {
    def genString(o: AnyRef): String = o.getClass.getName
    override def getClazz(o: AnyRef): Class[_] = o.getClass
  }
}