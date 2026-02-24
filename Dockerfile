FROM eclipse-temurin:25-jre
WORKDIR /app
RUN mkdir -p /app/logs
COPY build/libs/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
