package coryprowse.reactive.health

import java.time.Clock

import akka.actor.{Actor, ActorLogging, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.model._
import akka.pattern.pipe
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.Timeout
import coryprowse.reactive.health.HealthActor.QueryStatus
import coryprowse.reactive.health.HealthCheckerActor.{HealthCheckTick, TotalHealthUpdate, pollInterimDuration}
import coryprowse.reactive.queryscheduler.external.ExternalRepository.ExternalQuery

import scala.collection.immutable
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

object HealthCheckerActor {

  // TODO: Move to config
  val pollInterimDuration = 5 seconds

  case object HealthCheckTick

  case class TotalHealthUpdate(updatedStatusMap: Map[String, QueryStatus])

  def props()(implicit clock: Clock) = Props(new HealthCheckerActor())
}

class HealthCheckerActor()(implicit clock: Clock) extends Actor with ActorLogging {

  implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))
  implicit val executionContext = context.system.dispatcher
  implicit val timeout = Timeout(15 seconds)

  val http = Http(context.system)
  val healthMBeanActor = context.actorOf(HealthMBeanActor.props)

  def scheduleNextPoll() = context.system.scheduler.scheduleOnce(pollInterimDuration, self, HealthCheckTick)

  self ! HealthCheckTick

  var statusMap = immutable.Map.empty[String, QueryStatus]

  def receive = {
    case TotalHealthUpdate(updatedStatusMap) =>
      scheduleNextPoll()
      statusMap = updatedStatusMap

    case HealthCheckTick =>
      pipe(fetchHealthUpdate())
        .to(self)
        .to(context.parent)
        .to(healthMBeanActor)
      ()
  }

  private def fetchHealthUpdate(): Future[TotalHealthUpdate] = {
    // TODO: generate map of queryName -> jsonPostBody
    val queriesToTest = List(
      ExternalQuery("a", """{"queryName":"a","query":"a query"}""")
    )

    Future.sequence(queriesToTest.map(checkQueryHealth))
      .map(list => list.map(i => (i.queryName, i)))
      .map(_.toMap[String, QueryStatus])
      .map(TotalHealthUpdate)
  }

  private def checkQueryHealth(query: ExternalQuery): Future[QueryStatus] =
    http.singleRequest(
      HttpRequest(
        method = HttpMethods.POST,
        uri = "http://localhost:8080/executeQuery?bypassHealthCheck=true",
        entity = HttpEntity(`application/json`, query.query)
      ))
      .map { httpResponse =>
        // TODO: Include any error message here
        QueryStatus(query.queryName, httpResponse.status.isSuccess(), clock.instant())
      }
}
