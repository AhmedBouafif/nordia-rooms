
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline
COPY src/ src/
RUN ./mvnw -q -B package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 app && mkdir /data && chown app /data
COPY --from=build /app/target/*.jar app.jar
USER app
ENV DB_PATH=/data/nordia.db \
    PORT=8090
VOLUME /data
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]