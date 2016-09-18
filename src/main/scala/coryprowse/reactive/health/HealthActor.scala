package coryprowse.reactive.health

import java.time.{Clock, Instant}

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import coryprowse.reactive.health.HealthActor.{QueryStatus, RequestHealthStatus}
import coryprowse.reactive.health.HealthCheckerActor.HealthUpdate
import coryprowse.reactive.queryscheduler.external.ExternalRepository.{ExecuteExternalQuery, ExternalQueryResult}

import scala.collection.immutable
import scala.concurrent.duration._
import scala.language.postfixOps

object HealthActor {

  case object RequestHealthStatus

  case class QueryStatus(isAvailable: Boolean, lastChecked: Instant, lastError: Option[String] = None) {
    def isUnavailable = !isAvailable
  }

  def props(querySchedulerActor: ActorRef)(implicit clock: Clock) =
    Props(new HealthActor(querySchedulerActor))
}

class HealthActor(querySchedulerActor: ActorRef)(implicit clock: Clock) extends Actor with ActorLogging {

  implicit val executionContext = context.system.dispatcher
  implicit val timeout = Timeout(15 seconds)

  val healthCheckerActor = context.actorOf(HealthCheckerActor.props)
  var queryStatusMap = immutable.Map.empty[String, QueryStatus]

  def receive = {
    case msg@ExecuteExternalQuery(query) =>
      val maybeStatus = queryStatusMap.get(query.queryName)
      if (maybeStatus.exists(_.isUnavailable)) {
        // TODO: Response indicating query is unavailable
        val now = clock.instant
        sender() ! ExternalQueryResult(now, now, query, "Query is currently unavailable")
      } else {
        pipe(querySchedulerActor ? msg).to(sender())
        ()
      }

    case RequestHealthStatus => sender() ! queryStatusMap

    case HealthUpdate(updatedStatusMap) => queryStatusMap = updatedStatusMap
  }
}
