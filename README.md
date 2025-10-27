# cdmsa (Spring Boot)

## Requiermnets

- Java 17 or above
- Docker (para MariaDB en dev)

## Quick Start (Development)

### Steps

1. Launch MariaDB with Docker:

    ```bash
    docker compose up -d
    ```

2. Run the Spring Boot application with the development profile:

    ```bash
    ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
    ```

3. Test the application in your browser:
    **<http://localhost:8080/hello>**
