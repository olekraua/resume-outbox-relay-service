# outbox-relay-service

Standalone repository for the outbox-relay-service microservice.

## Local build

```bash
./mvnw -pl microservices/backend/services/outbox-relay-service -am -Dmaven.test.skip=true package
```

## Local run

```bash
./mvnw -pl microservices/backend/services/outbox-relay-service -am spring-boot:run
```

## Included modules

- shared
- staticdata
- profile
- notification
- auth
- search
- microservices/backend/services/outbox-relay-service

