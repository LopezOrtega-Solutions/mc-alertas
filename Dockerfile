# Multi-stage build: compila con Maven sobre JDK 17, ejecuta con JRE 17.
# (Constitución VII: imagen propia para el compose del entorno de desarrollo.)

# --- Stage 1: build ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q package -DskipTests

# --- Stage 2: runtime ---
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/target/ms-alertas-*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
