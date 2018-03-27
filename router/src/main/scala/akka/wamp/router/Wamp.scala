package akka.wamp.router

import akka.actor._
import akka.io.IO
import akka.wamp.messages._


/**
  * Is the extension driver for the Akka IO layer
  *
  * For a full description of the design and philosophy behind this
  * implementation please refer to <a href="http://doc.akka.io/docs/akka/current/scala/io.html">the Akka online documentation</a>.
  *
  *
  * == Bind ==
  *
  * (For routers) In order to listen inbound connections send a Bind message to the IO manager.
  *
  * {{{
  *   import akka.io._
  *   import akka.wamp._
  *   import akka.wamp.router._
  *   import akka.wamp.messages._
  *
  *   val router = Router.props()
  *
  *   val manager = IO(Wamp)
  *   manager ! Bind(router)
  *
  *   def receive = {
  *     case Bound(listener, uri) =>
  *       // listener is bound at uri
  *   }
  * }}}
  */
object Wamp extends ExtensionId[WampExt] with ExtensionIdProvider {

  /**
    * Returns the canonical ExtensionId for this Extension
    */
  override def lookup(): ExtensionId[_ <: Extension] = Wamp

  /**
    * Is used by Akka to instantiate the Extension identified by this ExtensionId,
    * internal use only.
    */
  override def createExtension(system: ExtendedActorSystem): WampExt = new WampExt(system)

  /**
    * Returns an instance of the extension identified by this ExtensionId instance.
    *
    * Java API: For extensions written in Scala that are to be used used from Java also,
    * this method should be overridden to get correct return type.
    */
  override def get(system: ActorSystem): WampExt = super.get(system)
}


private[wamp] class WampExt(system: ExtendedActorSystem) extends IO.Extension {
  val manager = system.actorOf(Manager.props(), name = "akka-wamp-router")
}
