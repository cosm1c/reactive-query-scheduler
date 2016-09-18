package coryprowse.reactive.health

import java.time.Instant
import java.util
import javax.management.ObjectName

import akka.actor.{Actor, ActorLogging, Props}
import coryprowse.reactive.health.HealthActor.{FunctionStatus, RequestHealthStatus}

import scala.collection.immutable

object HealthActor {

  case object RequestHealthStatus

  case class FunctionStatus(isOpen: Boolean, lastChecked: Instant, lastError: Option[String] = None)

  def props = Props[HealthActor]
}

class HealthActor extends Actor with ActorLogging with HealthActorMBean {

  import coryprowse.reactive.jmx.AkkaJmxRegistrar._

  import scala.collection.JavaConverters._

  // Because JMX and the actor model access from different threads
  @volatile private[this] var functionStatus = immutable.Map.empty[String, FunctionStatus]

  override val getActorPath: String = self.path.toStringWithoutAddress

  val objName = new ObjectName("coryprowse.reactive.health", {
    new java.util.Hashtable(
      Map(
        "name" -> "HealthActor"
      ).asJava
    )
  })

  override def getCurrentFunctionStatus: util.Map[String, String] = new util.HashMap(functionStatus.mapValues(_.toString).asJava)

  override def preStart(): Unit = {
    registerToMBeanServer(this, objName)
    ()
  }

  override def postStop(): Unit = unregisterFromMBeanServer(objName)

  def receive = {
    case RequestHealthStatus => sender() ! functionStatus
  }

}

trait HealthActorMBean {

  val getActorPath: String

  def getCurrentFunctionStatus: util.Map[String, String]
}
