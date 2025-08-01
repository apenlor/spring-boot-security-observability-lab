# Spring Boot Security & Observability Lab

This repository is a hands-on lab designed to demonstrate the architectural evolution of a modern Java application. We
will build a system from the ground up, starting with a secure monolith and progressively refactoring it into a fully
observable, distributed system using cloud-native best practices.

---

## Lab Progress: Phase 1 - The Standalone Secure Monolith

The `main` branch currently represents the completed state of **Phase 1**.

### Objective

The goal of this phase is to establish a robust security baseline for a standalone REST API. The service is responsible
for both authenticating users and issuing its own JSON Web Tokens (JWTs) without any external identity providers.

### Key Concepts Demonstrated

* **Self-Contained JWT Authentication:** The service acts as its own Authorization Server.
* **Credential Validation:** Correctly delegating username/password validation to Spring Security's
  `AuthenticationManager`.
* **Token Generation:** Using the `io.jsonwebtoken` (`jjwt`) library to create signed, expiring JWTs.
* **Custom Token Validation:** Implementing a custom `OncePerRequestFilter` to intercept requests, validate the Bearer
  token, and populate Spring's `SecurityContext`.
* **Stateless API Design:** Ensuring the application is fully stateless, a prerequisite for modern scalable services.

### Architecture Overview

The architecture for Phase 1 is a single Spring Boot application. The security logic is composed of three main
components:

1. **[SecurityConfig](resource-server/src/main/java/com/apenlor/lab/resourceserver/config/SecurityConfig.java):** This
   is the core configuration class. It defines the `AuthenticationManager` for validating credentials, sets up the
   `UserDetailsService` (using an in-memory store for this phase), and configures the main `SecurityFilterChain`.
2. **[TokenService](resource-server/src/main/java/com/apenlor/lab/resourceserver/service/TokenService.java):** A
   dedicated service that encapsulates all logic for creating and parsing JWTs using the `jjwt` library and our secret
   key.
3. **[JwtAuthenticationFilter](resource-server/src/main/java/com/apenlor/lab/resourceserver/config/JwtAuthenticationFilter.java):** 
   A custom filter that is added to the security chain. On every request to a protected endpoint, this filter
   extracts the `Bearer` token, uses the `TokenService` to validate it, and establishes the user's identity for the
   duration of the request.

---

## Local Development & Quick Start

The only prerequisite is a Java 21 JDK.

1. **Build the application:**
   ```bash
   ./mvnw clean install
   ```
2. **Run the application:**
   ```bash
   ./mvnw -pl resource-server spring-boot:run
   ```
   The application will start on `http://localhost:8081`.

---

## API Usage Examples (Phase 1)

*(Requires a command-line JSON processor like `jq` to easily extract the token.)*

#### 1. Authenticate and Get a Token

Send the hardcoded credentials to the `/auth/login` endpoint to receive a JWT.

```bash
    TOKEN=$(curl -s -X POST http://localhost:8081/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"user", "password":"password"}' | jq -r .jwtToken)
    
    echo "Acquired Token: $TOKEN"
```

#### 2. Access the Public Endpoint

This endpoint requires no token and should always succeed.

```bash
  curl http://localhost:8081/api/public/info
```

#### 3. Access the Secure Endpoint (with Token)

Use the acquired token in the `Authorization` header to access the protected resource.

```bash
    curl http://localhost:8081/api/secure/data -H "Authorization: Bearer $TOKEN"
```

#### 4. Access the Secure Endpoint (without Token)

This request will fail with a `401 Unauthorized` error, as handled by our security configuration.

```bash
    curl -i http://localhost:8081/api/secure/data
```