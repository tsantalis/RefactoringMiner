# ---------- Stage 1: Build ----------
FROM gradle:8.5-jdk17 AS builder

WORKDIR /app

# Clone and build the project
RUN git clone https://github.com/maorethians/RefactoringMiner.git . 
RUN chmod +x ./gradlew
RUN ./gradlew bootJar

# ---------- Stage 2: Runtime ----------
FROM bellsoft/liberica-openjdk-alpine:17

WORKDIR /app

# Copy the built JAR from builder
COPY --from=builder /app/build/libs/*.jar /app/app.jar

# Copy and set up entrypoint script
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

EXPOSE 8080

ENTRYPOINT ["/entrypoint.sh"]
