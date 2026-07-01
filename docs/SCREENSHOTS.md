# Screenshot Inventory

Tracks every screenshot this project needs, where it lives, and what it's evidence of.
Each row maps to a specific claim made in `README.md` — the goal is no claim in the
README rests on text alone when a screenshot could prove it.

Status legend: ✅ captured · ⏳ planned, not yet captured

## Architecture diagrams

| Filename | Status | Description |
|---|---|---|
| `docs/architecture.png` | ✅ (2026-06-30) | Structural diagram: laptop CI stack (Jenkins/SonarQube/app containers, DooD, Docker network), GitHub, and Azure infrastructure (VNet, ACR, AKS node pool, kubelet identity, state backend). |
| `docs/pipeline.png` | ✅ (2026-06-30) | Pipeline/flow diagram: all 9 stages with quality gates, DEPLOY_ENABLED gate separating CI from CD, success/failure paths. |

## App layer

| Filename | Status | Description | README section |
|---|---|---|---|
| `docs/app-ui-welcome.png` | ⏳ | Welcome page, Northwind navy theme, Bootstrap 5 nav. | The app |
| `docs/app-ui-quote-form.png` | ✅ (2026-06-30) | Request-a-quote form (driver + vehicle + coverage fields). | The app |
| `docs/app-ui-quote-result.png` | ✅ (2026-06-30) | A priced quote result, showing the full rating-factor breakdown. | The app |
| `docs/app-tests-passing.png` | ✅ (2026-06-30) | CLI: `./mvnw test` output, 10/10 passing. | The app |
| `docs/app-actuator-health.png` | ✅ (2026-06-26) | Browser: `/actuator/health` response showing UP. | The app |

## CI pipeline run

| Filename | Status | Description | README section |
|---|---|---|---|
| `docs/jenkins-dashboard.png` | ✅ (2026-06-30) | Jenkins UI: dashboard showing both pipeline jobs. | CI/CD pipeline |
| `docs/jenkins-pipeline-stages-green.png` | ⏳ | Jenkins stage view: all 6 CI-only stages green (Checkout → Build & Test → SonarQube Analysis → Quality Gate → Docker Build → Trivy Scan). | CI/CD pipeline |
| `docs/jenkins-build-test-junit.png` | ✅ (2026-06-30) | Jenkins: JUnit test results report, 10 tests passed. | CI/CD pipeline |
| `docs/sonarqube-dashboard.png` | ✅ (2026-06-30) | SonarQube: project dashboard, quality gate status. | CI/CD pipeline |
| `docs/sonarqube-quality-gate-passed.png` | ✅ (2026-06-30) | SonarQube: quality gate detail showing "Passed". | CI/CD pipeline |
| `docs/sonarqube-version-status.png` | ✅ (2026-06-30) | CLI: `curl http://localhost:9000/api/system/status` confirming version 26.6.0.123539. | CI/CD pipeline |
| `docs/trivy-scan-clean.png` | ✅ (2026-06-30) | Jenkins console: Trivy Scan stage output, zero CRITICAL/HIGH findings after suppression. | CI/CD pipeline |

## CD pipeline run

| Filename | Status | Description | README section |
|---|---|---|---|
| `docs/jenkins-pipeline-cd-stages-green.png` | ✅ (2026-06-30) | Jenkins stage view: all 9 stages green including Push to ACR / Deploy to AKS / Smoke Check. | CI/CD pipeline |
| `docs/acr-image-pushed.png` | ✅ (2026-06-30) | CLI: `az acr repository show-tags` showing the build-numbered tag in the registry. | CI/CD pipeline |
| `docs/aks-rollout-status.png` | ✅ (2026-06-30) | CLI: `kubectl get pods -n default` showing 2/2 ready, 0 restarts. | CI/CD pipeline |
| `docs/aks-smoke-check-pass.png` | ✅ (2026-06-30) | Jenkins console: Smoke Check stage curl returning `{"status":"UP"}`. | CI/CD pipeline |
| `docs/portal-aks-workloads.png` | ✅ (2026-06-30) | Portal: AKS Workloads blade showing `northwind-quote` Deployment, 2/2 pods running. | CI/CD pipeline |
| `docs/portal-acr-overview.png` | ✅ (2026-06-30) | Portal: ACR overview blade — name, login server, SKU. | Infra |
| `docs/portal-resource-group-overview.png` | ✅ (2026-06-30) | Portal: `rg-northwind-quote-dev` resource group listing ACR, AKS cluster, VNet. | Infra |
| `docs/terraform-apply-success.png` | ✅ (2026-06-30) | CLI: `terraform apply` output — "Apply complete! 7 added, 0 changed, 0 destroyed." | Infra |
| `docs/terraform-destroy-success.png` | ✅ (2026-06-30) | CLI: `terraform destroy` output — "Destroy complete! 7 destroyed." | Cost-conscious design |

## Problems found and fixed

| Filename | Status | Description | Problem # |
|---|---|---|---|
| `docs/problem-i18n-fallback-before.png` | ⏳ | Evidence of the i18n resource-bundle fallback bug before the fix. | 1 |
| `docs/problem-i18n-fallback-after.png` | ⏳ | Same screen after fix — correct text rendering. | 1 |
| `docs/problem-driver-id-null-error.png` | ⏳ | The `NULL not allowed for DRIVER_ID` error before the bidirectional `@ManyToOne` fix. | 2 |
| `docs/problem-driver-id-null-fixed.png` | ⏳ | A vehicle successfully saved against a driver after the fix. | 2 |
| `docs/problem-trivy-cve-before.png` | ⏳ | Trivy scan showing the `p11-kit` CVE-2026-2100 HIGH finding (exit code 1). | 3 |
| `docs/problem-trivy-cve-after.png` | ⏳ | Trivy scan passing with CVE suppressed in `.trivyignore`. | 3 |
| `docs/problem-jarlauncher-failure.png` | ⏳ | Container failing to start with plain `java -jar app.jar` against a layered-extracted image. | 4 |
| `docs/problem-jarlauncher-fixed.png` | ⏳ | Container starting cleanly with the `JarLauncher` entrypoint. | 4 |
| `docs/problem-docker-socket-perms.png` | ⏳ | Docker-socket permission error inside the Jenkins container before `group_add: ["0"]`. | 5 |
| `docs/problem-sonar-webhook-pending.png` | ⏳ | Quality Gate stage hung at `PENDING` before the SonarQube webhook was configured. | 6 |
| `docs/problem-sonar-webhook-fixed.png` | ⏳ | Quality Gate resolving immediately after the webhook fix. | 6 |

## Notes

- When a shot is captured, flip its status to ✅ and add the capture date.
- CD pipeline + infra shots (acr-image-pushed, aks-*, portal-*, terraform-*) are recaptured
  each time infra is reprovisioned — update the date stamp when refreshed.
- Problems 1–6 are historical (bugs fixed before this session began); capture if you can
  reproduce, otherwise leave ⏳ — the written narrative stands on its own.
