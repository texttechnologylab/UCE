# Use an appropriate base image with Java (e.g., OpenJDK)
FROM openjdk:21-jdk-slim

# Set the working directory inside the container
WORKDIR /app

RUN apt-get update && apt-get install -y maven

# Copy the entire uce.portal directory
COPY ./uce.portal ./uce.portal

# Set the working directory to uce.portal
WORKDIR /app/uce.portal
RUN mvn clean install -DskipTests

# Build uce.web
RUN mvn clean package -DskipTests

# Expose the port that your Spark application runs on
EXPOSE 4567

WORKDIR /app/uce.portal/uce.web/

# Command to run the application
# The name webportal.jar is finalized in the pom.xml of the uce.web project.
# Maybe: "-jar-with-dependencies" needs to be added behind webportal
CMD ["java", "-jar", "./target/webportal.jar"]
