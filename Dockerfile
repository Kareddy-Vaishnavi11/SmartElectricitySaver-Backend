FROM eclipse-temurin:24-jdk AS build

WORKDIR /app

COPY . .

RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:24-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 5050

ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-5050} -jar app.jar"]