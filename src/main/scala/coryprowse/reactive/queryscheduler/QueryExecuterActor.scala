package coryprowse.reactive.queryscheduler

import java.time.Instant

import akka.actor.{Actor, Props}
import coryprowse.reactive.queryscheduler.external.ExternalRepository
import coryprowse.reactive.queryscheduler.external.ExternalRepository.{ExecuteExternalQuery, ExternalQueryResult}

import scala.language.postfixOps

object QueryExecuterActor {
  def props(externalRepository: ExternalRepository) = Props(new QueryExecuterActor(externalRepository: ExternalRepository))
}

class QueryExecuterActor(externalRepository: ExternalRepository) extends Actor {

  def receive = {
    case ExecuteExternalQuery(query) =>
      val startInstant = Instant.now()
      val externalResult = externalRepository.executeQuery(query)
      val endInstant = Instant.now()
      sender() ! ExternalQueryResult(startInstant, endInstant, query, externalResult)
  }
}
