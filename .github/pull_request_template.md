### Lab Phase & Objective
<!--
State which phase of the lab this PR completes (e.g., "Phase 1: Secure Monolith").
Briefly describe the strategic objective being met.
-->


### Key Architectural Decisions & Trade-offs
<!--
This is the most important section.
- Justify the "why" behind the chosen approach.
- What alternatives were considered and why were they discarded?
- Example: "Chose a symmetric key for JWTs in Phase 1 for simplicity, acknowledging we will replace it with an asymmetric key via Keycloak in Phase 3."
-->


### Verification Strategy
<!--
Describe how the acceptance criteria for this phase have been met and tested.
-->

- [ ] **Unit/Integration Tests:** All new and existing tests pass via `./mvnw clean verify`. Key test classes:
    - `com.example.tests.ExampleTest.java`
- [ ] **Docker Compose:** The entire system is launchable with `docker-compose up`.
- [ ] **Manual Verification:** Provide `curl` commands or steps to manually verify the new functionality.
  ```bash
  # Example:
  curl http://localhost:8080/api/public/info
  ```
  
### Definition of Done Checklist
<!--
Confirm each of these has been met.
-->

- [ ] The PR title follows the Conventional Commits specification (e.g., feat: ..., fix: ...). 
- [ ] The code adheres to SOLID principles and project conventions. 
- [ ] The README.md has been updated to reflect the new state of the project. 
- [ ] All new logic is covered by automated tests. 
- [ ] Security implications of the changes have been considered.
- [ ] Observability implications (metrics, logs, traces) have been considered.