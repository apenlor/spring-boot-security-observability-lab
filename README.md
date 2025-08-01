# Spring Boot Security & Observability Lab

This repository is an advanced, hands-on lab demonstrating the architectural evolution of a modern Java application. We will build a system from the ground up, starting with a secure monolith and progressively refactoring it into a fully observable, distributed system using cloud-native best practices.

---

## Workshop Guide: The Evolutionary Phases

This lab is structured in distinct, self-contained phases. The `main` branch always represents the latest completed phase. To explore a previous phase's code and detailed documentation, use the links below.

| Phase                                    | Description & Key Concepts                                                                                                                                                        | Code & Docs (at tag)                                                                                                            | Key Pull Requests                                                                                                                                                                                                                              |
|:-----------------------------------------|:----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:--------------------------------------------------------------------------------------------------------------------------------|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **1. The Secure Monolith**               | A standalone service that issues and validates its own JWTs. Concepts: `AuthenticationManager`, custom `JwtAuthenticationFilter`, `jjwt` library, and a foundational CI pipeline. | [`v1.0-secure-monolith`](https://github.com/apenlor/spring-boot-security-observability-lab/blob/v1.0-secure-monolith/README.md) | [#2](https://github.com/apenlor/spring-boot-security-observability-lab/pull/2), [#3](https://github.com/apenlor/spring-boot-security-observability-lab/pull/3), [#4](https://github.com/apenlor/spring-boot-security-observability-lab/pull/4) |
| **2. Observing the Monolith**            | *Upcoming...*                                                                                                                                                                     | -                                                                                                                               | -                                                                                                                                                                                                                                              |
| **3. Evolving to Federated Identity**    | *Upcoming...*                                                                                                                                                                     | -                                                                                                                               | -                                                                                                                                                                                                                                              |
| **4. Tracing a Distributed System**      | *Upcoming...*                                                                                                                                                                     | -                                                                                                                               | -                                                                                                                                                                                                                                              |
| **5. Correlated Logs & Access Auditing** | *Upcoming...*                                                                                                                                                                     | -                                                                                                                               | -                                                                                                                                                                                                                                              |
| **6. Continuous Security Integration**   | *Upcoming...*                                                                                                                                                                     | -                                                                                                                               | -                                                                                                                                                                                                                                              |

---

## How to Follow This Lab

1.  **Start with the `main` branch** to see the latest state of the project.
2.  To go back in time, use the **"Code & Docs" link** for a specific phase. This will show you the `README.md` for that phase, which contains the specific instructions and examples for that version of the code.
3.  To understand the *"why"* behind the changes, review the **Key Pull Requests** for each phase.

---

## Running the Project

To run the application and see usage examples for the **current phase**, please refer to the detailed instructions in its tagged `README.md` file.

**[>> Go to instructions for the current phase: `v1.0-secure-monolith` <<](https://github.com/apenlor/spring-boot-security-observability-lab/blob/v1.0-secure-monolith/README.md#local-development--quick-start)**

As the lab progresses, this link will always be updated to point to the latest completed phase.