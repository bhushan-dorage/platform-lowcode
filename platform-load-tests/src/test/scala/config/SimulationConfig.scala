package config

object SimulationConfig {
  val baseUrl: String   = sys.env.getOrElse("PLATFORM_BASE_URL", "http://localhost:8000")
  val tokenUrl: String  = sys.env.getOrElse("TOKEN_URL", "http://localhost:8080/realms/platform/protocol/openid-connect/token")
  val clientId: String  = sys.env.getOrElse("CLIENT_ID", "load-test-client")
  val clientSecret: String = sys.env.getOrElse("CLIENT_SECRET", "load-test-secret")
  val tenantId: String  = sys.env.getOrElse("TENANT_ID", "load-test-tenant")

  // Influx / Grafana
  val influxUrl: String = sys.env.getOrElse("INFLUX_URL", "http://localhost:8086")
  val influxDb: String  = sys.env.getOrElse("INFLUX_DB", "gatling")
}
