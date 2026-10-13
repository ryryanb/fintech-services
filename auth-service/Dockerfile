FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY auth-service/pom.xml auth-service/pom.xml

RUN mvn -f auth-service/pom.xml dependency:go-offline -B

COPY auth-service/src auth-service/src

RUN mvn -f auth-service/pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY --from=build /app/auth-service/target/*.jar app.jar

EXPOSE 8086

ENTRYPOINT ["java", "-jar", "app.jar"]