FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY settings.xml .
COPY src src
RUN mvn -s settings.xml -B -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 commerce && mkdir -p /app/data/uploads && chown -R commerce:commerce /app
COPY --from=build /workspace/target/commerce-api-*.jar app.jar
USER commerce
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
