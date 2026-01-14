FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# baixa dependências com cache
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

# compila o projeto
COPY src ./src
RUN mvn -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar /app/app.jar

CMD ["sh", "-c", "java -jar /app/app.jar --server.port=${PORT:-8081}"]
