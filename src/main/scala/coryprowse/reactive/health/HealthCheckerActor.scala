package coryprowse.reactive.health

import java.time.Clock

import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.pipe
import akka.util.Timeout
import coryprowse.reactive.health.HealthActor.QueryStatus
import coryprowse.reactive.health.HealthCheckerActor.{HealthCheckTick, HealthUpdate, pollInterimDuration}

import scala.collection.immutable
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

object HealthCheckerActor {

  // TODO: Move to config
  val pollInterimDuration = 5 seconds

  case object HealthCheckTick

  case class HealthUpdate(updatedStatusMap: Map[String, QueryStatus])

  def props()(implicit clock: Clock) = Props(new HealthCheckerActor())
}

class HealthCheckerActor()(implicit clock: Clock) extends Actor with ActorLogging {

  implicit val executionContext = context.system.dispatcher
  implicit val timeout = Timeout(15 seconds)

  val healthMBeanActor = context.actorOf(HealthMBeanActor.props)

  def scheduleNextPoll() = context.system.scheduler.scheduleOnce(pollInterimDuration, self, HealthCheckTick)

  scheduleNextPoll()

  var statusMap = immutable.Map.empty[String, QueryStatus]

  def receive = {
    case HealthUpdate(updatedStatusMap) =>
      scheduleNextPoll()
      statusMap = updatedStatusMap

    case HealthCheckTick =>
      pipe(fetchHealthUpdate().map(HealthUpdate))
        .to(self)
        .to(context.parent)
        .to(healthMBeanActor)
      ()
  }

  private var isAvailable = true

  private def fetchHealthUpdate(): Future[Map[String, QueryStatus]] =
  // TODO: Http requests on localhost
    Future.successful {
      val dummyQueryName = "a"
      isAvailable = !isAvailable
      log.info(s"""Testing -- query:"$dummyQueryName"  isAvailable=$isAvailable""")
      Map(dummyQueryName -> QueryStatus(isAvailable, clock.instant(), None))
    }

  private def sendUpdates() = {
    val update = HealthUpdate(statusMap)
    context.parent ! update
    healthMBeanActor ! update
  }
}
