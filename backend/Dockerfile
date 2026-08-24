# Build stage: compile with the project's own Maven wrapper so the build
# uses exactly the Maven version pinned in .mvn/wrapper, same as local dev.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline
COPY src/ src/
RUN ./mvnw -B -q -DskipTests package && \
    mv target/backend-*.jar target/app.jar && \
    rm -f target/original-*.jar

# Runtime stage: JRE only, no build toolchain in the final image.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
