package coryprowse.reactive.queryscheduler

import java.time.{Clock, Duration, Instant}

import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.{ask, pipe}
import akka.routing.BalancingPool
import akka.util.Timeout
import coryprowse.reactive.queryscheduler.QuerySchedulerActor.JobCacheEntry
import coryprowse.reactive.queryscheduler.external.ExternalRepository.{ExecuteExternalQuery, ExternalQuery, ExternalQueryResult}
import coryprowse.reactive.queryscheduler.external.{ExternalRepository, SimulatedExternalRepository}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

object QuerySchedulerActor {

  // TODO: Move to config
  val cacheTimeMs: Long = (1 seconds).toMillis

  case class JobCacheEntry(query: ExternalQuery,
                           eventualResult: Future[ExternalQueryResult],
                           lastStarted: Instant,
                           maybeLastEnded: Option[Instant] = None) {

    def isFresh()(implicit clock: Clock): Boolean = queryInProgress || cacheEntryFresh

    def queryInProgress: Boolean = !eventualResult.isCompleted

    def cacheEntryFresh()(implicit clock: Clock): Boolean = {
      val now = clock.instant()
      maybeLastEnded.exists {
        lastEnded => Duration.between(lastEnded, now).toMillis <= cacheTimeMs
      }
    }
  }

  def props(repository: ExternalRepository, clock: Clock = Clock.systemUTC()) = Props(new QuerySchedulerActor(repository)(clock))
}

class QuerySchedulerActor(repository: ExternalRepository)(implicit val clock: Clock) extends Actor with ActorLogging {

  implicit val executionContext = context.system.dispatcher
  implicit val timeout = Timeout(2 seconds)

  val executerPool = context.actorOf(BalancingPool(5).props(QueryExecuterActor.props(new SimulatedExternalRepository)), "querySchedulerPool")
  var queryCache = Map.empty[ExternalQuery, JobCacheEntry]

  def receive = {

    case msg@ExecuteExternalQuery(query) =>
      queryCache.get(query)
        .filter(_.isFresh).map(entry => pipe(entry.eventualResult))
        .getOrElse {
          val startedInstant = clock.instant()
          val eventualResult: Future[ExternalQueryResult] = (executerPool ? msg).mapTo[ExternalQueryResult]
          queryCache += query -> JobCacheEntry(query, eventualResult, startedInstant)
          pipe(eventualResult).to(self)
        }
        .to(sender())
      ()

    case ExternalQueryResult(startInstant, endInstant, query, result) =>
      queryCache.get(query) match {
        case Some(entry) =>
          val endedInstant = clock.instant()
          queryCache += query -> entry.copy(maybeLastEnded = Some(endedInstant))

        case None => log.error("Received ExternalQueryResult for unknown query - should have been added at query time", query)
      }
  }
}
