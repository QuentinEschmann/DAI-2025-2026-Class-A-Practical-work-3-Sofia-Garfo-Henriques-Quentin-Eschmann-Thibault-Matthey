FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn .mvn
COPY src src
RUN chmod +x mvnw
RUN ./mvnw clean package
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "target/project3-1.0-SNAPSHOT.jar"]