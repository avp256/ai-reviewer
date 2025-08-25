# ===== Build stage =====
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

# ===== Run stage =====
FROM eclipse-temurin:21-jre
ENV APP_HOME=/opt/ai-reviewer
WORKDIR ${APP_HOME}
# Create logs directory (can be overridden by LOG_DIR env)
RUN mkdir -p /var/log/ai-reviewer
ENV LOG_DIR=/var/log/ai-reviewer
COPY --from=build /app/target/ai-reviewer-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/opt/ai-reviewer/app.jar"]
