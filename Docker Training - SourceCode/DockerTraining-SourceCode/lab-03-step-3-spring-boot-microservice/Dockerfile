FROM openjdk:8-jre-alpine

LABEL maintainer="me@someserver.be"

COPY ./target/*.jar /tmp

WORKDIR /tmp

ENTRYPOINT ["java","-jar","products-app-springboot.jar"]