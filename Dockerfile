# syntax=docker/dockerfile:1

##########################################################################
# Build stage — compiles the Spring Boot (Java 25) app into a boot jar.
##########################################################################
FROM eclipse-temurin:25-jdk-noble AS build
WORKDIR /workspace

# Copy only what's needed to resolve dependencies first. This layer stays cached
# and is reused on subsequent builds unless the Gradle build scripts change,
# so day-to-day source edits don't re-download the whole dependency graph.
COPY gradlew ./
COPY gradle ./gradle
COPY settings.gradle build.gradle ./
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

# Now bring in the source and build the executable jar. Tests are intentionally
# skipped here: they need a MongoDB replica set and belong in CI, not the image
# build (which must stay hermetic and networkless).
COPY src ./src
RUN ./gradlew --no-daemon clean bootJar -x test

##########################################################################
# Runtime stage — small JRE image that runs the jar as a non-root user.
##########################################################################
FROM eclipse-temurin:25-jre-noble AS runtime
WORKDIR /app

# Run unprivileged.
RUN groupadd --system app && useradd --system --gid app --home /app app

# rootProject.name=getkempt -> build/libs/getkempt-<version>.jar (single fat jar).
COPY --from=build /workspace/build/libs/*.jar app.jar
RUN chown -R app:app /app
USER app

# Container-aware JVM: cap the heap to a share of the task's memory limit so a
# small Fargate/Lightsail task can't OOM. Tune JAVA_OPTS per task size at deploy
# time. SERVER_PORT is read by application.yml (defaults to 4000).
ENV SERVER_PORT=4000 \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

EXPOSE 4000

# `exec` makes java PID 1 so it receives SIGTERM directly and shuts down
# gracefully during a rolling deploy / task drain.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
