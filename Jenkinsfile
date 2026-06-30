// ============================================================================
// Northwind Mutual Car Quote Generator — CI/CD pipeline.
//
// Stages: Checkout -> Build & Test -> SonarQube analysis -> Quality Gate
//         -> Docker build -> Trivy scan -> ACR push -> AKS deploy -> Smoke check.
//
// The ACR/AKS stages are written ahead of that infrastructure actually
// existing (Phase 7, not yet provisioned) — they're guarded so the pipeline
// still runs end-to-end (and stops cleanly) against the local-only stack.
// Once ACR/AKS exist, set DEPLOY_ENABLED=true via a Jenkins credential/param
// to exercise them for real.
// ============================================================================
pipeline {
    agent any

    options {
        // Keep the workspace tidy and cap total runtime so a hung stage can't
        // run forever.
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds()
    }

    parameters {
        // Off by default: lets the CI half (build through Trivy scan) keep
        // running standalone against the free local stack. Flip to true once
        // ACR_NAME/AKS_CLUSTER/AKS_RESOURCE_GROUP point at real infra.
        booleanParam(name: 'DEPLOY_ENABLED', defaultValue: false,
            description: 'Push to ACR and deploy to AKS (requires Phase 7 infra to exist)')
    }

    environment {
        IMAGE_NAME          = 'northwind-quote'
        IMAGE_TAG           = "${env.BUILD_NUMBER}"
        ACR_NAME            = 'acrnorthwindquote1e24cb'
        AKS_CLUSTER_NAME    = 'aks-northwind-quote-dev'
        AKS_RESOURCE_GROUP  = 'rg-northwind-quote-dev'
        K8S_NAMESPACE       = 'default'
        AZURE_TENANT_ID     = '892a000d-d8af-4fd6-9936-7b358542b184'
    }

    stages {

        stage('Checkout') {
            steps {
                // Jenkins checks the repo out automatically for a "Pipeline
                // script from SCM" job; this makes the commit explicit in logs.
                checkout scm
            }
        }

        stage('Build & Test') {
            steps {
                // clean verify compiles, runs the 10 unit tests, and packages
                // the JAR. Uses the Maven wrapper so no Maven tool is needed.
                sh 'chmod +x mvnw && ./mvnw -B clean verify'
            }
            post {
                always {
                    // Publish the JUnit results so test trends show in Jenkins.
                    junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                // withSonarQubeEnv injects SONAR_HOST_URL + token from the
                // 'SonarQube' server config. The Maven sonar goal reuses the
                // already-compiled classes from the previous stage.
                withSonarQubeEnv('SonarQube') {
                    sh './mvnw -B sonar:sonar -Dsonar.projectKey=northwind-quote-generator'
                }
            }
        }

        stage('Quality Gate') {
            steps {
                // Waits for SonarQube's webhook callback, then fails the build
                // if the gate is not green. Requires the SonarQube->Jenkins
                // webhook (http://jenkins:8080/sonarqube-webhook/).
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Docker Build') {
            steps {
                // Builds the app image via the mounted host Docker socket (DooD).
                // --platform linux/arm64 is required: the Jenkins host build agent is
                // amd64, but every VM size this project uses (including the AKS node
                // pool) is ARM64 (Standard_B2pls_v2). Without this flag, docker build
                // produces an amd64 image that AKS schedules and then crash-loops with
                // "exec format error" -- a real bug hit and fixed during this project.
                // --load pulls the cross-built image back into the local Docker engine
                // so the Trivy Scan and Push to ACR stages can find it by tag.
                sh "docker buildx build --platform linux/arm64 --load -t ${IMAGE_NAME}:${IMAGE_TAG} -t ${IMAGE_NAME}:latest ."
            }
        }

        stage('Trivy Scan') {
            steps {
                // Fail the build on CRITICAL or HIGH vulnerabilities. --no-progress
                // keeps logs clean; the first run downloads the vuln DB.
                sh """
                    trivy image \
                      --severity CRITICAL,HIGH \
                      --exit-code 1 \
                      --no-progress \
                      --ignorefile .trivyignore \
                      ${IMAGE_NAME}:${IMAGE_TAG}
                """
            }
        }

        stage('Push to ACR') {
            when { expression { params.DEPLOY_ENABLED } }
            steps {
                // 'azure-sp' is a Jenkins "Username with password" credential:
                // username = service principal appId, password = its secret.
                // The SP needs AcrPush on the registry — nothing broader.
                withCredentials([usernamePassword(credentialsId: 'azure-sp',
                        usernameVariable: 'AZ_SP_ID', passwordVariable: 'AZ_SP_SECRET')]) {
                    sh """
                        az login --service-principal -u "\$AZ_SP_ID" -p "\$AZ_SP_SECRET" --tenant "\$AZURE_TENANT_ID"
                        az acr login --name ${ACR_NAME}
                        docker tag ${IMAGE_NAME}:${IMAGE_TAG} ${ACR_NAME}.azurecr.io/${IMAGE_NAME}:${IMAGE_TAG}
                        docker push ${ACR_NAME}.azurecr.io/${IMAGE_NAME}:${IMAGE_TAG}
                    """
                }
            }
        }

        stage('Deploy to AKS') {
            when { expression { params.DEPLOY_ENABLED } }
            steps {
                // Same service principal as the ACR push; it also needs
                // Azure Kubernetes Service Cluster User role on the cluster.
                // No imagePullSecrets in the manifests — AKS pulls via its
                // own kubelet identity, granted AcrPull through
                // `az aks update --attach-acr` at provisioning time.
                withCredentials([usernamePassword(credentialsId: 'azure-sp',
                        usernameVariable: 'AZ_SP_ID', passwordVariable: 'AZ_SP_SECRET')]) {
                    sh """
                        az login --service-principal -u "\$AZ_SP_ID" -p "\$AZ_SP_SECRET" --tenant "\$AZURE_TENANT_ID"
                        az aks get-credentials \
                          --name ${AKS_CLUSTER_NAME} \
                          --resource-group ${AKS_RESOURCE_GROUP} \
                          --overwrite-existing
                        kubectl apply -f k8s/service.yaml -n ${K8S_NAMESPACE}
                        sed "s#IMAGE_PLACEHOLDER#${ACR_NAME}.azurecr.io/${IMAGE_NAME}:${IMAGE_TAG}#" k8s/deployment.yaml \
                          | kubectl apply -n ${K8S_NAMESPACE} -f -
                        kubectl rollout status deployment/northwind-quote -n ${K8S_NAMESPACE} --timeout=180s
                    """
                }
            }
        }

        stage('Smoke Check') {
            when { expression { params.DEPLOY_ENABLED } }
            steps {
                // Confirms the new pods are actually answering, not just
                // that the rollout reported success. Port-forward only —
                // there's no public endpoint yet (ClusterIP, see k8s/service.yaml).
                sh """
                    kubectl port-forward svc/northwind-quote 18080:80 -n ${K8S_NAMESPACE} &
                    PF_PID=\$!
                    sleep 5
                    curl -sf http://localhost:18080/actuator/health/readiness
                    kill \$PF_PID
                """
            }
        }
    }

    post {
        success {
            echo "CI passed: ${IMAGE_NAME}:${IMAGE_TAG} built, scanned, and clean."
        }
        failure {
            echo "CI failed — check the stage logs above."
        }
    }
}