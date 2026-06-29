// ============================================================================
// Northwind Mutual Car Quote Generator — CI pipeline (Phase 3).
//
// Stages: Checkout -> Build & Test -> SonarQube analysis -> Quality Gate
//         -> Docker build -> Trivy scan.
//
// ACR push and AKS deploy are added in Phases 5-6 once that infra exists; this
// Jenkinsfile deliberately stops at "image built and scanned" so the entire
// build-quality half is proven with zero Azure spend.
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

    environment {
        IMAGE_NAME = 'northwind-quote'
        IMAGE_TAG  = "${env.BUILD_NUMBER}"
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
                sh "docker build -t ${IMAGE_NAME}:${IMAGE_TAG} -t ${IMAGE_NAME}:latest ."
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
                      ${IMAGE_NAME}:${IMAGE_TAG}
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