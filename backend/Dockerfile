FROM openjdk:17
COPY ./build/libs/2025-0.0.1-SNAPSHOT.jar 2025.jar
EXPOSE 8080
ENTRYPOINT ["java","-Xms256m","-Xmx512m","-jar","./2025.jar"]