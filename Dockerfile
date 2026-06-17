# ── build ────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Cache dependency layer – only re-downloaded when pom.xml changes
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Compile and package (tests skipped; run them in CI separately)
COPY src/ src/
RUN mvn package -DskipTests

# ── runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Non-root user for security
RUN addgroup -S facility && adduser -S facility -G facility
USER facility

COPY --from=builder /app/target/facility-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
