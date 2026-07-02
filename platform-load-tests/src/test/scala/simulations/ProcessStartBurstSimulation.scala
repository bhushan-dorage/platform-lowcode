package simulations

import config.SimulationConfig
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class ProcessStartBurstSimulation extends Simulation {

  val processStart = scenario("ProcessStartBurst")
    .exec(TokenInjector.fetchToken)
    .exec(TokenInjector.withAuth)
    .repeat(20) {
      exec(
        http("Start Loan Application Process")
          .post("/api/v1/processes")
          .header("Authorization", "#{authHeader}")
          .body(StringBody(
            s"""{
               |  "processKey": "loan-application",
               |  "tenantId": "${SimulationConfig.tenantId}",
               |  "variables": {
               |    "applicantId": "user-#{userId}",
               |    "amount": 50000,
               |    "currency": "USD"
               |  }
               |}""".stripMargin
          ))
          .check(status.in(200, 201, 202))
          .check(responseTimeInMillis.lte(2000))
      )
      .pause(100.milliseconds)
    }

  setUp(
    processStart.inject(
      rampUsers(500).during(2.minutes),
      constantUsersPerSec(100).during(3.minutes)
    ).protocols(TokenInjector.httpProtocol)
  ).assertions(
    global.responseTime.percentile(99).lte(2000),
    global.failedRequests.percent.lte(1.0)
  )
}
