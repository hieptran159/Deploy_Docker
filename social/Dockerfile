FROM eclipse-temurin:17-jdk-focal as build
WORKDIR /workspace/app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src

RUN chmod +x mvnw
RUN ./mvnw -q -DskipTests install

FROM eclipse-temurin:17-jdk-focal
WORKDIR /app
VOLUME /tmp
COPY --from=build /workspace/app/target/*.jar app.jar
# ./uploads/images (app.file.upload-dir) nằm ngay trong /app -> mount volume vào /app/uploads
EXPOSE 8081 8082
ENTRYPOINT ["java","-jar","/app/app.jar"]
