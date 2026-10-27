FROM maven:3.9-eclipse-temurin-21 AS backend-builder

WORKDIR /build
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests


FROM eclipse-temurin:21-jre-jammy

WORKDIR /app
RUN groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app --home-dir /app --shell /usr/sbin/nologin app

COPY --from=backend-builder --chown=app:app \
    /build/target/daka-backend-1.0.0.jar /app/app.jar

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai"

USER app
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
