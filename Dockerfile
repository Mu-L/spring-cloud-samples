# ============================================================
# Spring Cloud Samples - Multi-module Dockerfile
# Usage: docker build --build-arg MODULE=<module-name> -t <tag> .
# Example: docker build --build-arg MODULE=cloud-provider-sample -t provider .
# ============================================================
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="javahongxi"

ARG MODULE

RUN mkdir -p /app

# Copy target directory and extract the Spring Boot fat JAR
# Note: BuildKit cannot resolve wildcards with ARG vars in COPY source,
#       so we copy the whole target dir and pick the JAR via shell.
# For nested modules (e.g. cloud-seata-sample/xxx), JAR name is the basename.
COPY ${MODULE}/target/ /tmp/build/
RUN JAR_NAME=$(basename ${MODULE}) && \
    if [ -f /tmp/build/${JAR_NAME}.jar ]; then \
      cp /tmp/build/${JAR_NAME}.jar /app/app.jar; \
    elif ls /tmp/build/${JAR_NAME}-*.jar 1>/dev/null 2>&1; then \
      cp /tmp/build/${JAR_NAME}-*.jar /app/app.jar; \
    else \
      echo "ERROR: No JAR found"; ls -la /tmp/build/; exit 1; \
    fi \
    && rm -rf /tmp/build

WORKDIR /app

# Default JVM options - override via JAVA_OPTS env var in docker-compose
ENV JAVA_OPTS="-Xmx256m -Xms128m -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app/app.jar"]
