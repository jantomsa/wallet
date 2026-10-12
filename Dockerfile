# ---- build stage: compile and package the app with the Maven wrapper (JDK 25) ----
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# dependencies first – this layer is cached until pom.xml changes
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src src
RUN ./mvnw -B package -DskipTests

# ---- runtime stage: only JRE + the jar ----
FROM eclipse-temurin:25-jre
WORKDIR /app

# curl for the container health check (docker compose)
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

# app runs as non-root – the user must exist before "USER wallet" below
RUN groupadd --system wallet && useradd --system --gid wallet wallet

COPY --from=build /app/target/wallet-*.jar app.jar

USER wallet
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
