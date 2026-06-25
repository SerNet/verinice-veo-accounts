FROM gcr.io/distroless/java25-debian13:nonroot@sha256:dade01b669efd3bea3977f73cc196c56f1ee678a71ec8305f84ec15fd5a23c8d

ARG VEO_ACCOUNTS_VERSION

LABEL org.opencontainers.image.title="vernice.veo accounts"
LABEL org.opencontainers.image.description="REST API for account management in veo"
LABEL org.opencontainers.image.ref.name=verinice.veo-accounts
LABEL org.opencontainers.image.vendor="SerNet GmbH"
LABEL org.opencontainers.image.authors=verinice@sernet.de
LABEL org.opencontainers.image.licenses=AGPL-3.0
LABEL org.opencontainers.image.source=https://github.com/verinice/verinice-veo-accounts

ENV JDK_JAVA_OPTIONS "-Djdk.serialFilter=maxbytes=0"

USER nonroot

COPY --chown=nonroot:nonroot build/libs/veo-accounts-${VEO_ACCOUNTS_VERSION}.jar /app/veo-accounts.jar

WORKDIR /app
EXPOSE 8099
CMD ["veo-accounts.jar"]
