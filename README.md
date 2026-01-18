## CDMSA Backend

### Environment Variables

To deploy the project, you need to make a copy of the sample environment file located at:

```
src/main/resources/application.yml
```

Place the copy in the same folder and rename it to:

```
application-prod.yml
```

You will need to obtain the following:

* **A JWT token**
  On Linux:

  ```bash
  head -c 32 /dev/urandom | base64
  ```

* **An OpenRouter API key**
  Required to use `mistralai/mistral-7b-instruct:free`.

* **Microsoft OAuth credentials**
  To use Microsoft OAuth, you must:

  * Register an application in the Microsoft Registry.
  * Retrieve the `client-id` and `client-secret`.
  * Configure the `redirect-url` to match the Microsoft OAuth callback URL used by the frontend.
  * Ensure this same `redirect-url` is also specified in `application-prod.yml`.

---

### Deploy

1. Clone the project.
2. Configure `application-prod.yml`.
3. Run:

   ```bash
   docker compose up -d
   ```

---

### Local Development

1. Copy your application configuration and rename it to:

   ```
   application-dev.yml
   ```

   Adjust the configuration if needed.

2. Edit the Docker Compose file in `dsd-backend` and update the `command` line to:

   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```
