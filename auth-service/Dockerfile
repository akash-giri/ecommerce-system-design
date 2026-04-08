FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY config-server/pom.xml config-server/pom.xml
COPY discovery-server/pom.xml discovery-server/pom.xml
COPY inventory-service/pom.xml inventory-service/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY auth-service/src auth-service/src
RUN mvn -pl auth-service -am clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/auth-service/target/auth-service-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
