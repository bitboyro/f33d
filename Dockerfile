FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY target/f33d.jar app.jar
RUN mkdir -p /data

COPY entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

ENV PORT=8080
ENV HTTPS_ENABLED=false
ENV KEYSTORE_PATH=/data/f33d-keystore.p12
ENV KEYSTORE_PASSWORD=f33d-secret
ENV F33D_AUTH_MODE=local
ENV TOKENS_FILE=
ENV CLIENTS=
ENV ADMIN_USER=
ENV ADMIN_PASSWORD=

EXPOSE 8080 8443

VOLUME /data

ENTRYPOINT ["/app/entrypoint.sh"]
CMD ["serve"]
