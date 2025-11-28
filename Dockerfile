FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

COPY build/libs/*SNAPSHOT.jar app.jar

EXPOSE 8888

ENTRYPOINT ["java", "-jar", "app.jar"]
