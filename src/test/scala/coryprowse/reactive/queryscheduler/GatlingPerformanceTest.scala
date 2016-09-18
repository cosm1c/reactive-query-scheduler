package coryprowse.reactive.queryscheduler

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

import scala.language.postfixOps

class GatlingPerformanceTest extends Simulation {

  val httpConf = http
    .warmUp("http://localhost:8080")
    .baseURL("http://localhost:8080")
    .disableCaching
    .disableAutoReferer

  val queriesFeeder: IndexedSeq[Map[String, String]] =
    ('a' to 'j').map(ch =>
      Map(
        "queryName" -> s""""$ch"""",
        "postBody" -> s"""{"queryName":"$ch","query":"$ch query"}"""
      ))

  setUp(
    scenario("QueryScheduler")
      .feed(queriesFeeder.random)
      .exec(http("${queryName}")
        .post("/executeQuery")
        .header("Content-Type", "application/json")
        .body(StringBody("${postBody}")))
      .inject(
        //atOnceUsers(1)
        rampUsers(1000) over (10 seconds)
      )
  ).protocols(httpConf)
}
