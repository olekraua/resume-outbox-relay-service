# resume-outbox-relay-service

Single-service repository for `microservices/backend/services/outbox-relay-service`.

## Build
`./mvnw -pl microservices/backend/services/outbox-relay-service -am -DskipTests package`

## Run
`./mvnw -pl microservices/backend/services/outbox-relay-service -am spring-boot:run`

## Shared libraries
This service depends on artifacts from `resume-platform-libs`.

Install/update shared libraries before building service repos:
`cd ../resume-platform-libs && ./mvnw -DskipTests install`
