# ==========================================
# Build Stage
# ==========================================
FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /app

# Copy the maven wrapper and pom.xml
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Give execution permission to the wrapper
RUN chmod +x ./mvnw

# Download dependencies (this layer will be cached)
RUN ./mvnw dependency:go-offline -B

# Copy the source code
COPY src/ src/

# Build the application
RUN ./mvnw clean package -DskipTests

# ==========================================
# Runtime Stage
# ==========================================
FROM eclipse-temurin:23-jre-jammy

WORKDIR /app

# Add a non-root user for security
RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring

# Copy the built jar file from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port 8080
EXPOSE 8080

# Configure healthcheck using the actuator endpoint
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the jar file
ENTRYPOINT ["java", "-jar", "app.jar"]
