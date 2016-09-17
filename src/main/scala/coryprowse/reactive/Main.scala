package coryprowse.reactive

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatchers.RemainingPath
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import coryprowse.reactive.queryscheduler.QuerySchedulerActor
import coryprowse.reactive.queryscheduler.external.ExternalRepository.{ExecuteExternalQuery, ExternalQuery, ExternalQueryResult}
import coryprowse.reactive.queryscheduler.external.SimulatedExternalRepository

import scala.concurrent.duration._
import scala.io.StdIn
import scala.language.postfixOps

object Main extends LazyLogging {

  def main(args: Array[String]): Unit = {

    implicit val system = ActorSystem("ReactiveQueryScheduler")
    implicit val executionContext = system.dispatcher

    implicit val materializer = ActorMaterializer()
    implicit val timeout = Timeout(5 minutes)

    //    implicit val queryMarshaller: ToResponseMarshaller[ExternalQuery] = ???
    //    implicit val queryUnMarshaller: FromRequestUnmarshaller[ExternalQuery] = ???
    //    implicit val responseMarshaller: ToResponseMarshaller[ExternalResult] = ???
    //    implicit val responseUnMarshaller: FromRequestUnmarshaller[ExternalResult] = ???

    val querySchedulerActor = system.actorOf(QuerySchedulerActor.props(new SimulatedExternalRepository))

    val route =
      path("executeQuery") {
        post {
          decodeRequest {
            entity(as[ExternalQuery]) { query =>
              complete {
                (querySchedulerActor ? ExecuteExternalQuery(query)).mapTo[ExternalQueryResult].map(_.result)
              }
            }
          }
        }
      } ~
        pathEndOrSingleSlash {
          get {
            getFromResource("ui/index.html")
          }
        } ~
        path(RemainingPath) { filePath =>
          getFromResource("ui/" + filePath)
        }

    val bindingFuture = Http().bindAndHandle(route, "0.0.0.0", 8080)

    println(s"Server online at http://0.0.0.0:8080/\nPress RETURN to stop...")
    StdIn.readLine()

    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ ⇒ system.terminate())
  }
}
