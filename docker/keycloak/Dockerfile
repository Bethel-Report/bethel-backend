FROM maven:3.9.9-eclipse-temurin-21 AS spi-build
WORKDIR /build
COPY keycloak-spi/pom.xml pom.xml
COPY keycloak-spi/src ./src
RUN mvn -B -DskipTests package

FROM quay.io/keycloak/keycloak:26.3.3

COPY --from=spi-build /build/target/keycloak-email-rabbit-spi-1.0.0.jar \
    /opt/keycloak/providers/

RUN /opt/keycloak/bin/kc.sh build \
    --spi-email-sender--provider=rabbit

COPY docker/keycloak/bootstrap.sh /opt/keycloak/bootstrap.sh
