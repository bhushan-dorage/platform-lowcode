package simulations

import config.SimulationConfig
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class FormSubmissionSimulation extends Simulation {

  private def buildFormData(fieldCount: Int): String = {
    val fields = (1 to fieldCount).map(i => s""""field$i": "value-$i"""").mkString(",\n")
    s"""{
       |  "formKey": "complex-form",
       |  "tenantId": "${SimulationConfig.tenantId}",
       |  "data": {
       |    $fields
       |  }
       |}""".stripMargin
  }

  val formSubmission = scenario("FormSubmission")
    .exec(TokenInjector.fetchToken)
    .exec(TokenInjector.withAuth)
    .exec(
      http("Submit Complex Form")
        .post("/api/v1/forms/submit")
        .header("Authorization", "#{authHeader}")
        .body(StringBody(buildFormData(50)))
        .check(status.in(200, 201, 202))
        .check(responseTimeInMillis.lte(1000))
    )

  setUp(
    formSubmission.inject(
      rampUsers(300).during(2.minutes),
      constantUsersPerSec(50).during(3.minutes)
    ).protocols(TokenInjector.httpProtocol)
  ).assertions(
    global.responseTime.percentile(99).lte(1000),
    global.failedRequests.percent.lte(1.0)
  )
}
