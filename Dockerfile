FROM maven:3.8-openjdk-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/taichubackend-starter/target/*.jar app.jar
COPY --from=build /app/taichubackend-starter/src/main/resources/application.yml /app/application.yml

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"] 