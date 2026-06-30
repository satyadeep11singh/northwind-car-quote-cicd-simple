# Pipeline Architecture — Northwind Mutual Car Quote Generator

```mermaid
flowchart TD
    GH(["GitHub\nnorthwind-car-quote-cicd-simple\nmain branch"])

    subgraph LOCAL["Local stack (free — zero Azure spend)"]
        direction TB

        subgraph JENKINS_CTR["Jenkins container  :8081"]
            S1["① Checkout\ncheckout scm"]
            S2["② Build & Test\n./mvnw -B clean verify\n10 unit tests · JUnit published"]
            S3["③ SonarQube Analysis\n./mvnw sonar:sonar\nwithSonarQubeEnv injects token"]
            S4["④ Quality Gate\nwaitForQualityGate\nblocks on webhook callback"]
            S5["⑤ Docker Build\ndocker buildx build\n--platform linux/arm64 --load"]
            S6["⑥ Trivy Scan\n--severity CRITICAL,HIGH\n--exit-code 1 --ignorefile .trivyignore"]
        end

        subgraph SONAR_CTR["SonarQube container  :9000"]
            SA["Static analysis\nJava · XML sensors"]
            SG["Quality gate evaluation\n(no new bugs / vulns / smells)"]
            SW["Webhook callback\nPOST → jenkins:8080/sonarqube-webhook/"]
        end

        DE["Docker Desktop engine\n(ARM64 image via QEMU buildx)"]
    end

    subgraph AZURE["Azure — rg-northwind-quote-dev  (provisioned by Terraform)"]
        direction TB

        subgraph GATE["DEPLOY_ENABLED=true gates these stages"]
            S7["⑦ Push to ACR\naz login (service principal)\naz acr login · docker push :BUILD_NUMBER"]
            S8["⑧ Deploy to AKS\naz aks get-credentials\nkubectl apply service + deployment\nkubectl rollout status --timeout=180s"]
            S9["⑨ Smoke Check\nkubectl port-forward :18080\ncurl /actuator/health/readiness"]
        end

        ACR["Azure Container Registry\nBasic SKU\nacrnorthwindquote&lt;suffix&gt;.azurecr.io"]

        subgraph AKS["aks-northwind-quote-dev"]
            KI["Kubelet identity\nAcrPull on ACR\n(Terraform role assignment)"]
            DEP["Deployment: northwind-quote\n2 replicas · ARM64\nUID 65532 · requests/limits set"]
            SVC_K["Service: ClusterIP\nport 80 → 8080"]
            LR["Liveness: /actuator/health/liveness\nReadiness: /actuator/health/readiness"]
        end
    end

    subgraph RESULT["Build result"]
        PASS(["✅ SUCCESS\nImage built, scanned, deployed\nSmoke check UP"])
        FAIL(["❌ FAILURE\nPipeline aborts at failing stage\nNo image pushed if scan fails"])
    end

    %% Flow
    GH -->|"git checkout"| S1
    S1 --> S2
    S2 -->|"compiled classes reused"| S3
    S3 -->|"analysis submitted"| SA
    SA --> SG
    SG -->|"gate result via webhook"| SW
    SW -->|"POST callback"| S4
    S4 -->|"gate PASSED"| S5
    S5 -->|"linux/arm64 image"| DE
    DE -->|"--load into engine"| S6
    S6 -->|"zero findings"| S7
    S7 -->|"push :BUILD_NUMBER"| ACR
    S7 --> S8
    KI -->|"pulls image\n(no imagePullSecrets)"| ACR
    S8 -->|"kubectl apply"| DEP
    S8 -->|"kubectl apply"| SVC_K
    DEP --- LR
    S9 -->|"port-forward"| SVC_K
    SVC_K -->|"curl /readiness → UP"| S9
    S9 --> PASS
    S6 -->|"CRITICAL/HIGH found"| FAIL
    S4 -->|"gate FAILED"| FAIL

    %% Options bar
    OPT["Pipeline options\ntimestamps() · timeout 30m\ndisableConcurrentBuilds()"]
    OPT -.-> S1

    style LOCAL fill:#1e2a3a,color:#fff,stroke:#3a5070
    style JENKINS_CTR fill:#0d2137,color:#fff,stroke:#1a4060
    style SONAR_CTR fill:#0d2137,color:#fff,stroke:#1a4060
    style AZURE fill:#003360,color:#fff,stroke:#0060aa
    style GATE fill:#002040,color:#fff,stroke:#004080,stroke-dasharray:5 3
    style AKS fill:#002550,color:#fff,stroke:#0050a0
    style RESULT fill:#0a1a0a,color:#fff,stroke:#2a4a2a
    style PASS fill:#1a3a1a,color:#aaffaa,stroke:#2a6a2a
    style FAIL fill:#3a1a1a,color:#ffaaaa,stroke:#6a2a2a
    style GH fill:#24292e,color:#fff,stroke:#444
    style OPT fill:#111,color:#888,stroke:#333,stroke-dasharray:3 3
```

## Stage-by-stage reference

| # | Stage | Runs when | Key mechanism | Fails build if |
|---|---|---|---|---|
| ① | Checkout | Always | `checkout scm` from GitHub `main` | Repo unreachable |
| ② | Build & Test | Always | `./mvnw -B clean verify` · JUnit published via `post { always }` | Any unit test fails |
| ③ | SonarQube Analysis | Always | `withSonarQubeEnv` injects `SONAR_HOST_URL` + token; reuses compiled classes | Sonar server unreachable |
| ④ | Quality Gate | Always | `waitForQualityGate abortPipeline: true` · 5-min timeout · webhook-driven | Gate status not `OK` |
| ⑤ | Docker Build | Always | `docker buildx build --platform linux/arm64 --load` via DooD socket | Build error |
| ⑥ | Trivy Scan | Always | `--severity CRITICAL,HIGH --exit-code 1 --ignorefile .trivyignore` | Any unignored CRITICAL/HIGH CVE |
| ⑦ | Push to ACR | `DEPLOY_ENABLED=true` | SP login → `az acr login` → `docker push :<BUILD_NUMBER>` | Auth failure or push error |
| ⑧ | Deploy to AKS | `DEPLOY_ENABLED=true` | `kubectl apply` + `rollout status --timeout=180s` | Rollout doesn't complete in 3 min |
| ⑨ | Smoke Check | `DEPLOY_ENABLED=true` | `kubectl port-forward` → `curl /actuator/health/readiness` | Readiness endpoint not `UP` |

## Authentication model

```
Jenkins SP (azure-sp credential)
  ├── AcrPush on ACR          → stages ⑦ (image push)
  └── AKS Cluster User Role   → stage  ⑧ (get-credentials)

AKS kubelet identity (Terraform azurerm_role_assignment)
  └── AcrPull on ACR          → node pulls image (no imagePullSecrets in manifests)
```
