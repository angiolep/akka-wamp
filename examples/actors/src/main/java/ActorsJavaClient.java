/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.md', which is part of this source code package.
 */

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.wamp.SessionScopedIdGenerator;
import akka.wamp.Wamp;
import akka.wamp.client.AbstractClientActor;
import akka.wamp.messages.*;
import akka.wamp.serialization.Payload;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.net.URI;
import java.util.concurrent.TimeUnit;


public class ActorsJavaClient {
  public static void main(String[] args) {
    final ActorSystem system = ActorSystem.create("myapp");
    system.actorOf(Props.create(Client.class, () -> new Client()));
  }
}


class Client extends AbstractClientActor {
  private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
  
  private final URI ROUTER_URI;
  private final Integer MAX_ATTEMPTS = 8;
  private Integer attempts = 0;
  private ActorRef conn;
  private Long sessionId;
  private SessionScopedIdGenerator gen;
  private Long requestId;
  private AbstractActor.Receive connecting, connected, open;

  Client() throws Exception {
    ROUTER_URI = new URI("ws://localhost:8080/wamp");
    final ActorRef manager = Wamp.get(getContext().system()).manager();
    final ExecutionContextExecutor executor = context().dispatcher();

    connecting = receiveBuilder()
      .match(AttemptConnection.class, cmd -> {
        if (attempts < MAX_ATTEMPTS) {
          attempts = attempts + 1;
          log.info("Connection attempt #{} to {}", attempts, ROUTER_URI);
          manager.tell(new Connect(ROUTER_URI, "json"), self());
        }
        else {
          log.warning("Max connection attempts reached!");
          self().tell(PoisonPill.getInstance(), self());
        }
      })
      .match(CommandFailed.class, sig -> {
        log.warning(sig.ex().getMessage());
        context().system().scheduler().scheduleOnce(
          new FiniteDuration(1, TimeUnit.MINUTES), self(), new AttemptConnection(), executor, self());
      })
      .match(Connected.class, sig -> {
        conn = sig.handler();
        log.info("Connected {}", conn);
        attempts = 0;
        getContext().become(connected);
        conn.tell(new Hello("default", Hello.defaultDetails(), validator()), self());
      })
      .build();


    connected = receiveBuilder()
      .match(Disconnect.class, sig -> {
        log.warning("Disconnected");
        this.sessionId = 0L;
        this.conn = null;
        self().tell(new AttemptConnection(), self());
      })
      .match(Abort.class, msg -> {
        log.warning(msg.toString());
        this.sessionId = 0L;
        self().tell(PoisonPill.getInstance(), self());
      })
      .match(Welcome.class, msg -> {
        this.sessionId = msg.sessionId();
        this.gen = new SessionScopedIdGenerator();
        log.info("Session #{} open", sessionId);
        // TODO getContext().become(open);
        Runnable runnable = () -> {
          ProtocolMessage publish = new Publish(gen.nextId(), Publish.defaultOptions(), "myapp.topic", Payload.apply(), validator());
          log.info("Publishing {}", publish);
          conn.tell(publish, self());
        };
        scheduler().schedule(Duration.Zero(), Duration.create(1, TimeUnit.SECONDS), runnable, executor);
      })
      .build();
  }


  @Override
  public Receive createReceive() {
    return connecting;
  }

  @Override
  public void preStart() throws Exception {
    self().tell(new AttemptConnection(), self());
  }

  class AttemptConnection {}
}


