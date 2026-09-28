# ---------- Etapa 1: compilar ----------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

COPY .mvn .mvn
COPY mvnw pom.xml ./
# Si el repo se clono en Windows, mvnw puede perder el permiso de ejecucion
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

COPY src src
# Los tests ya se corren en el CI, aqui solo se empaqueta
RUN ./mvnw -B clean package -DskipTests

# ---------- Etapa 2: imagen final (solo JRE) ----------
FROM eclipse-temurin:25-jre
WORKDIR /app

# No correr como root
RUN useradd -r -u 1001 appuser
COPY --from=build /app/target/*.jar app.jar
USER appuser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]