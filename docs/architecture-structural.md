# Structural Architecture — Northwind Mutual Car Quote Generator

```mermaid
graph TB
    subgraph DEV["Developer Laptop (Windows 11 / Docker Desktop / WSL2)"]
        direction TB

        subgraph CI_NET["Docker network: ci"]
            direction LR

            subgraph JENKINS["northwind-jenkins:local  •  :8081"]
                J_CLI["Docker CLI + buildx"]
                J_TRIVY["Trivy scanner"]
                J_AZ["az CLI + kubectl"]
                J_PLUG["Plugins: git · sonar · docker-workflow"]
            end

            subgraph SONAR["northwind-sonarqube:community  •  :9000"]
                SQ_ANAL["Analysis engine"]
                SQ_GATE["Quality gate"]
                SQ_WH["Webhook → Jenkins :8080"]
            end
        end

        subgraph APP_CTR["nwq (northwind-quote:local)  •  :8080"]
            SPRING["Spring Boot 4.1\n(Driver / Vehicle / Quote)"]
            H2["H2 in-memory DB"]
            ACT["Actuator\n/health · /prometheus\n/livez · /readyz"]
        end

        DOCKER_SOCK["/var/run/docker.sock\n(Docker Desktop engine)"]
        DOCKER_ENGINE["Docker Desktop engine\n(amd64 host, ARM64 QEMU via buildx)"]
    end

    subgraph GITHUB["GitHub — northwind-car-quote-cicd-simple"]
        REPO["main branch\nJenkinsfile · dockerfile · k8s/ · infra/"]
    end

    subgraph AZURE["Azure — canadacentral  •  rg-northwind-quote-dev"]
        direction TB

        subgraph VNET["vnet-northwind-quote  10.40.0.0/16"]
            SUBNET["snet-aks  10.40.1.0/24"]
        end

        ACR["Azure Container Registry\nacrnorthwindquote&lt;suffix&gt;\nBasic SKU · admin disabled"]

        subgraph AKS_CLUSTER["aks-northwind-quote-dev\nFree tier · Kubernetes 1.35"]
            subgraph NODE["Node: Standard_B2pls_v2 (ARM64, 2 vCPU)"]
                POD1["northwind-quote pod 1\nUID 65532 (non-root)"]
                POD2["northwind-quote pod 2\nUID 65532 (non-root)"]
            end
            SVC["Service: northwind-quote\nClusterIP  port 80→8080"]
            KI["Kubelet identity\n(AcrPull on ACR)"]
        end

        TFSTATE["rg-northwind-tfstate\nstnorthwindtf676746\n(persistent — never destroy)"]
    end

    %% DooD: Jenkins talks to host Docker engine via socket
    J_CLI -->|"DooD — docker buildx build\n--platform linux/arm64"| DOCKER_SOCK
    DOCKER_SOCK --- DOCKER_ENGINE

    %% SonarQube webhook back to Jenkins
    SQ_WH -->|"POST /sonarqube-webhook/"| JENKINS

    %% Jenkins checks out from GitHub
    JENKINS -->|"git checkout"| REPO

    %% Jenkins pushes image to ACR via az CLI
    J_AZ -->|"az acr login\ndocker push :BUILD_NUMBER"| ACR

    %% Jenkins deploys to AKS via kubectl
    J_AZ -->|"az aks get-credentials\nkubectl apply"| AKS_CLUSTER

    %% AKS pulls image from ACR via managed identity (no imagePullSecrets)
    KI -->|"AcrPull role assignment\n(Terraform azurerm_role_assignment)"| ACR
    ACR -->|"image pull"| NODE

    %% Pods sit inside subnet
    NODE --- SUBNET

    %% Smoke check port-forward
    J_AZ -->|"kubectl port-forward\ncurl /actuator/health/readiness"| SVC
    SVC --> POD1
    SVC --> POD2

    %% Terraform state
    AZURE -.->|"terraform state"| TFSTATE

    style DEV fill:#1e2a3a,color:#fff,stroke:#3a5070
    style CI_NET fill:#162032,color:#fff,stroke:#2a4060
    style JENKINS fill:#0d2137,color:#fff,stroke:#1a4060
    style SONAR fill:#0d2137,color:#fff,stroke:#1a4060
    style APP_CTR fill:#0d2137,color:#fff,stroke:#1a4060
    style AZURE fill:#003360,color:#fff,stroke:#0060aa
    style VNET fill:#002550,color:#fff,stroke:#0050a0
    style AKS_CLUSTER fill:#002550,color:#fff,stroke:#0050a0
    style NODE fill:#001840,color:#fff,stroke:#004090
    style GITHUB fill:#24292e,color:#fff,stroke:#444
    style TFSTATE fill:#001030,color:#aaa,stroke:#334,stroke-dasharray:4 4
```

## Key design decisions shown above

| Decision | Why |
|---|---|
| DooD (socket mount) not DinD | No nested daemon; Jenkins uses the host Docker Desktop engine directly |
| `--platform linux/arm64` via buildx | AKS node pool is ARM64 (`Standard_B2pls_v2`); build agent is amd64 — QEMU cross-compilation bridges the gap |
| `USER 65532` not `adduser` | All `RUN` steps removed from the ARM64 runtime stage to avoid QEMU emulation failures during build |
| AcrPull via kubelet identity | `azurerm_role_assignment` in Terraform — no `imagePullSecrets`, no credential rotation |
| ClusterIP only | No public endpoint yet; smoke check uses `kubectl port-forward` — LoadBalancer/Ingress deferred to hardening phase |
| Persistent state backend separate RG | `rg-northwind-tfstate` never destroyed; `rg-northwind-quote-dev` torn down between sessions |
