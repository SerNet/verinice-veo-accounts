FROM gcr.io/distroless/java25-debian13:nonroot@sha256:ca60da1345c0f17b6d019049e6749e15f10fd3c0da86dec938d2b4ec565d0629

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
