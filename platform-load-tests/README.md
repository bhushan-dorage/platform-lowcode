# Platform Load Tests (Gatling 3.9)

## Simulations

| Simulation | Users | Duration | p99 Target |
|-----------|-------|----------|-----------|
| ProcessStartBurst | 500 VUs, 20 starts each = 10,000 total | 5 min | < 2,000 ms |
| TaskInboxLoad | 200 VUs polling every 10s | 10 min | < 500 ms |
| TaskClaimComplete | 100 VUs, 10 claim+complete each | ~15 min | < 200 ms claim |
| FormSubmission | 300 VUs concurrent | 5 min | < 1,000 ms |

## Running

```bash
# Run a specific simulation
mvn gatling:test -Dgatling.simulationClass=simulations.ProcessStartBurstSimulation

# With custom target
PLATFORM_BASE_URL=http://staging.platform.example.com \
CLIENT_ID=load-test \
CLIENT_SECRET=secret \
TENANT_ID=tenant-perf \
mvn gatling:test -Dgatling.simulationClass=simulations.TaskInboxLoadSimulation
```

## Viewing Results

Results are written to `target/gatling/`. Open `index.html` in a browser.

For live metrics, configure `INFLUX_URL` and view in Grafana using the Gatling dashboard.
