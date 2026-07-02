package simulations

import config.SimulationConfig
import io.gatling.core.Predef._
import io.gatling.http.Predef._

object TokenInjector {

  val httpProtocol = http
    .baseUrl(SimulationConfig.baseUrl)
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .header("X-Tenant-ID", SimulationConfig.tenantId)

  val fetchToken: io.gatling.core.structure.ChainBuilder =
    exec(
      http("Fetch OAuth2 Token")
        .post(SimulationConfig.tokenUrl)
        .header("Content-Type", "application/x-www-form-urlencoded")
        .formParam("grant_type", "client_credentials")
        .formParam("client_id", SimulationConfig.clientId)
        .formParam("client_secret", SimulationConfig.clientSecret)
        .check(status.is(200))
        .check(jsonPath("$.access_token").saveAs("accessToken"))
    )

  def withAuth: io.gatling.core.structure.ChainBuilder =
    exec(session =>
      session.set("authHeader", s"Bearer ${session("accessToken").as[String]}")
    )
}
