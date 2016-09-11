package akka.wamp.serialization


import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.wamp._
import akka.wamp.messages._
import akka.wamp.{messages => wamp}

import org.scalatest._
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent._
import scala.concurrent.duration._

class JsonSerializationSpec extends WordSpec 
  with MustMatchers 
  with TryValues with OptionValues with EitherValues with ScalaFutures
  with ParallelTestExecution
  with BeforeAndAfterAll
{

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher
  
  override protected def afterAll(): Unit = {
    materializer.shutdown()
    system.terminate()
    Await.ready(system.whenTerminated, 10.seconds)
  }
  
  implicit val validator = new Validator(strictUris = false)
  
  val s = new JsonSerialization
  
  "The wamp.2.json serialization" when {
    "deserializing" should {
      
      "fail for invalid messages" in {
        List(
          """  { noscan """,
          """["invalid",noscan] """,
          """[null,""",
          """[999,noscan] """
        ).foreach { text =>
          a[DeserializeException] mustBe thrownBy(s.deserialize(source(text)))
        }
      }
      
      //[HELLO, Realm|uri, Details|dict]
      "fail for invalid HELLO" in {
        List(
          """[1]""",
          """[1,null]""",
          """[1,"myapp.realm"]""",
          """[1,"myapp.realm",null]""",
          """[1,"myapp.realm",{}]"""",
          """[1,"myapp.realm",{"roles":null}]"""",
          """[1,"myapp.realm",{"roles":{}}]"""",
          """[1,"myapp.realm",{"roles":{"invalid":{}}}]"""",
          """[1,"invalid..realm",{"roles":{"publisher":{}}}]""""
        ).foreach { text =>
          a[DeserializeException] mustBe thrownBy(s.deserialize(source(text)))
        }
      }
      "succeed for valid HELLO" in {
        s.deserialize(source("""[  1  ,"myapp.realm",  {"roles":{"caller":{},"callee":{}}}]""")) match {
          case message: wamp.Hello =>
            message.realm mustBe "myapp.realm"
            message.details mustBe Map("roles" -> Map("caller" -> Map(), "callee" -> Map()))
          case _ => fail
        }
      }

      //[WELCOME, Session|id, Details|dict]
      "fail for invalid WELCOME" in {
        List(
          """[2]""",
          """[2,null]""",
          """[2,1]""",
          """[2,1,null]""",
          """[2,0,{"roles":{"broker":{}}}]""",
          """[2,9007199254740993,{"roles":{"broker":{}}}]"""
        ).foreach { text =>
          a[DeserializeException] mustBe thrownBy(s.deserialize(source(text)))
        }
      }
      "succeed for valid WELCOME" in {
        s.deserialize(source("""[2,9007199254740992,{"roles":{"broker":{}}}]""")) match {
          case message: wamp.Welcome =>
            message.sessionId mustBe 9007199254740992L
            message.details mustBe Map("roles" -> Map("broker" -> Map()))
          case _ => fail
        }
      }

      
      //[ABORT, Details|dict, Reason|uri]
      "fail for invalid ABORT" in {
        List(
          """[3]""",
          """[3,null]""",
          """[3,{}]""",
          """[3,{},null]"""
        ).foreach { text =>
          a[DeserializeException] mustBe thrownBy(s.deserialize(source(text)))
        }
      }
      "succeed for valid ABORT" in {
        s.deserialize(source("""[3, {"message": "The realm does not exist."},"wamp.error.no_such_realm"]""")) match {
          case message: wamp.Abort =>
            message.details mustBe Map("message" ->"The realm does not exist.")
            message.reason mustBe "wamp.error.no_such_realm"
          case _ => fail
        }
      }

      
      //[GOODBYE, Details|dict, Reason|uri]
      "fail for invalid GOODBYE" in {
        List(
          """[6]""",
          """[6,null]""",
          """[6,{}]""",
          """[6,{},null]""",
          """[6,{},"invalid..reason"]"""
        ).foreach { text =>
          a[DeserializeException] mustBe thrownBy(s.deserialize(source(text)))
        }
      }
      "succeed for valid GOODBYE" in {
        s.deserialize(source("""[6,{"message": "The host is shutting down now."},"akka.wamp.system_shutdown"]""")) match {
          case message: wamp.Goodbye =>
            message.details mustBe Map("message" -> "The host is shutting down now.")
            message.reason mustBe "akka.wamp.system_shutdown"
          case _ => fail
        }
      }

      
      //[ERROR, REQUEST.Type|int, REQUEST.Request|id, Details|dict, Error|uri]
      //[ERROR, REQUEST.Type|int, REQUEST.Request|id, Details|dict, Error|uri, Arguments|list]
      //[ERROR, REQUEST.Type|int, REQUEST.Request|id, Details|dict, Error|uri, Arguments|list, ArgumentsKw|dict]
      "fail for invalid ERROR" in {
        List(
          """[8]""",
          """[8,null]""",
          """[8,34,null]""",
          """[8,34,1,null]""",
          """[8,34,1,{},null]""",
          """[8,34,1,{},"invalid..error"]""",
          """[8,34,0,{},"wamp.error.no_such_subscription"]""",
          """[8,34,9007199254740993,{},"wamp.error.no_such_subscription"]"""
        ).foreach { text =>
          a[DeserializeException] mustBe thrownBy(s.deserialize(source(text)))
        }
      }
      "succeed for valid ERROR bearing NO payload at all" in {
        s.deserialize(source("""[8,34,9007199254740992,{},"wamp.error.no_such_subscription"]""")) match {
          case message: wamp.Error =>
            message.requestType mustBe 34
            message.requestId mustBe 9007199254740992L
            message.details mustBe empty
            message.error mustBe "wamp.error.no_such_subscription"
            message.payload mustBe None
          case _ => fail
        }
      }
      "succeed for valid ERROR bearing Arguments|list" in {
        s.deserialize(source("""[8,34,1,{},"wamp.error.no_such_subscription",["paolo",40],{"arg0":"pietro","age":40,"male":true}]""")) match {
          case message: wamp.Error =>
            message.requestType mustBe 34
            message.requestId mustBe 1
            message.details mustBe empty
            message.error mustBe "wamp.error.no_such_subscription"
            message.payload match {
              case Some(payload: TextPayload) =>
                whenReduced(payload.source) { text =>
                  text mustBe """["paolo",40],{"arg0":"pietro","age":40,"male":true}]"""
                }
                whenReady(payload.arguments) { args =>
                  args mustBe List("paolo", 40)
                }
                whenReady(payload.argumentsKw) { args =>
                  args mustBe Map("arg0" -> "pietro", "arg1" -> 40, "age" -> 40, "male" -> true)
                }
              case _ => fail()
            }
          case _ => fail
        }
      }


      //[PUBLISH, Request|id, Options|dict, Topic|uri]
      //[PUBLISH, Request|id, Options|dict, Topic|uri, Arguments|list]
      //[PUBLISH, Request|id, Options|dict, Topic|uri, Arguments|list, ArgumentsKw|dict]
      "fail for invalid PUBLISH" in {
        List(
          """[16]""",
          """[16,null]""",
          """[16,1,null]""",
          """[16,1,{},null]""",
          """[16,1,{},"invalid..topic"]"""
        ).foreach { text =>
          a[DeserializeException] mustBe thrownBy(s.deserialize(source(text)))
        }
      }
      "succeed for valid PUBLISH bearing NO payload at all" in  {
        s.deserialize(source("""[16,9007199254740992,{"acknowledge":true},"myapp.topic1"]"""")) match {
          case message: wamp.Publish =>
            message.requestId mustBe 9007199254740992L
            message.options mustBe Map("acknowledge"->true)
            message.topic mustBe "myapp.topic1"
            message.payload mustBe None
          case _ =>
            fail("type mismatch")
        }
      }
      "succeed for valid PUBLISH bearing a payload" in {
        s.deserialize(source(s"""[16,1,{},"myapp.topic1",["paolo",40],{"arg0":"pietro","age":40,"male":true}]""")) match {
          case message: wamp.Publish =>
            message.requestId mustBe 1
            message.options mustBe empty
            message.topic mustBe "myapp.topic1"
            message.payload match {
              case Some(p: TextPayload) =>
                whenReduced(p.source) { text =>
                  text mustBe """["paolo",40],{"arg0":"pietro","age":40,"male":true}]"""
                }
                whenReady(p.arguments) { args =>
                  args mustBe List("paolo", 40)
                }
                whenReady(p.argumentsKw) { args =>
                  args mustBe Map("arg0"->"pietro", "arg1"->40, "age"->40, "male"->true)
                }
              case _ => fail
            }
          case _ => fail
        }
      }
      "succeed for valid PUBLISH bearing streamed payload arguments" in {
        pending
      }

      
      
      //[PUBLISHED, PUBLISH.Request|id, Publication|id]
      "fail for invalid PUBLISHED" in {
        List(
          """[17]""",
          """[17,null]""",
          """[17,1]""",
          """[17,1,null]""",
          """[17,0,2]""",
          """[17,9007199254740993,2]""",
          """[17,1,0]""",
          """[17,1,9007199254740993]"""
        ).foreach { text =>
          a[DeserializeException] mustBe thrownBy(s.deserialize(source(text)))
        }
      }
      "succeed for valid PUBLISHED" in {
        s.deserialize(source("""[17,9007199254740988,9007199254740992]""")) match {
          case message: wamp.Published =>
            message.requestId mustBe 9007199254740988L
            message.publicationId mustBe 9007199254740992L
          case _ => fail
        }
      }


      //[SUBSCRIBE, Request|id, Options|dict, Topic|uri]
      "fail for invalid SUBSCRIBE" in {
        List(
          """[32]""",
          """[32,null]""",
          """[32,1,null]""",
          """[32,1,{},null]""",
          """[32,1,{},"invalid..uri"]""",
          """[32,0,{},"myapp.topic1"]""",
          """[32,9007199254740993,{},"myapp.topic1"]"""
        ).foreach { text =>
          a[DeserializeException] mustBe thrownBy(s.deserialize(source(text)))
        }
      }
      "succeed for valid SUBSCRIBE" in {
        s.deserialize(source("""[32,9007199254740992,{},"myapp.topic1"]""")) match {
          case message: wamp.Subscribe =>
            message.requestId mustBe 9007199254740992L
            message.options mustBe empty
            message.topic mustBe "myapp.topic1"
          case _ => fail
        }
      }


      //[SUBSCRIBED, SUBSCRIBE.Request|id, Subscription|id]
      "fail for invalid SUBSCRIBED" in {
        List(
          """[33]""",
          """[33,null]""",
          """[33,1]""",
          """[33,1,null]""",
          """[33,0,2]""",
          """[33,9007199254740993,2]""",
          """[33,1,0]""",
          """[33,1,9007199254740993]"""
        ).foreach { text =>
          a[DeserializeException] mustBe thrownBy(s.deserialize(source(text)))
        }
      }
      "succeed for valid SUBSCRIBED" in {
        s.deserialize(source("""[33,9007199254740977,9007199254740992]""")) match {
          case message: wamp.Subscribed =>
            message.requestId mustBe 9007199254740977L
            message.subscriptionId mustBe 9007199254740992L
          case _ => fail
        }
      }

      
      //[UNSUBSCRIBE, Request|id, SUBSCRIBED.Subscription|id]
      "fail for invalid UNSUBSCRIBE" in {
        List(
          """[34]""",
          """[34,null]""",
          """[34,1]""",
          """[34,1,null]""",
          """[34,0,2]""",
          """[34,9007199254740993,2]""",
          """[34,1,0]""",
          """[34,1,9007199254740993]"""
        ).foreach { text =>
          a[DeserializeException] mustBe thrownBy(s.deserialize(source(text)))
        }
      }
      "succeed for valid UNSUBSCRIBE" in {
        s.deserialize(source("""[34,9007199254740955,9007199254740992]""")) match {
          case message: wamp.Unsubscribe =>
            message.requestId mustBe 9007199254740955L
            message.subscriptionId mustBe 9007199254740992L
          case _ => fail
        }
      }


      //[UNSUBSCRIBED, UNSUBSCRIBE.Request|id]
      "fail for invalid UNSUBSCRIBED" in {
        List(
          """[35]""",
          """[35,null]""",
          """[35,0]""",
          """[35,9007199254740993]"""
        ).foreach { text =>
          a[DeserializeException] mustBe thrownBy(s.deserialize(source(text)))
        }
      }
      "succeed for valid UNSUBSCRIBED" in {
        s.deserialize(source("""[35,9007199254740992]""")) match {
          case message: wamp.Unsubscribed => 
            message.requestId mustBe 9007199254740992L
          case _ =>  fail
        }
      }

      
      //[EVENT, SUBSCRIBED.Subscription|id, PUBLISHED.Publication|id, Details|dict]
      //[EVENT, SUBSCRIBED.Subscription|id, PUBLISHED.Publication|id, Details|dict, Arguments|list]
      //[EVENT, SUBSCRIBED.Subscription|id, PUBLISHED.Publication|id, Details|dict, Arguments|list, ArgumentsKw|dict]
      "fail for invalid EVENTs" in {
        List(
          """[36]""",
          """[36,null]""",
          """[36,1,null]""",
          """[36,1,2,null]""",
          """[36,0,2,{}]""",
          """[36,9007199254740993,2,{}]""",
          """[36,1,0,{}]""",
          """[36,1,9007199254740993,{}]"""
        ).foreach { text =>
          a[DeserializeException] mustBe thrownBy(s.deserialize(source(text)))
        }
      }
      
      "succeed for valid EVENT bearing NO payload at all" in {
        s.deserialize(source("""[36,9007199254740933,9007199254740992,{}]""")) match {
          case message: wamp.Event =>
            message.subscriptionId mustBe 9007199254740933L
            message.publicationId mustBe 9007199254740992L
            message.details mustBe empty
            message.payload mustBe None
          case _ => fail
        }
      }

      "succeed for valid EVENT bearing Arguments|list" in {
        s.deserialize(source("""[36,1,2,{},["paolo",40,{"human":true}],{"arg0":"pietro","age":40,"male":true}]""")) match {
          case message: wamp.Event =>
            message.subscriptionId mustBe 1
            message.publicationId mustBe 2
            message.details mustBe empty
            message.payload match {
              case Some(p: TextPayload) =>
                whenReduced(p.source) { text =>
                  text mustBe """["paolo",40,{"human":true}],{"arg0":"pietro","age":40,"male":true}]"""
                }
                whenReady(p.arguments) { args =>
                  args mustBe List("paolo", 40, Map("human"->true))
                }
                whenReady(p.argumentsKw) { args =>
                  args mustBe Map("arg0"->"pietro", "arg1"->40, "arg2"->Map("human"->true), "age"->40, "male"->true)
                }
              case _ => fail
            }
          case _ => fail
        }
      }
    }
    
    
    
    "serializing" should {

      "serialize HELLO" in {
        val message = wamp.Hello("akka.wamp.realm", Dict().withRoles("publisher"))
        whenReduced(s.serialize(message)) { json =>
          json mustBe """[1,"akka.wamp.realm",{"roles":{"publisher":{}}}]"""  
        }
      }
      
      "serialize WELCOME" in {
        val message = wamp.Welcome(1233242, Dict().withAgent("akka-wamp-0.7.0").withRoles("broker"))
        whenReduced(s.serialize(message)) { json =>
          json mustBe """[2,1233242,{"agent":"akka-wamp-0.7.0","roles":{"broker":{}}}]"""
        }
      }

      "serialize GOODBYE" in {
        val message = wamp.Goodbye(Dict(), "wamp.error.goobye_and_out")
        whenReduced(s.serialize(message)) { json =>
          json mustBe """[6,{},"wamp.error.goobye_and_out"]"""
        }
      }

      "serialize ABORT" in {
        pending
      }

      "serialize ERROR" in {
        val message1 = wamp.Error(Subscribe.tpe, 341284, Error.defaultDetails, "wamp.error.no_such_subscription")
        whenReduced(s.serialize(message1)) { json =>
          json mustBe """[8,32,341284,{},"wamp.error.no_such_subscription"]"""
        }

        val message2 = wamp.Error(Subscribe.tpe, 341284, Error.defaultDetails, "wamp.error.no_such_subscription", Some(Payload(List("paolo", 40, true))))
        whenReduced(s.serialize(message2)) { json =>
          json mustBe s"""[8,32,341284,{},"wamp.error.no_such_subscription",["paolo",40,true]]"""
        }

        val message3 = wamp.Error(Subscribe.tpe, 341284, Error.defaultDetails, "wamp.error.no_such_subscription", Some(Payload(Dict("arg0"->"paolo","age"->40,"arg2"->true))))
        whenReduced(s.serialize(message3)) { json =>
          json mustBe """[8,32,341284,{},"wamp.error.no_such_subscription",[],{"arg0":"paolo","age":40,"arg2":true}]"""
        }
      }


      "serialize PUBLISH" in {
        val message1 = wamp.Publish(341284, options = Dict(), "myapp.topic1")
        whenReduced(s.serialize(message1)) { json =>
          json mustBe """[16,341284,{},"myapp.topic1"]"""
        }

        val message2 = wamp.Publish(341284, options = Dict(), "myapp.topic1", Some(Payload(List("paolo", 40, true))))
        whenReduced(s.serialize(message2)) { json =>
          json mustBe """[16,341284,{},"myapp.topic1",["paolo",40,true]]"""
        }

        val message3 = wamp.Publish(341284, options = Dict(), "myapp.topic1", Some(Payload(List(), Dict("name"->"paolo","age"->40))))
        whenReduced(s.serialize(message3)) { json =>
          json mustBe """[16,341284,{},"myapp.topic1",[],{"name":"paolo","age":40}]"""
        }
        
        val message4 = wamp.Publish(341284, options = Dict(), "myapp.topic1", Some(Payload(List("paolo",true), Dict("age"->40))))
        whenReduced(s.serialize(message4)) { json =>
          json mustBe """[16,341284,{},"myapp.topic1",["paolo",true],{"age":40}]"""
        }
      }

      "serialize PUBLISHED" in {
        val message =  wamp.Published(713845233, 5512315)
        whenReduced(s.serialize(message)) { json =>
          json mustBe """[17,713845233,5512315]"""
        }
      }
      
      "serialize SUBSCRIBE" in {
        val message =  wamp.Subscribe(1, Dict(), "myapp.topic1")
        whenReduced(s.serialize(message)) { json =>
          json mustBe """[32,1,{},"myapp.topic1"]"""
        }
      }
      
      "serialize SUBSCRIBED" in {
        val message =  wamp.Subscribed(713845233, 5512315)
        whenReduced(s.serialize(message)) { json =>
          json mustBe """[33,713845233,5512315]"""
        }
      }

      "serialize UNSUBSCRIBED" in {
        val message =  wamp.Unsubscribed(85346237)
        whenReduced(s.serialize(message)) { json =>
          json mustBe """[35,85346237]"""
        }
      }

      "serialize EVENT" in {
        val message1 = wamp.Event(713845233, 5512315)
        whenReduced(s.serialize(message1)) { json =>
          json mustBe """[36,713845233,5512315,{}]"""
        }
        
        val message2 = wamp.Event(713845233, 5512315, Dict(), Some(Payload(List("paolo", 40, true))))
        whenReduced(s.serialize(message2)) { json =>
          json mustBe s"""[36,713845233,5512315,{},["paolo",40,true]]"""
        }

        val message3 = wamp.Event(713845233, 5512315, Dict(), Some(Payload(List(), Dict("arg0"->"paolo","age"->40,"arg2"->true))))
        whenReduced(s.serialize(message3)) { json =>
          json mustBe """[36,713845233,5512315,{},[],{"arg0":"paolo","age":40,"arg2":true}]"""
        }
      }
    }
  }
  
  
  def whenReduced(source: String)(testCode: (String) => Unit): Unit = {
    // whenReady(source.runReduce(_ + _)) { text =>
      testCode(source)
    // }
  }
  
  def source[A](x: A): A = identity(x)
}
