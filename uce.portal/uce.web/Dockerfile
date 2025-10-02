# Use an appropriate base image with Java (e.g., OpenJDK)
FROM maven:3.9.6-eclipse-temurin-21 AS builder

# Set the working directory inside the container
WORKDIR /app

# Copy the entire uce.portal directory
COPY ./uce.portal ./uce.portal

# Override the default config
COPY ./uce.portal/uce.common/src/main/resources/common-release.conf /app/uce.portal/uce.common/src/main/resources/common.conf

# Set the working directory to uce.portal
WORKDIR /app/uce.portal

# We are also building the target jar here; we only need uce.web
RUN mvn clean install -DskipTests -pl uce.web -am

FROM openjdk:21-jdk-slim

# Expose the port that your Spark application runs on
EXPOSE 4567

COPY --from=builder /app/uce.portal/uce.web/target/webportal-jar-with-dependencies.jar /app/uce.portal/uce.web/target/webportal-jar-with-dependencies.jar

# Copy the resources that aren't bundled in the jar
COPY ./uce.portal/resources /app/uce.portal/resources

WORKDIR /app/uce.portal/uce.web/

# Command to run the application
# The name webportal.jar is finalized in the pom.xml of the uce.web project.
# Maybe: "-jar-with-dependencies" needs to be added behind webportal
#CMD ["java", "-jar", "./target/webportal-jar-with-dependencies.jar"]
