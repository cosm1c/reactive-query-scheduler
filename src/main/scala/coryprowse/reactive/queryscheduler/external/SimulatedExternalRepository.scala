package coryprowse.reactive.queryscheduler.external

import java.time.Instant

import coryprowse.reactive.queryscheduler.external.ExternalRepository.{ExternalQuery, ExternalResult}

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

object SimulatedExternalRepository {

  val minDuration = 20 milliseconds
  val maxDuration = 100 milliseconds

  private val durationDifferenceMs: Int = (maxDuration - minDuration).toMillis.toInt
  private val minDurationMs: Long = minDuration.toMillis
}

class SimulatedExternalRepository extends ExternalRepository {

  import SimulatedExternalRepository._

  def executeQuery(query: ExternalQuery): ExternalResult = {
    val simulatedQueryTime: Long = Random.nextInt(durationDifferenceMs) + minDurationMs
    Thread.sleep(simulatedQueryTime)
    s"Responded after ${simulatedQueryTime}ms at ${Instant.now()}"
  }

}
