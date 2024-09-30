FROM openjdk:24-slim-bullseye@sha256:d3d89f0ff751bd92d856f11a0b3223744fbe55ee95ba8990bcfe016223762079

WORKDIR /app
COPY ./choral-build/frontend-1.0-SNAPSHOT.jar /app/frontend.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/frontend.jar"]
