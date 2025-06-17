FROM maven:3.8-openjdk-11 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

FROM openjdk:11-jdk-slim
WORKDIR /app
COPY --from=build /app/taichubackend-starter/target/*.jar app.jar
COPY --from=build /app/taichubackend-starter/src/main/resources/application-prod.yml /app/application.yml

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"] 