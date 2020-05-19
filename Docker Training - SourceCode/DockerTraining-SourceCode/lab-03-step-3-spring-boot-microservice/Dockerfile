FROM openjdk:8-jre-alpine

COPY ./target/*.jar /tmp
WORKDIR /tmp

# EXPOSE IS  FOR DOCUMENTATION PURPOSES !!!!

EXPOSE 8080 

ENTRYPOINT ["java","-jar","products-app-springboot.jar"]