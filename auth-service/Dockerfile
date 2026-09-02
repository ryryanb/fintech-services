
# ============================================================
# Build stage
# ============================================================

FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /app

# Copy Maven descriptor first for dependency layer caching
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application
RUN mvn clean package -DskipTests


# ============================================================
# Runtime stage
# ============================================================

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8086

ENTRYPOINT ["java", "-jar", "app.jar"]

