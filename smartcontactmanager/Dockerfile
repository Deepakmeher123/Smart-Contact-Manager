 # Step 1: Build stage (Optimized for caching)
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /smartcontactmanager
COPY pom.xml .
RUN mvn dependency:go-offline
COPY . .
RUN mvn clean package -DskipTests

# Step 2: Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app
# Wildcard prevents failure if the version in pom.xml changes
COPY --from=build /smartcontactmanager/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
