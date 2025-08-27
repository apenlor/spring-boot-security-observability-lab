# ===================================================================
# Stage 1: The Build Environment
# ===================================================================
FROM maven:3.9.11-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy pom files.
COPY pom.xml .
RUN mkdir -p web-client
COPY web-client/pom.xml web-client/

# Download dependencies for the specific module.
RUN mvn -f web-client/pom.xml dependency:go-offline

# Copy the source code for the module.
# Changes to the source code will only invalidate this layer and subsequent layers.
COPY web-client/src ./web-client/src

# Build the application module. We assume tests are passed before docker image build.
RUN mvn -f web-client/pom.xml clean package -DskipTests


# ===================================================================
# Stage 2: The Final Production Image
# ===================================================================
FROM eclipse-temurin:21-jre-jammy

# Create a non-root user for security, replicating the resource-server's approach.
RUN adduser --system --group spring-user
USER spring-user

WORKDIR /app

# Copy the final JAR from the builder stage's correct target directory.
COPY --from=builder /app/web-client/target/web-client-*.jar web-client.jar

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "web-client.jar"]