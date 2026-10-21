# possy-daemon — builds inside the container (JDK 21, Maven 3.9)
#
# Build context: repository root, e.g.  docker build -f daemon/Dockerfile .

# ---------- build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src

COPY pom.xml .
COPY service/pom.xml service/
COPY epson-tm-t20-cups-docker/pom.xml epson-tm-t20-cups-docker/
COPY daemon/pom.xml daemon/

COPY daemon/ daemon/
# git-commit-id-plugin generates git.properties (used for the version shown in the UI)
COPY .git/ .git/

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp -pl daemon -DskipTests package \
 && cp daemon/target/daemon-*.jar /app.jar

# ---------- runtime ----------
FROM eclipse-temurin:21-jre

ENV LANG=C.UTF-8 \
    JAVA_OPTS=""

# PDFs are written here when possy.pdf-generator.redirect-to-file=true (no printer needed)
RUN mkdir -p /output
VOLUME /output

COPY --from=build /app.jar /app.jar

EXPOSE 8081

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Djava.io.tmpdir=/output -Djava.security.egd=file:/dev/./urandom -jar /app.jar"]
