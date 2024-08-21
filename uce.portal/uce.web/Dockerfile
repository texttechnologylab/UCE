# Use an appropriate base image with Java (e.g., OpenJDK)
FROM openjdk:21-jdk-slim

# Set the working directory inside the container
WORKDIR /app

RUN apt-get update && apt-get install -y maven

# Copy the entire biofid.portal directory
COPY ./biofid.portal ./biofid.portal

# Set the working directory to biofid.portal
WORKDIR /app/biofid.portal
RUN mvn clean install -DskipTests

# Set the working directory to biofid.portal/biofid.common and build it
#WORKDIR /app/biofid.portal/biofid.common
#RUN mvn clean install -DskipTests
# Set the working directory to biofid.portal/biofid.search and build it
#WORKDIR /app/biofid.portal/biofid.search
#RUN mvn clean install -DskipTests
# Set the working directory to biofid.portal/biofid.web
#WORKDIR /app/biofid.portal/biofid.web

# Build biofid.web
RUN mvn clean package -DskipTests

# Expose the port that your Spark application runs on
EXPOSE 4567

WORKDIR /app/biofid.portal/biofid.web/

# Command to run the application
# The name webportal.jar is finalized in the pom.xml of the biofid.web project.
CMD ["java", "-jar", "./target/webportal-jar-with-dependencies.jar"]
