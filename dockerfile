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
# ============================================================================

# ---- Stage 1: build the JAR from source ----
FROM maven:3.9-eclipse-temurin-21 AS build
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
FROM eclipse-temurin:21-jre AS extract
WORKDIR /app
# The artifactId/version from pom.xml produce this exact JAR name.
COPY --from=build /app/target/quote-generator-1.0.0-SNAPSHOT.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher

# ---- Stage 3: minimal runtime ----
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Run as a non-root user — least privilege, and required by hardened k8s setups.
RUN addgroup -S northwind && adduser -S northwind -G northwind

# Copy the extracted layers from least- to most-frequently-changing so Docker
# layer caching is maximally effective.
COPY --from=extract /app/app/dependencies/ ./
COPY --from=extract /app/app/spring-boot-loader/ ./
COPY --from=extract /app/app/snapshot-dependencies/ ./
COPY --from=extract /app/app/application/ ./

USER northwind
EXPOSE 8080

ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# After a layered '--launcher' extraction there is no single runnable jar, so we
# invoke Spring Boot's launcher class directly. Note the '.launch.' package —
# relocated in Spring Boot 3.2+ (this project is on 4.1.0).
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]