import java.time.LocalDate

import cats.effect.Resource
import com.typesafe.scalalogging.LazyLogging
import io.circe.{Json, JsonObject}
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import monix.execution.Scheduler
import monix.execution.schedulers.SchedulerService
import org.http4s.circe._
import org.http4s._
import org.http4s.client.Client

import scala.concurrent.TimeoutException
import scala.concurrent.duration._

import org.http4s.client.asynchttpclient.AsyncHttpClient
import org.http4s.client.blaze.BlazeClientBuilder

object HttpClientPoC extends LazyLogging {

  private implicit val encoder: EntityEncoder[Task, JsonObject] = jsonEncoderOf[Task, JsonObject]

  def main(args: Array[String]): Unit = {

    val clientScheduler: SchedulerService = Scheduler.io(s"http-pool")

    val clientResource: Resource[Task, Client[Task]] = AsyncHttpClient.resource[Task](
      new org.asynchttpclient.DefaultAsyncHttpClientConfig.Builder()
        .setMaxConnectionsPerHost(200)
        .setMaxConnections(400)
        .setRequestTimeout(30000)
        .build()
    )
    // val clientResource: Resource[Task, Client[Task]] =
    //   BlazeClientBuilder[Task](clientScheduler).withSocketKeepAlive(true).resource

    val _ = clientResource
      .use { client =>
        val workers: Seq[Task[List[Unit]]] = (1 to 10).map { n =>
          (for {
            work <- Task
              .traverse(1 to 10000 toList) { x =>
                post(client, s"abc-$x")
                  .onErrorRestart(5)
                  .asyncBoundary(clientScheduler)
                  .logTimed(s"worker $n job $x")
              }
          } yield {
            work
          })
        }
        Task.gatherUnordered(workers).logTimed("completed")
      }
      .runSyncUnsafe(10 minutes)
  }

  def post(client: Client[Task], s: String): Task[Unit] = {
    val request = Request[Task](
      method = Method.POST,
      uri = Uri.fromString(s"http://localhost/post").getOrElse(Uri())
    ).withEntity(JsonObject("abc" -> Json.fromString(s)))

    client.fetch(request) { response =>
      response.attemptAs[Json].value.map { r =>
        r match {
          case Left(e)  => logger.error(e.getMessage())
          case Right(json) =>
            if (json.hcursor.downField("json").downField("abc").as[String] == Right(s)) ()
            else
              logger.error(Json.fromString(s) + ":" + json.hcursor.downField("json").downField("abc").as[Json].toString)
        }
      }
    }
  }

  implicit class TaskLogTimed[A](task: Task[A]) {

    def logTimed(message: String): Task[A] = {
      for {
        timed <- task.timed
        (duration, t) = timed
        _ <- Task(logger.trace(message + s" in ${duration.toMillis} ms"))
      } yield {
        t
      }
    }
  }
}
