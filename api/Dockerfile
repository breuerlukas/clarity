FROM eclipse-temurin:25-jdk AS build

WORKDIR /build

COPY gradlew settings.gradle.kts build.gradle.kts lombok.config ./
COPY gradle/ gradle/
RUN ./gradlew --no-daemon dependencies > /dev/null

COPY src/ src/
RUN ./gradlew --no-daemon bootJar

FROM eclipse-temurin:25-jre

# Tesseract reads scanned invoices on this machine; distribution packages
# follow the base image, so their versions are not pinned.
# hadolint ignore=DL3008
RUN apt-get update && \
    apt-get install -y --no-install-recommends tesseract-ocr tesseract-ocr-deu tesseract-ocr-eng && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=build /build/build/libs/api.jar api.jar
COPY docker-entrypoint.sh docker-entrypoint.sh

RUN chmod +x docker-entrypoint.sh && \
    useradd --uid 1000 --no-create-home --shell /usr/sbin/nologin clarity || true && \
    mkdir -p /app/data && \
    chown -R 1000:1000 /app

USER 1000

EXPOSE 8080

ENTRYPOINT ["/app/docker-entrypoint.sh"]