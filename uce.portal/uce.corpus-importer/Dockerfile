# Use an appropriate base image with Java (e.g., OpenJDK)
FROM eclipse-temurin:21-jdk

# Set the working directory inside the container
WORKDIR /app
RUN apt-get update && apt-get install -y maven

# Copy the entire uce.portal directory
COPY ./uce.portal ./uce.portal

# Override the default config with release settings (container-to-container hosts)
COPY ./uce.portal/uce.common/src/main/resources/common-release.conf /app/uce.portal/uce.common/src/main/resources/common.conf

# Set the working directory to uce.portal
WORKDIR /app/uce.portal
RUN mvn -T 1C -DskipTests -pl uce.corpus-importer -am clean package

# Expose the port that your Spark application runs on
# EXPOSE 4567

WORKDIR /app/uce.portal/uce.corpus-importer/

# Command to run the application
# The name webportal.jar is finalized in the pom.xml of the uce.corpus-importer project.
#CMD ["java", "-jar", "./target/importer-jar-with-dependencies.jar"]
