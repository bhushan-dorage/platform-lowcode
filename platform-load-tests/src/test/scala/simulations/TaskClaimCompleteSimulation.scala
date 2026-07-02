package simulations

import config.SimulationConfig
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class TaskClaimCompleteSimulation extends Simulation {

  val claimAndComplete = scenario("TaskClaimComplete")
    .exec(TokenInjector.fetchToken)
    .exec(TokenInjector.withAuth)
    .repeat(10) {
      exec(
        http("Get Available Task")
          .get("/api/v1/tasks?pageSize=1&status=OPEN")
          .header("Authorization", "#{authHeader}")
          .check(status.is(200))
          .check(jsonPath("$.content[0].id").saveAs("taskId"))
      )
      .doIf(session => session.contains("taskId")) {
        exec(
          http("Claim Task")
            .post("/api/v1/tasks/#{taskId}/claim")
            .header("Authorization", "#{authHeader}")
            .body(StringBody("{}"))
            .check(status.in(200, 409))
            .check(responseTimeInMillis.lte(200))
        )
        .pause(500.milliseconds)
        .exec(
          http("Complete Task")
            .post("/api/v1/tasks/#{taskId}/complete")
            .header("Authorization", "#{authHeader}")
            .body(StringBody("""{"outcome":"APPROVED","variables":{"decision":"approved"}}"""))
            .check(status.in(200, 204))
        )
      }
      .pause(1.second)
    }

  setUp(
    claimAndComplete.inject(
      rampUsers(100).during(1.minute)
    ).protocols(TokenInjector.httpProtocol)
  ).assertions(
    global.responseTime.percentile(99).lte(1000),
    details("Claim Task").failedRequests.percent.lte(0.0)
  )
}
