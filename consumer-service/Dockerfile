FROM maven:3.9-eclipse-temurin-17
WORKDIR /app

# Cache Maven dependencies first
COPY pom.xml .
RUN mvn dependency:resolve -B --fail-never

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests -B

# Install curl for health check
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

EXPOSE 8001

ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "target/consumer-service-1.0.0.jar"]
