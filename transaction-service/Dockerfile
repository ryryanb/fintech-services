# --------- Stage 1: Build the application ----------
FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /app

# Install the root Maven parent POM
COPY pom.xml .
RUN mvn install -N -DskipTests

# Copy transaction service POM
COPY transaction-service/pom.xml transaction-service/pom.xml

# Copy source code
COPY transaction-service/src transaction-service/src

# Build the application
RUN mvn -f transaction-service/pom.xml clean package -DskipTests


# --------- Stage 2: Run the application ----------
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY --from=build /app/transaction-service/target/*.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]