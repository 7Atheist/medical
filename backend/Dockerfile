# =====================================================================
# 业务后端镜像：Java 17 + Spring Boot
# =====================================================================
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline || true
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/assessment-backend.jar app.jar
# 报告输出目录（挂载卷）
RUN mkdir -p /app/reports
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
