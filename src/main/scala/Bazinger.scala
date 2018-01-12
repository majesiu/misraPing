import Bazing.{DropPing, DropPong, Start}
import akka.actor.{Actor, ActorSelection, ActorSystem, Props}
import akka.event.Logging
import com.lightbend.akka.sample.Node.{Bazinga, Ping, Pong}
import com.typesafe.config.ConfigFactory

object Bazing {
  case class Start(selection: ActorSelection)
  case class DropPing(selection: ActorSelection)
  case class DropPong(selection: ActorSelection)
}

class Bazing extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case Start(selection) =>
      selection ! Ping(1)
      selection ! Pong(-1)
    case DropPing(selection) =>
      selection ! Bazinga("Lose Ping")
    case DropPong(selection) =>
      selection ! Bazinga("Lose Pong")

    case _ => log.info("received unknown message")
  }
}
//#main-class
object Bazinger extends App {
  val config = ConfigFactory.load()
  val nodeAddress = args(0)
  val command = args(1)
  println(nodeAddress)
  println(command)
  val system = ActorSystem("mutualExclusionRing",config.getConfig("bazinger").withFallback(config))
  val selection = system.actorSelection(nodeAddress)
  val actorRef = system.actorOf(Props[Bazing], "Bazing")
  if(command == "Start"){
    actorRef ! Start(selection)
  } else if (command == "DropPing"){
    actorRef ! DropPing(selection)
  } else if (command == "DropPong"){
    actorRef ! DropPong(selection)
  }
}