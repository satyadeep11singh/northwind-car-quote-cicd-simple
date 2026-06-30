# Northwind Mutual — Car Quote Generator (CI/CD with Jenkins)

A Java Spring Boot car-insurance quote application, built and quality-gated by a
**self-hosted Jenkins pipeline**: Checkout → Maven build/test → SonarQube analysis +
quality gate → Docker build → Trivy vulnerability scan. Deploying that scanned image to
AKS is the next phase of this project — see [On the horizon](#on-the-horizon).

> **Disclaimer:** "Northwind Mutual" is a fictional company. All code, infrastructure,
> and data in this project are for personal learning purposes only and do not represent
> or use any real systems, data, or processes from any actual insurance company or
> employer.

---

## What this project demonstrates

Projects 1–3 in this portfolio use Azure DevOps Pipelines, a managed CI/CD service.
This project deliberately uses **Jenkins** instead — a self-hosted CI server you install,
configure, patch, and operate yourself. That's a different skill: instead of trusting a
managed control plane, you're responsible for the controller's plugins, its Docker socket
access, its credentials, and its integration with a second self-hosted tool (SonarQube).

The app itself is a small, honest insurance-domain exercise: driver → vehicle → coverage
choice → calculated premium, with every rating factor visible on the result page rather
than a single opaque number. The interesting engineering is in the pipeline around it —
proving a real build-test-scan loop works **before** any of it touches billable Azure
infrastructure.

### Methodology: manual first, then automate

Every stage in the Jenkinsfile was run by hand first — `./mvnw clean verify` at a
terminal, a manual `docker build`, a manual `trivy image` scan — before it was wired into
Jenkins. That order matters: when a pipeline stage fails, you want to already know what
success looks like from having done it manually, so you can tell a real bug from a CI
environment problem in seconds instead of guessing.

### Project status

The full CI/CD pipeline is complete and verified end-to-end: app build → test → Sonar
quality gate → Docker cross-build (ARM64) → Trivy scan → push to ACR → deploy to AKS →
smoke check. Infra (ACR + AKS) is torn down between sessions and reprovisioned via
Terraform when needed.

---

## Architecture

### Structural view

Three containers on one Docker network: the app itself, the Jenkins controller, and
SonarQube. Jenkins builds Docker images via **Docker-outside-of-Docker (DooD)** — it
doesn't run its own Docker daemon, it talks to the host's Docker Desktop engine through a
mounted socket. This keeps the whole stack runnable on a laptop with no cloud dependency
during development.

![Structural diagram](./docs/architecture-structural.png)
<!-- TODO: draw.io export — app/Jenkins/SonarQube containers, ci network, host port mappings 8080/8081/9000, DooD socket mount -->

### Pipeline flow

```
Checkout → Build & Test → SonarQube Analysis → Quality Gate → Docker Build → Trivy Scan
         → Push to ACR → Deploy to AKS → Smoke Check
```

Each stage gates the next: tests must pass before Sonar analysis runs, the quality gate
must pass before an image is built, and the image must scan clean before it's pushed.
The three CD stages (Push to ACR, Deploy to AKS, Smoke Check) are gated behind a
`DEPLOY_ENABLED` pipeline parameter so the CI half can run standalone against the free
local stack, and the full 9-stage run fires only when infra is provisioned.

![Pipeline flow diagram](./docs/architecture-pipeline.png)
<!-- TODO: draw.io export — six pipeline stages, what each produces/consumes, gate points -->

---

## Resource list

Everything in this phase runs locally — there is no billable Azure infrastructure yet.

| Component | Image / Tool | Host port | Notes |
|---|---|---|---|
| App | `northwind-quote:local` (this repo's `dockerfile`) | `8080` | Spring Boot, H2 in-memory DB, Actuator health/info/prometheus exposed |
| Jenkins | `northwind-jenkins:local` (`tools/jenkins/dockerfile`, based on `jenkins/jenkins:lts-jdk21`) | `8081` (UI), `50000` (agent, unused) | Docker CLI + Trivy baked in; plugins pre-installed via `jenkins-plugin-cli` |
| SonarQube | `sonarqube:community` | `9000` | Code quality analysis + quality gate, webhooks back to Jenkins |

All three are defined in [`tools/docker-compose.yml`](./tools/docker-compose.yml) on a
shared `ci` Docker network, with named volumes (`jenkins_home`, `sonarqube_data`,
`sonarqube_extensions`, `sonarqube_logs`) persisting state across container restarts.

---

## Repository structure

```
/src                  # Spring Boot app source (Driver / Vehicle / Quote domain)
/tools
  docker-compose.yml  # local CI stack: Jenkins + SonarQube
  /jenkins
    dockerfile        # custom Jenkins controller image (Docker CLI + buildx, Trivy, az CLI, kubectl, plugins)
/k8s
  deployment.yaml     # AKS Deployment: 2 replicas, liveness/readiness probes, no imagePullSecrets
  service.yaml        # ClusterIP Service (port-forward only for this phase)
/infra
  /modules
    /network          # reusable: subnet for AKS node pool
    /registry         # reusable: Azure Container Registry (Basic SKU)
    /aks              # reusable: AKS cluster (ARM64 node pool, SystemAssigned identity, kubelet identity output)
  /environments
    /dev              # composes the three modules; ACR+AKS+AcrPull role assignment
dockerfile            # multi-stage app image (build → extract → runtime, ARM64 via --platform cross-build)
Jenkinsfile            # CI/CD pipeline definition (9 stages, DEPLOY_ENABLED param gates CD half)
.trivyignore          # CVE-2026-2100 (p11-kit) suppressed — see "Problems found and fixed" #9
/docs                 # architecture diagrams, screenshot inventory
```

---

## The app

Built on a **PetClinic structural skeleton** (Spring Boot + Thymeleaf + Spring Data JPA),
with the entire domain swapped from pets/owners to car insurance: `Driver`, `Vehicle`,
`Quote`, with a transparent `QuoteCalculationService` that multiplies five independent
rating factors (driver age/experience, vehicle age, usage type, liability limit,
deductible) against an $800 base rate. The calculation is intentionally illustrative
rather than actuarially accurate — the goal is a formula a reader can follow end-to-end,
with every intermediate factor stored on the resulting `Quote` so the UI can show a full
breakdown instead of a single number.

UI is real **Bootstrap 5 + Bootstrap Icons** (via WebJars), navy Northwind theme. 10 unit
tests cover the rating logic; `./mvnw test` passes 10/10. Actuator exposes
`health`/`info`/`prometheus`, with `/livez` and `/readyz` probe groups already enabled
(`management.endpoint.health.probes.add-additional-paths=true`) — wired in ahead of time
for the AKS liveness/readiness probes planned in Phase 8.

![Quote request form](./docs/app-ui-quote-form.png)

![Quote result with rating breakdown](./docs/app-ui-quote-result.png)

![Unit tests passing](./docs/app-tests-passing.png)

---

## Dockerfile

Three-stage build (`./dockerfile`):

1. **Build** — `maven:3.9-eclipse-temurin-21`, runs the Maven wrapper to produce the fat
   JAR. The `.mvn`/`mvnw`/`pom.xml` layer is copied and `dependency:go-offline` run before
   source is copied, so dependency resolution is cached separately from application code.
2. **Extract** — `eclipse-temurin:21-jre`, unpacks the fat JAR into Spring Boot's
   **layered** format (`-Djarmode=tools ... extract --layers --launcher`), so Docker can
   cache rarely-changing dependency layers separately from frequently-changing application
   classes.
3. **Runtime** — `eclipse-temurin:21-jre-alpine`, JRE-only (no compiler, smaller attack
   surface), runs as a non-root user `northwind`, container-aware heap sizing via
   `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0`, and a `HEALTHCHECK` against
   `/actuator/health`.

Tests are deliberately **not** re-run inside the image build (`-DskipTests`) — they run
once, as a dedicated Jenkins stage where results are reported and the Sonar quality gate
is evaluated against them. Re-running them inside the image build would duplicate work
without adding signal.

---

## CI/CD pipeline

### Stages

| Stage | What it does |
|---|---|
| **Checkout** | `checkout scm` — explicit in logs even though Jenkins auto-checks-out for "Pipeline script from SCM" jobs |
| **Build & Test** | `./mvnw -B clean verify` — compiles, runs all 10 unit tests, packages the JAR; JUnit results published to Jenkins regardless of outcome |
| **SonarQube Analysis** | `./mvnw -B sonar:sonar` inside `withSonarQubeEnv('SonarQube')`, reusing the already-compiled classes from the previous stage |
| **Quality Gate** | `waitForQualityGate abortPipeline: true` — blocks on SonarQube's webhook callback (5-minute timeout), fails the build if the gate isn't green |
| **Docker Build** | `docker build` against the host engine via the mounted socket (DooD), tags `<build-number>` and `latest` |
| **Trivy Scan** | `trivy image --severity CRITICAL,HIGH --exit-code 1` — fails the build on any CRITICAL/HIGH finding |
| **Push to ACR** *(gated)* | `az acr login` + `docker push`, tags `<acr>.azurecr.io/northwind-quote:<build-number>` |
| **Deploy to AKS** *(gated)* | `az aks get-credentials` then `kubectl apply` of `k8s/service.yaml` and `k8s/deployment.yaml` (image tag substituted in), waits on `kubectl rollout status` |
| **Smoke Check** *(gated)* | Port-forwards the new Service and curls `/actuator/health/readiness`, confirming the rollout actually answers traffic, not just that Kubernetes reports it healthy |

Pipeline options: `timestamps()`, a 30-minute overall timeout, and
`disableConcurrentBuilds()` so overlapping runs can't race each other.

![Jenkins CD pipeline, all 9 stages green](./docs/jenkins-pipeline-cd-stages-green.png)

![SonarQube quality gate passed](./docs/sonarqube-quality-gate-passed.png)

![Trivy scan, zero findings](./docs/trivy-scan-clean.png)

### Local CI stack

Jenkins and SonarQube run as containers on this laptop (`tools/docker-compose.yml`), not
on Azure infrastructure — see [Cost-conscious design](#cost-conscious-design). Start with:

```bash
docker compose -f tools/docker-compose.yml up -d --build
```

![Jenkins dashboard](./docs/jenkins-dashboard.png)

![SonarQube dashboard](./docs/sonarqube-dashboard.png)

---

## Problems found and fixed

Each of these is a genuine bug hit during this project, not a hypothetical — included
because the diagnosis, not just the fix, is the actual signal.

### 1. i18n resource-bundle fallback was inconsistent

Spring Boot's implicit message-bundle fallback behavior across Spring Boot/JDK
combinations is a known source of inconsistency
([spring-boot#30801](https://github.com/spring-projects/spring-boot/issues/30801)). Rather
than rely on it, the fix was to ship an explicit `messages_en.properties` alongside the
base `messages.properties`, set `spring.messages.basename=messages/messages` explicitly,
and disable system-locale fallback (`spring.messages.fallback-to-system-locale=false`). The
result: `en`/`en_CA`/`en_US` all resolve predictably without depending on implicit
behavior.

### 2. `NULL not allowed for DRIVER_ID`

Saving a `Vehicle` failed with a not-null constraint violation on `DRIVER_ID`. The
relationship between `Driver` and `Vehicle` wasn't being persisted from both sides — JPA's
`@ManyToOne` association needs the owning side set explicitly, so the fix made the
Driver↔Vehicle relationship genuinely bidirectional, with the `Vehicle` side responsible
for setting its `Driver` reference before save.

### 3. Trivy CVE finding in the base image (`p11-kit` CVE-2026-2100)

The first Trivy scan against the runtime image surfaced a HIGH-severity finding in
`p11-kit`, a package that ships with Alpine but wasn't yet patched in the
`eclipse-temurin:21-jre-alpine` base image as published. The fix exists in Alpine's
package repos (`0.26.2-r0`) but can't be applied via `apk upgrade` at cross-build time
because QEMU emulation prevents executing Alpine binaries on this host (see problem #9).
The finding is suppressed in `.trivyignore` with a documented rationale and will be
removed once the upstream base image ships the patched package.

### 4. Layered JAR extraction needs the `JarLauncher` entrypoint

After switching the Docker build to Spring Boot's layered-extraction format (for better
layer caching), the obvious `ENTRYPOINT ["java", "-jar", "app.jar"]` no longer works —
there is no single `app.jar` after extraction, just a directory of layers. The fix:
`ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]`, which is
exactly what the extracted layers are structured for.

### 5. Jenkins couldn't reach the Docker socket (Docker Desktop / WSL2)

DooD builds failed with a permission error against the mounted `/var/run/docker.sock`.
The fix is `group_add: ["0"]` on the Jenkins service in `docker-compose.yml` — adding the
Jenkins container process to the root group, which on Docker Desktop's WSL2 backend owns
the socket. A user-level `usermod -aG docker jenkins`-style fix inside the image does
**not** work here, because the socket's owning GID inside Docker Desktop's WSL2 VM doesn't
match any group baked into the image at build time.

### 6. SonarQube quality gate hung at `PENDING`

The Quality Gate stage timed out waiting for a webhook callback that never arrived.
SonarQube only calls back to Jenkins if a webhook is explicitly configured — the fix was
creating one in SonarQube pointed at `http://jenkins:8080/sonarqube-webhook/` (the
container's internal hostname/port on the shared `ci` network, not the host-mapped
`8081`). Once configured, the gate result returns within seconds of analysis completing.

### 7. Azure CLI apt repo has no Debian Trixie release

`jenkins/jenkins:lts-jdk21` runs Debian Trixie (the current testing branch). Microsoft's
Azure CLI apt repository only publishes releases for named stable Debian codenames
(bookworm, bullseye). The `$VERSION_CODENAME` shell expansion inside the Dockerfile
resolved to `trixie`, which produced a 404 from Microsoft's package server and a build
failure. The fix: pin the apt source line to `bookworm` explicitly rather than letting
`$VERSION_CODENAME` resolve dynamically. This installs az CLI 2.87.0 cleanly from
the bookworm release, which is ABI-compatible with Trixie.

### 8. ARM64 image crash-loops with `exec format error`

The AKS node pool uses `Standard_B2pls_v2` (ARM64/Ampere). The Jenkins build agent runs
on `x86_64` Docker Desktop. A plain `docker build` produces an `amd64` image — which AKS
successfully schedules (image pulled fine via the AcrPull managed identity) but
immediately crash-loops every pod with `exec format error` when the kubelet tries to run
the `java` binary for the wrong architecture.

Fix: switch to `docker buildx build --platform linux/arm64 --load`, which cross-compiles
the runtime stage for ARM64 using QEMU emulation via Docker Desktop's built-in binfmt
support. This requires the `docker-buildx-plugin` package (not included in `docker-ce-cli`
alone) — added to the Jenkins image.

### 9. QEMU can't execute Alpine binaries during ARM64 cross-build (`apk upgrade`, `adduser`)

After switching to `--platform linux/arm64`, the Dockerfile's `RUN apk upgrade` and
`RUN adduser` steps in the runtime stage failed with `exec format error` during the
*build* — not at container start. Docker Desktop registers `linux/arm64` as a supported
buildx platform, but the QEMU emulation it provides via DooD cannot actually execute
Alpine/musl `sh` or `apk`. Glibc-based ARM64 images (Debian) hit the same failure.

Fix: add `--platform=$BUILDPLATFORM` to the build and extract stages so they run natively
on `amd64`, and eliminate all `RUN` steps from the runtime stage. Non-root is achieved
with `USER 65532` (a numeric UID, no `adduser` needed). The `apk upgrade` that previously
patched CVE-2026-2100 (`p11-kit`) cannot be run under QEMU, so it is suppressed in
`.trivyignore` with a documented rationale — the fix exists in Alpine's package repos but
isn't yet published in the base image, and patching it in-flight is blocked by the QEMU
limitation.

---

## Cost-conscious design

- **Everything in this phase runs locally.** Jenkins and SonarQube are Docker containers
  on a laptop, not Azure VMs — the entire build-test-scan loop is proven at zero Azure
  spend before any billable infrastructure is provisioned.
- **Manual-first methodology** (see [above](#methodology-manual-first-then-automate))
  catches configuration mistakes before they become a paid pipeline run, not after.
- **Infra is torn down between sessions** against the persistent Terraform state backend
  (`rg-northwind-tfstate` / `stnorthwindtf676746`), confirming plan resource counts before
  every apply and an empty resource group before ending each session — the same discipline
  used across this portfolio.

## On the horizon

Phase 7 (ACR + AKS Terraform, CD pipeline, end-to-end deploy) is complete. Remaining:

1. **AKS hardening** — HPA, NetworkPolicy, Ingress + TLS, replacing the current
   ClusterIP + port-forward exposure. Node pool currently 1 node (`Standard_B2pls_v2`)
   due to this subscription's 4-vCPU regional quota; a 2-node pool requires a quota
   increase.
2. **Key Vault** for the SonarQube token and any other secrets, with Jenkins granted a
   managed identity with least-privilege vault access — replacing the service-principal
   credential (`azure-sp`) the CD stages currently use.
3. **Monitoring/alerting** — Container Insights / Azure Monitor, with at least one real
   alert rule.

**Deliberately deferred** (documented, not built): VNet peering between the CI and AKS
networks, image signing/SBOM generation, multi-environment AKS, Front Door/WAF in front
of the Ingress.

---

## Running it locally

```bash
# App only
./mvnw spring-boot:run
# → http://localhost:8080

# Full CI stack (Jenkins + SonarQube)
docker compose -f tools/docker-compose.yml up -d --build
# Jenkins → http://localhost:8081
# SonarQube → http://localhost:9000
```

`JAVA_HOME` must point at a JDK 17+ install (the build enforces this via
`maven-enforcer-plugin`). No system Maven is required — everything goes through the
`./mvnw` wrapper.
