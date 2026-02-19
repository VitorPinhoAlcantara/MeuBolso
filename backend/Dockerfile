FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY .mvn .mvn
COPY mvnw mvnw
COPY pom.xml pom.xml
COPY src src

RUN chmod +x mvnw && ./mvnw -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 4444

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
