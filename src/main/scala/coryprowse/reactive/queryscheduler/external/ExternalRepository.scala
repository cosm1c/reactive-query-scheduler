package coryprowse.reactive.queryscheduler.external

import java.time.Instant

import scala.language.postfixOps

object ExternalRepository {

  case class ExternalQuery(queryName: String, query: String)
  type ExternalResult = String

  case class ExecuteExternalQuery(query: ExternalQuery)

  case class ExternalQueryResult(startInstant: Instant, endInstant: Instant, query: ExternalQuery, result: ExternalResult)

}

trait ExternalRepository {

  import ExternalRepository._

  def executeQuery(query: ExternalQuery): ExternalResult
}
