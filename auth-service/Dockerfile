FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn install -N -DskipTests

COPY auth-service/pom.xml auth-service/pom.xml
COPY auth-service/src auth-service/src

RUN mvn -f auth-service/pom.xml clean package -DskipTests


FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY --from=build /app/auth-service/target/*.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]