# ===================================================================
# Stage 1: The Build Environment
# ===================================================================
FROM maven:3.9.11-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy pom files
COPY pom.xml .
RUN mkdir -p resource-server
COPY resource-server/pom.xml resource-server/

# Download dependencies for the specific module.
RUN mvn -f resource-server/pom.xml dependency:go-offline

# Copy the source code for the module.
# Changes to the source code will only invalidate this layer and subsequent layers.
COPY resource-server/src ./resource-server/src

# Build the application module. We assume tests are passed before docker image build.
RUN mvn -f resource-server/pom.xml clean package -DskipTests


# ===================================================================
# Stage 2: The Final Production Image
# ===================================================================
FROM eclipse-temurin:21-jre-jammy

# Create a non-root user for security.
RUN adduser --system --group spring-user
USER spring-user

WORKDIR /app

# Copy the final JAR from the builder stage's correct target directory.
COPY --from=builder /app/resource-server/target/resource-server-*.jar resource-server.jar

EXPOSE 8081 9092
ENTRYPOINT ["java", "-jar", "resource-server.jar"]