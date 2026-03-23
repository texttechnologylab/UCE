# syntax=docker/dockerfile:1.7
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

COPY ./uce.portal/pom.xml ./uce.portal/pom.xml
COPY ./uce.portal/settings.xml ./uce.portal/settings.xml
COPY ./uce.portal/uce.common/pom.xml ./uce.portal/uce.common/pom.xml
COPY ./uce.portal/uce.corpus-importer/pom.xml ./uce.portal/uce.corpus-importer/pom.xml
COPY ./uce.portal/uce.web/pom.xml ./uce.portal/uce.web/pom.xml
COPY ./uce.portal/uce.search/pom.xml ./uce.portal/uce.search/pom.xml
COPY ./uce.portal/uce.analysis/pom.xml ./uce.portal/uce.analysis/pom.xml

WORKDIR /app/uce.portal
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -T 1C -DskipTests dependency:go-offline -pl uce.web -am

WORKDIR /app
COPY ./uce.portal ./uce.portal
COPY ./uce.portal/uce.common/src/main/resources/common-release.conf /app/uce.portal/uce.common/src/main/resources/common.conf

WORKDIR /app/uce.portal
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -T 1C -DskipTests package -pl uce.web -am

FROM eclipse-temurin:21-jdk

# Expose the port that your Spark application runs on
EXPOSE 4567

COPY --from=builder /app/uce.portal/uce.web/target/webportal-jar-with-dependencies.jar /app/uce.portal/uce.web/target/webportal-jar-with-dependencies.jar

# Copy the resources that aren't bundled in the jar
COPY ./uce.portal/resources /app/uce.portal/resources
# Copy static assets used by the web app
COPY ./uce.portal/uce.web/src/main/resources/public /app/uce.portal/uce.web/src/main/resources/public

WORKDIR /app/uce.portal/uce.web/

# Command to run the application
# The name webportal.jar is finalized in the pom.xml of the uce.web project.
# Maybe: "-jar-with-dependencies" needs to be added behind webportal
#CMD ["java", "-jar", "./target/webportal-jar-with-dependencies.jar"]
