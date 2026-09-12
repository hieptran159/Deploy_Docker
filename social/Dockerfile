FROM eclipse-temurin:17-jdk-jammy as build
WORKDIR /workspace/app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src

RUN chmod +x mvnw
RUN ./mvnw -q -DskipTests install

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
# curl: cho HEALTHCHECK trong docker-compose.yml gọi /actuator/health
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
VOLUME /tmp
COPY --from=build /workspace/app/target/*.jar app.jar
# ./uploads/images (app.file.upload-dir) nằm ngay trong /app -> mount volume vào /app/uploads
EXPOSE 8081 8082
ENTRYPOINT ["java","-jar","/app/app.jar"]
