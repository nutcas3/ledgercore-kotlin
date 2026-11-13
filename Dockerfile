FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --version --no-daemon

COPY src src
RUN ./gradlew bootWar --no-daemon -x test

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd -r -u 1001 ledgercore
COPY --from=build --chown=ledgercore /app/build/libs/ledgercore.war app.war
USER ledgercore

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.war"]
