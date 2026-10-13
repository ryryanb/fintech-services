FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY customer-service/pom.xml customer-service/pom.xml

RUN mvn -f customer-service/pom.xml dependency:go-offline -B

COPY customer-service/src customer-service/src

RUN mvn -f customer-service/pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY --from=build /app/customer-service/target/*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]