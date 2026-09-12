# Étape 1 : Build de l'application avec Maven
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copier les fichiers de dépendances
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# Donner les permissions d'exécution au script mvnw
RUN chmod +x mvnw

# Télécharger les dépendances
RUN ./mvnw dependency:go-offline -B

# Copier le code source et compiler
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Étape 2 : Image d'exécution légère avec OpenJDK 17
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copier le JAR compilé depuis l'étape de build
COPY --from=build /app/target/*.jar app.jar

# Exposer le port par défaut de Spring Boot
EXPOSE 8080

# Commande pour démarrer l'application
ENTRYPOINT ["java", "-jar", "app.jar"]