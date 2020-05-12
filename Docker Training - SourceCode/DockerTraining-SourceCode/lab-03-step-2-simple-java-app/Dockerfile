FROM openjdk:8-jre-alpine
COPY ./target/*.jar /tmp
WORKDIR /tmp
ENTRYPOINT ["java","-jar","HelloApp.jar"]