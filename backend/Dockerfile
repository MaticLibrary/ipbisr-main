# Kontekst budowania = KATALOG GŁÓWNY repo (patrz docker-compose.yml),
# bo Maven kopiuje do JAR-a także ../bpmn/wykonywalne i ../dmn.

# Etap 1: budowanie JAR-a (nie trzeba mieć lokalnie Javy ani Mavena)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /repo
COPY bpmn/wykonywalne ./bpmn/wykonywalne
COPY dmn ./dmn
WORKDIR /repo/backend
COPY backend/pom.xml .
RUN mvn -q dependency:go-offline
COPY backend/src ./src
RUN mvn -q package -DskipTests

# Etap 2: lekki obraz z samym JRE
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /repo/backend/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
