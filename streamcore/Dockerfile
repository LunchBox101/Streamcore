# ---- Build stage ----
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn clean package -DskipTests -q

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Add a non-root user for security
RUN addgroup -S streamcore && adduser -S streamcore -G streamcore
USER streamcore

COPY --from=build /app/target/streamcore-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
