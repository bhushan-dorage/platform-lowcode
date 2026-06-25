package simulations

import io.gatling.core.Predef._

// Run individual simulations with:
//   mvn gatling:test -Dgatling.simulationClass=simulations.ProcessStartBurstSimulation
//   mvn gatling:test -Dgatling.simulationClass=simulations.TaskInboxLoadSimulation
//   mvn gatling:test -Dgatling.simulationClass=simulations.TaskClaimCompleteSimulation
//   mvn gatling:test -Dgatling.simulationClass=simulations.FormSubmissionSimulation
class AllSimulations extends Simulation {
  setUp()
}
