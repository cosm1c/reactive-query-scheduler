package coryprowse.reactive.queryscheduler

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

import scala.concurrent.duration._
import scala.language.postfixOps

class GatlingPerformanceTest extends Simulation {

  val httpConf = http
    .warmUp("http://www.google.com")
    .baseURL("http://localhost:8080")
    .disableCaching
    .disableAutoReferer

  val queriesFeeder: IndexedSeq[Map[String, String]] =
    ('a' to 'j').map(ch =>
      Map(
        "queryName" -> s"$ch",
        "postBody" -> s"""{query:"$ch"}"""
      ))

  setUp(
    scenario("QueryScheduler")
      .feed(queriesFeeder.random)
      .exec(http("${queryName}")
        .post("/executeQuery")
        .body(StringBody("${postBody}")))
      .inject(
        rampUsers(1000) over (10 seconds)
      )
  ).protocols(httpConf)
}
