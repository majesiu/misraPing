//#full-example
package com.lightbend.akka.sample

import akka.actor.{Actor, ActorSelection, ActorSystem, Props}
import akka.event.Logging
import com.lightbend.akka.sample.Node.{Bazinga}
import com.typesafe.config.ConfigFactory

object Node {
  case class Ping(nr: Int)
  case class Pong(nr: Int)
  case class Bazinga(msg: String)
  def props(nextActor: ActorSelection): Props = Props(new Node(nextActor))
}

class Node(nextActor: ActorSelection) extends Actor {
  val log = Logging(context.system, this)
  var m = 0
  var ping_in = false
  var pong_in = false
  var lose_pong = false
  var lose_ping = false
  import Node._


  def receive: PartialFunction[Any, Unit] = {

    case Ping(nr) => log.info(s"Received Ping $nr grating enter to CS")
      if (lose_ping) {
        log.info(s"Lose Ping $nr flag active!")
        lose_ping = false
      }
      else if (m.abs > nr) log.info(s"Received Ping $nr that is no longer valid")
      else {
        ping_in = true
        Thread.sleep(1500)
        if(m == nr){ //Lost Pong
          log.info(s"Detected lost pong")
            m = m.abs+1
            nextActor ! Pong(-m.abs)
        } else if (pong_in){
          log.info(s"Have both ping and pong")
          m = nr + 1
          nextActor ! Pong(-m.abs)
        } else {
          m = nr
        }
        ping_in = false
        nextActor ! Ping(m.abs)
        log.info(s"Sent Ping $nr and left CS")
      }

    case Pong(nr) => log.info(s"Received Pong $nr")
      if (lose_pong) {
        log.info(s"Lose Pong $nr flag active!")
        lose_pong = false
      }
      else if (m.abs > nr.abs) log.info(s"Received Pong $nr that is no longer valid")
      else {
        pong_in = true
        if(m == nr){ //Lost Ping
          log.info(s"Detected lost ping")
          m = m.abs+1
          nextActor ! Ping(m.abs)
        } else if (ping_in){
          log.info(s"Have both pong and ping")
          m = nr - 1
          nextActor ! Ping(m.abs)
        } else {
          m = nr
        }
        pong_in = false
        nextActor ! Pong(-m.abs)
        log.info(s"Sent Pong $nr")
      }

    case Bazinga("Startup") => log.info(s"received startup message")
    case Bazinga("Lose Ping") => log.info(s"received lose ping message")
      lose_ping = true
    case Bazinga("Lose Pong") => log.info(s"received lose ping message")
      lose_pong = true

    case _      ⇒ log.info("received unknown message")
  }
}

//#main-class
object AkkaQuickstart extends App {
  val config = ConfigFactory.load()
  val nodeName = args(0)
  val system = ActorSystem("mutualExclusionRing", config.getConfig(s"$nodeName").withFallback(config))
  val address = ("akka.tcp://mutualExclusionRing@"+config.getString(s"$nodeName.next.hostname")
    +":"+ config.getInt(s"$nodeName.next.port")+"/user/"+config.getString(s"$nodeName.next.name"))
  println(address)
  val selection = system.actorSelection(address)
  val actorRef = system.actorOf(Node.props(selection), nodeName)
  actorRef ! Bazinga("Startup")
}