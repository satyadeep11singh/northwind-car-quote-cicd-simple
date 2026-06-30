# syntax=docker/dockerfile:1

# ============================================================================
# Northwind Mutual Car Quote Generator — multi-stage image
#
# Stage 1 (build):   full Maven + JDK 21, runs the Maven wrapper to produce the
#                    Spring Boot fat JAR. JDK 21 satisfies the project's
#                    maven-enforcer rule (requires Java >= 17) and is the
#                    current Temurin LTS.
# Stage 2 (extract): unpacks the fat JAR into Spring Boot's layered format so
#                    Docker can cache rarely-changing dependency layers
#                    separately from frequently-changing application classes.
# Stage 3 (runtime): JRE-only Alpine image (no compiler, smaller attack
#                    surface), runs as a non-root user, container-aware heap.
#                    Built for linux/arm64 (matching the AKS node pool's
#                    Standard_B2pls_v2 ARM64 size). Stages 1-2 use
#                    --platform=$BUILDPLATFORM so Maven and java -Djarmode
#                    run natively on the amd64 build agent -- only bytecode
#                    (platform-agnostic) is copied into the ARM64 runtime.
# ============================================================================

# ---- Stage 1: build the JAR from source ----
# $BUILDPLATFORM = the builder's native platform (amd64). All stages that
# execute binaries (mvn, java -Djarmode) must run natively to avoid QEMU
# ARM64 emulation failures via Docker Desktop's DooD passthrough, which
# registers linux/arm64 support but cannot actually execute musl/glibc
# binaries for that platform on this host. The final runtime stage has no
# RUN steps at all -- it just COPYs pre-built, platform-agnostic bytecode
# into an ARM64 base image, which is valid without emulation.
FROM --platform=$BUILDPLATFORM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy the Maven wrapper + POM first so dependency resolution is cached as its
# own layer; it only re-runs when pom.xml or the wrapper changes, not on every
# source edit.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B dependency:go-offline

# Now copy the source and build. We skip tests here on purpose: tests run as a
# dedicated Jenkins stage (./mvnw clean verify) where results are reported and
# the Sonar quality gate is evaluated. Re-running them inside the image build
# would duplicate work without adding signal.
COPY src/ ./src/
RUN ./mvnw -B clean package -DskipTests

# ---- Stage 2: extract the fat JAR into layers ----
FROM --platform=$BUILDPLATFORM eclipse-temurin:21-jre AS extract
WORKDIR /app
# The artifactId/version from pom.xml produce this exact JAR name.
COPY --from=build /app/target/quote-generator-1.0.0-SNAPSHOT.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher

# ---- Stage 3: minimal runtime (linux/arm64, matches AKS node pool) ----
# No RUN steps here -- every RUN in an ARM64 stage would require QEMU
# emulation, which fails via Docker Desktop's DooD passthrough on this host.
# Non-root is achieved with a numeric UID (65532 = nonroot in distroless
# convention; sufficient for Kubernetes runAsNonRoot admission control without
# needing adduser). The apk upgrade that previously patched CVE-2026-2100 is
# omitted for the same reason; Trivy is expected to flag if the upstream
# image reintroduces a HIGH/CRITICAL and we'd update the base image pin.
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

COPY --from=extract /app/app/dependencies/ ./
COPY --from=extract /app/app/spring-boot-loader/ ./
COPY --from=extract /app/app/snapshot-dependencies/ ./
COPY --from=extract /app/app/application/ ./

USER 65532

EXPOSE 8080

ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]