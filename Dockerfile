# for gcloud
FROM bitnami/google-cloud-sdk as gcloud-build
#FROM gcr.io/google.com/cloudsdktool/google-cloud-cli:alpine as gcloud-build

# build application jar
FROM maven:alpine AS MAVEN_TOOL_CHAIN
WORKDIR /tmp
COPY pom.xml ./
COPY src ./src
RUN mvn package

# for java
FROM openjdk:11-jdk as jre-build
#FROM amazoncorretto:11-alpine as jre-build
#RUN apk add --no-cache binutils

# build small JRE image
RUN $JAVA_HOME/bin/jlink \
         --add-modules ALL-MODULE-PATH \
	 --strip-debug \
         --no-man-pages \
         --no-header-files \
         --compress=2 \
         --output /customjre

# for perfkit
FROM python:3.10.0-slim-buster as perfkit-build
RUN apt-get update && apt-get install --no-install-recommends --assume-yes git
RUN git clone https://github.com/prakhag2/PerfKitBenchmarker /tmp/pkb/PerfKitBenchmarker
RUN \rm -rf /tmp/pkb/PerfKitBenchmarker/.git* && \
    \rm -rf /tmp/pkb/PerfKitBenchmarker/docs && \
    \rm -rf /tmp/pkb/PerfKitBenchmarker/tutorials && \
    \rm -rf /tmp/pkb/PerfKitBenchmarker/tests && \
    \rm -rf /tmp/pkb/PerfKitBenchmarker/*.md
RUN python3 -m venv /venv
ENV PATH=/venv/bin:$PATH
RUN pip3 install --no-cache-dir -r /tmp/pkb/PerfKitBenchmarker/requirements.txt

# actual image
FROM python:3.10.0-slim-buster

# copy java custom JRE
ENV JAVA_HOME /customjre
COPY --from=jre-build /customjre/ $JAVA_HOME
ENV PATH $PATH:/customjre/bin

# copy application jar
COPY --from=MAVEN_TOOL_CHAIN /tmp/target/spring-boot-perfkit-1.0.0-SNAPSHOT.jar /tmp/
COPY src/main/resources/application.properties /tmp/
COPY benchmark.sh /tmp/

# copy perfkit & venv
RUN apt-get update && \
    apt-get install --no-install-recommends -y openssh-client && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* && \
    rm -rf /var/cache/*
COPY --from=perfkit-build /venv /venv
COPY --from=perfkit-build /tmp/pkb/PerfKitBenchmarker /tmp/pkb/PerfKitBenchmarker
ENV PATH=/venv/bin:$PATH

# copy gcloud sdk
COPY --from=gcloud-build /opt/bitnami/google-cloud-sdk/ /usr/local/gcloud/google-cloud-sdk
#COPY --from=gcloud-build /google-cloud-sdk/ /usr/local/gcloud/google-cloud-sdk
ENV PATH $PATH:/usr/local/gcloud/google-cloud-sdk/bin

WORKDIR /tmp
RUN chmod +x /tmp/benchmark.sh
EXPOSE 8080

CMD ["java","-jar","spring-boot-perfkit-1.0.0-SNAPSHOT.jar", "--spring.config.location=file:application.properties"]

