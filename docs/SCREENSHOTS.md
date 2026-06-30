# Screenshot Inventory

Tracks every screenshot this project needs, where it lives, and what it's evidence of.
Each row maps to a specific claim made in `README.md` — the goal is no claim in the
README rests on text alone when a screenshot could prove it. Filenames are called out
at the moment of capture during the build, not invented retroactively.

Status legend: ✅ captured · ⏳ planned, not yet captured

## Architecture diagrams

| Filename | Status | Description |
|---|---|---|
| `docs/architecture-structural.png` | ⏳ | Structural diagram: app container, Jenkins container, SonarQube container, Docker network, host ports — the "what exists and how it's wired" view. |
| `docs/architecture-pipeline.png` | ⏳ | Pipeline/flow diagram: Checkout → Build & Test → SonarQube Analysis → Quality Gate → Docker Build → Trivy Scan, annotated with what each stage produces/consumes. |

## App layer

| Filename | Status | Description | README section |
|---|---|---|---|
| `docs/app-ui-welcome.png` | ⏳ | Welcome page, Northwind navy theme, Bootstrap 5 nav. | Evidence — app |
| `docs/app-ui-quote-form.png` | ⏳ | Request-a-quote form (driver + vehicle + coverage fields). | Evidence — app |
| `docs/app-ui-quote-result.png` | ⏳ | A priced quote result, showing the full rating-factor breakdown. | Evidence — app |
| `docs/app-tests-passing.png` | ⏳ | CLI: `./mvnw test` output, 10/10 passing. | Evidence — app |
| `docs/app-actuator-health.png` | ⏳ | CLI or browser: `/actuator/health` response showing UP, livez/readyz groups. | Evidence — app |

## Local CI stack

| Filename | Status | Description | README section |
|---|---|---|---|
| `docs/docker-ps-stack-running.png` | ⏳ | CLI: `docker ps` showing all three containers (`northwind-jenkins`, `northwind-sonarqube`, `nwq`) up and healthy. | Evidence — CI stack |
| `docs/jenkins-dashboard.png` | ⏳ | Portal (Jenkins UI): dashboard after first successful pipeline run. | Evidence — CI stack |
| `docs/sonarqube-dashboard.png` | ⏳ | Portal (SonarQube UI): project dashboard, quality gate status. | Evidence — CI stack |
| `docs/sonarqube-version-status.png` | ⏳ | CLI: `curl http://localhost:9000/api/system/status` confirming the exact running version. | Evidence — CI stack |

## CI pipeline run (end to end)

| Filename | Status | Description | README section |
|---|---|---|---|
| `docs/jenkins-pipeline-stages-green.png` | ⏳ | Portal: Jenkins stage view, all 6 CI stages green (Checkout → Build & Test → SonarQube Analysis → Quality Gate → Docker Build → Trivy Scan). | CI/CD pipeline |
| `docs/jenkins-build-test-junit.png` | ⏳ | Portal: JUnit test results trend/report published from the Build & Test stage. | CI/CD pipeline |
| `docs/sonarqube-quality-gate-passed.png` | ⏳ | Portal: SonarQube quality gate detail showing "Passed". | CI/CD pipeline |
| `docs/trivy-scan-clean.png` | ⏳ | CLI: Trivy scan output, zero CRITICAL/HIGH findings. | CI/CD pipeline |
| `docs/docker-image-built.png` | ⏳ | CLI: `docker images` showing `northwind-quote:<build-number>` and `:latest` tags produced by the pipeline. | CI/CD pipeline |

## CD pipeline run (Phase 7 complete — capture these now)

| Filename | Status | Description | README section |
|---|---|---|---|
| `docs/jenkins-pipeline-cd-stages-green.png` | ⏳ | Portal: Jenkins stage view with `DEPLOY_ENABLED=true`, all 9 stages green including Push to ACR / Deploy to AKS / Smoke Check. | CI/CD pipeline |
| `docs/acr-image-pushed.png` | ⏳ | CLI (`az acr repository show-tags --name acrnorthwindquotebba3df --repository northwind-quote`) + Portal (ACR Repositories blade): build-numbered tag present. | CI/CD pipeline |
| `docs/aks-rollout-status.png` | ⏳ | CLI: `kubectl get pods -n default` showing 2/2 ready, 0 restarts. | CI/CD pipeline |
| `docs/aks-smoke-check-pass.png` | ⏳ | Jenkins console: Smoke Check stage curl returning `{"status":"UP"}`. | CI/CD pipeline |
| `docs/portal-aks-workloads.png` | ⏳ | Portal: AKS Workloads blade showing `northwind-quote` Deployment, 2/2 pods running. | CI/CD pipeline |
| `docs/portal-acr-overview.png` | ⏳ | Portal: ACR overview blade — name, login server, SKU. | CI/CD pipeline |
| `docs/portal-resource-group-overview.png` | ⏳ | Portal: `rg-northwind-quote-dev` resource group listing ACR, AKS cluster, VNet. | Infra |
| `docs/terraform-apply-success.png` | ⏳ | CLI: `terraform apply` output — "Apply complete! 7 added, 0 changed, 0 destroyed." | Infra |
| `docs/terraform-destroy-success.png` | ⏳ | CLI: `terraform destroy` output — all dev resources removed. | Cost-conscious design |

## Problems found and fixed

| Filename | Status | Description | Problem # |
|---|---|---|---|
| `docs/problem-i18n-fallback-before.png` | ⏳ | Evidence of the i18n resource-bundle fallback bug before the fix (e.g. missing/garbled message). | 1 |
| `docs/problem-i18n-fallback-after.png` | ⏳ | Same screen after adding `messages_en.properties` + explicit basename — correct text rendering. | 1 |
| `docs/problem-driver-id-null-error.png` | ⏳ | The `NULL not allowed for DRIVER_ID` error before the bidirectional `@ManyToOne` fix. | 2 |
| `docs/problem-driver-id-null-fixed.png` | ⏳ | A vehicle successfully saved against a driver after the fix. | 2 |
| `docs/problem-trivy-cve-before.png` | ⏳ | Trivy scan showing the `p11-kit` CVE-2026-2100 HIGH finding (suppressed via `.trivyignore` — see problem #9 for why `apk upgrade` can't be used). | 3 |
| `docs/problem-trivy-cve-after.png` | ⏳ | Trivy scan passing with the CVE suppressed in `.trivyignore`. | 3 |
| `docs/problem-jarlauncher-failure.png` | ⏳ | The container failing to start with a plain `java -jar app.jar` entrypoint against a layered-extracted image. | 4 |
| `docs/problem-jarlauncher-fixed.png` | ⏳ | The container starting cleanly with the `JarLauncher` entrypoint. | 4 |
| `docs/problem-docker-socket-perms.png` | ⏳ | The Docker-socket permission error inside the Jenkins container before `group_add: ["0"]`. | 5 |
| `docs/problem-sonar-webhook-pending.png` | ⏳ | The Quality Gate stage hung at `PENDING` before the Jenkins webhook was configured in SonarQube. | 6 |
| `docs/problem-sonar-webhook-fixed.png` | ⏳ | The Quality Gate resolving immediately after the webhook fix, Portal view of the webhook config. | 6 |
| `docs/problem-azcli-trixie-failure.png` | ⏳ | Jenkins image build failing: "The repository 'https://packages.microsoft.com/repos/azure-cli trixie Release' does not have a Release file." | 7 |
| `docs/problem-azcli-trixie-fixed.png` | ⏳ | Jenkins image building cleanly after pinning the Azure CLI apt source to `bookworm`. | 7 |
| `docs/problem-arm64-exec-format-error.png` | ⏳ | AKS pod in CrashLoopBackOff; `kubectl logs` showing `exec format error` — amd64 image on ARM64 node. | 8 |
| `docs/problem-qemu-alpine-run-failure.png` | ⏳ | `docker buildx build --platform linux/arm64` failing at `RUN apk upgrade` with `exec format error` under QEMU DooD. | 9 |

## Notes

- Every pairing above should be **CLI + Portal/UI** where both exist, per the project's
  standing screenshot-pairing discipline — a CLI screenshot proves the underlying state,
  a Portal/UI screenshot proves it's visible the way a human operator would see it.
- Phase 7 (ACR + AKS) is now provisioned — the CD pipeline screenshot entries above are
  ready to capture before running `terraform destroy`.
- When a shot is captured, flip its status to ✅ and add the capture date as a trailing
  note, e.g. `✅ (2026-06-30)`.
