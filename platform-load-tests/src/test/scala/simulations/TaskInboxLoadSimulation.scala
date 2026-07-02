package simulations

import config.SimulationConfig
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class TaskInboxLoadSimulation extends Simulation {

  val taskInboxPolling = scenario("TaskInboxLoad")
    .exec(TokenInjector.fetchToken)
    .exec(TokenInjector.withAuth)
    .during(10.minutes) {
      exec(
        http("Poll Task Inbox")
          .get("/api/v1/tasks?pageSize=20&status=OPEN")
          .header("Authorization", "#{authHeader}")
          .check(status.in(200, 204))
          .check(responseTimeInMillis.lte(500))
      )
      .pause(10.seconds)
    }

  setUp(
    taskInboxPolling.inject(
      rampUsers(200).during(1.minute)
    ).protocols(TokenInjector.httpProtocol)
  ).assertions(
    global.responseTime.percentile(99).lte(500),
    global.failedRequests.percent.lte(1.0)
  )
}
