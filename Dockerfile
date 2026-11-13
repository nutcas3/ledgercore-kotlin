FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --version --no-daemon

RUN --mount=type=cache,target=/root/.gradle ./gradlew dependencies --no-daemon

COPY src src
RUN --mount=type=cache,target=/root/.gradle ./gradlew bootWar --no-daemon -x test

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd -r -u 1001 ledgercore
COPY --from=build --chown=ledgercore /app/build/libs/ledgercore.war app.war
USER ledgercore

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.war"]
