FROM ubuntu:latest
LABEL authors="baiks"

FROM maven:latest AS build

WORKDIR /app

COPY pom.xml .

COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8064

ENTRYPOINT ["java", "-jar", "app.jar"]