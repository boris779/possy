# possy-daemon — builds inside the container (JDK 11, Maven 3.8)
#
# Build context: repository root, e.g.  docker build -f daemon/Dockerfile .

# ---------- build ----------
FROM maven:3.8.8-eclipse-temurin-11 AS build
WORKDIR /src

COPY pom.xml .
COPY service/pom.xml service/
COPY epson-tm-t20-cups-docker/pom.xml epson-tm-t20-cups-docker/
COPY pdfbox2-layout/pom.xml pdfbox2-layout/
COPY daemon/pom.xml daemon/

COPY pdfbox2-layout/ pdfbox2-layout/
COPY daemon/ daemon/

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp -pl daemon -am -DskipTests package \
 && cp daemon/target/daemon-*.jar /app.jar

# ---------- runtime ----------
FROM eclipse-temurin:11-jre

ENV LANG=C.UTF-8 \
    JAVA_OPTS=""

# PDFs are written here when possy.pdf-generator.redirect-to-file=true (no printer needed)
RUN mkdir -p /output
VOLUME /output

COPY --from=build /app.jar /app.jar

EXPOSE 8081

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Djava.io.tmpdir=/output -Djava.security.egd=file:/dev/./urandom -jar /app.jar"]
