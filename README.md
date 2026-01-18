# cdmsa (Spring Boot)

## Requiermnets
- Docker and Docker compose

- A JWT token
```
head -c 32 /dev/urandom | base64
```

- A key from OpenRouter to use mistralai/mistral-7b-instruct:free

- To use Microsoft OAuth, you must declare an application on Microsoft Registry and get `client-id` and `client-secret`. On Microsoft Registry, don't forget to specify redirect-url to the specific Microsoft address. See example `src/main/ressources/application.yml`




## Getting started

1. Make a copy of `application.yml` and name it `application-prod.yml` in `src/main/ressources/`
2. Fill the information gathered earlier.
3. `docker compose up`

3. Test the application in your browser (default port is 8080):
    **<http://localhost:8080/hello>**
