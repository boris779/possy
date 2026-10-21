# possy-service — builds inside the container with the original toolchain
# (JDK 11, Maven 3.8; Node 12 is fetched by the frontend-maven-plugin),
# so nothing besides Docker is needed on the host.
#
# Build context: repository root, e.g.  docker build -f service/Dockerfile .

# ---------- build ----------
FROM maven:3.8.8-eclipse-temurin-11 AS build
WORKDIR /src

# reactor + module poms first (better layer caching)
COPY pom.xml .
COPY daemon/pom.xml daemon/
COPY epson-tm-t20-cups-docker/pom.xml epson-tm-t20-cups-docker/
COPY pdfbox2-layout/pom.xml pdfbox2-layout/
COPY service/pom.xml service/

COPY service/ service/
# git-commit-id-plugin generates git.properties (used for the version shown in the UI)
COPY .git/ .git/

# Kotlin 1.3 / kapt only works on JDK <= 15 -> JDK 11 here is intentional.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp -pl service -Pprod -DskipTests package \
 && cp service/target/service-*.jar /app.jar

# ---------- runtime ----------
FROM eclipse-temurin:11-jre

ENV LANG=C.UTF-8 \
    JAVA_OPTS=""

COPY --from=build /app.jar /app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -Dvaadin.productionMode=true -jar /app.jar"]
