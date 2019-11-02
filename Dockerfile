# Builds the Dockerfile for the backend API server.
FROM openjdk:14-slim

WORKDIR /

COPY target/uberjar/task-tracker-0.1.0-SNAPSHOT-standalone.jar task-tracker.jar
EXPOSE 5000

CMD java -jar task-tracker.jar
