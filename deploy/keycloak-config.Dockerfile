FROM alpine:3.20

RUN apk add --no-cache curl jq

COPY deploy/keycloak-config.sh /scripts/keycloak-config.sh
RUN chmod +x /scripts/keycloak-config.sh

ENTRYPOINT ["/bin/sh", "-lc"]
CMD ["/scripts/keycloak-config.sh"]

