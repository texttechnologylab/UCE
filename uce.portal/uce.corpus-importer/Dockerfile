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
RUN --mount=type=cache,id=uce-m2-repo,target=/root/.m2/repository,sharing=locked \
    mvn -B -T 1C -nsu -DskipTests -Dmaven.repo.local=/root/.m2/repository \
    dependency:go-offline -pl uce.corpus-importer -am

WORKDIR /app
COPY ./uce.portal ./uce.portal
COPY ./uce.portal/uce.common/src/main/resources/common-release.conf /app/uce.portal/uce.common/src/main/resources/common.conf

WORKDIR /app/uce.portal
RUN --mount=type=cache,id=uce-m2-repo,target=/root/.m2/repository,sharing=locked \
    mvn -B -T 1C -nsu -DskipTests -Dmaven.repo.local=/root/.m2/repository \
    package -pl uce.corpus-importer -am

FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY --from=builder /app/uce.portal/uce.corpus-importer/target/importer.jar /app/uce.portal/uce.corpus-importer/target/importer.jar

# Expose the port that your Spark application runs on
# EXPOSE 4567

WORKDIR /app/uce.portal/uce.corpus-importer/

# Command to run the application
# The name webportal.jar is finalized in the pom.xml of the uce.corpus-importer project.
#CMD ["java", "-jar", "./target/importer-jar-with-dependencies.jar"]
