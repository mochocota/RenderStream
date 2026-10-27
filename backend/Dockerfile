# Etapa 1: Compilación
FROM gradle:8.10-jdk17 AS build
WORKDIR /home/gradle/src
COPY . .
RUN gradle installDist --no-daemon

# Etapa 2: Imagen ligera de ejecución
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /home/gradle/src/build/install/moonstream-backend /app

# Variable PORT inyectada automáticamente por Render (e.g. 10000)
ENV PORT=8080
ENV HOST=0.0.0.0

EXPOSE 8080

ENTRYPOINT ["/app/bin/moonstream-backend"]
