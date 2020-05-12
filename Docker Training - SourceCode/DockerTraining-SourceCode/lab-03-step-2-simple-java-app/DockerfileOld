FROM openjdk:8-jdk-alpine
COPY ./target/*.jar /tmp
WORKDIR /tmp
ENTRYPOINT ["java","-jar","HelloApp.jar"]