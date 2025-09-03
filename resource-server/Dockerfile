# ===================================================================
# Stage 1: Dependency resolution
# ===================================================================
FROM maven:3.9.11-eclipse-temurin-21 AS dependency_builder

WORKDIR /app

COPY pom.xml .
COPY lab-aspects/pom.xml ./lab-aspects/
COPY resource-server/pom.xml ./resource-server/
COPY web-client/pom.xml ./web-client/

RUN mvn dependency:go-offline

# ===================================================================
# Stage 2: Build
# ===================================================================
FROM dependency_builder AS app_builder

# Copy all source code.
COPY . .

# Build only the resource-server and its required dependencies.
RUN mvn clean install -pl resource-server -am -DskipTests

# ===================================================================
# Stage 3: Final image
# ===================================================================
FROM eclipse-temurin:21-jre-jammy

# Create a non-root user for security, replicating the resource-server's approach.
RUN adduser --system --group spring-user
USER spring-user

WORKDIR /app
COPY --from=app_builder /app/resource-server/target/resource-server-*.jar resource-server.jar

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "resource-server.jar"]