package coryprowse.reactive.health

import java.time.Clock
import java.util
import javax.management.ObjectName

import akka.actor.{Actor, ActorLogging, Props}
import coryprowse.reactive.health.HealthActor.QueryStatus
import coryprowse.reactive.health.HealthCheckerActor.TotalHealthUpdate
import coryprowse.reactive.jmx.AkkaJmxRegistrar._

import scala.collection.JavaConverters._
import scala.collection.immutable

object HealthMBeanActor {

  case class HealthUpdate(statusMap: Map[String, QueryStatus])

  def props = Props[HealthMBeanActor]
}

class HealthMBeanActor extends Actor with ActorLogging with HealthMBeanActorMBean {

  implicit val clock = Clock.systemUTC()

  // Because JMX and the actor model access from different threads
  @volatile private[this] var queryStatusMap = immutable.Map.empty[String, QueryStatus]

  val objName = new ObjectName("coryprowse.reactive.health", {
    new java.util.Hashtable(
      Map(
        "name" -> "HealthMBeanActor"
      ).asJava
    )
  })

  override def preStart(): Unit = {
    registerToMBeanServer(this, objName)
    ()
  }

  override def postStop(): Unit = unregisterFromMBeanServer(objName)

  override val getActorPath: String = self.path.toStringWithoutAddress

  override def getCurrentQueryStatus: util.Map[String, String] = new util.HashMap(queryStatusMap.mapValues(_.toString).asJava)

  def receive = {
    case TotalHealthUpdate(statusMap) => queryStatusMap = statusMap
  }

}

trait HealthMBeanActorMBean {

  def getActorPath: String

  def getCurrentQueryStatus: util.Map[String, String]
}
