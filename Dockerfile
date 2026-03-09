FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

RUN apk add --no-cache bash curl && \
    curl -fLo /usr/local/bin/sbt https://github.com/sbt/sbt/releases/download/v1.10.6/sbt-1.10.6.tgz && \
    cd /usr/local && tar xzf /usr/local/bin/sbt && rm /usr/local/bin/sbt && \
    ln -s /usr/local/sbt/bin/sbt /usr/local/bin/sbt

COPY build.sbt ./
COPY project/ project/
RUN sbt update

COPY src/ src/
RUN sbt assembly

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=build /app/target/scala-3.3.4/chat-pulse-assembly-0.1.0.jar app.jar

RUN chown -R appuser:appgroup /app
USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
    CMD wget -qO- http://localhost:8080/api/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
