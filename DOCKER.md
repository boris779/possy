# Running possy with Docker Compose

Everything is built inside containers with the original toolchain (JDK 11, Maven 3.8,
Node 12 via frontend-maven-plugin). The host only needs Docker with Compose v2 and BuildKit.

## Start

```
cp .env.example .env        # adjust passwords, Jira, printer names
docker compose up --build -d
docker compose logs -f
```

The first build takes a while (Maven + npm downloads); later builds reuse the Maven cache.

| Component      | URL                     | Login                                      |
|----------------|-------------------------|--------------------------------------------|
| possy-service  | http://localhost:8080   | `POSSY_ADMIN_USER` / `POSSY_ADMIN_PASSWORD` |
| possy-daemon   | http://localhost:8081   | –                                          |
| CUPS           | http://localhost:631    | `CUPS_ADMIN_USER` / `CUPS_ADMIN_PASSWORD`   |

## Without a printer

With `POSSY_REDIRECT_TO_FILE=true` (default) the daemon renders each print request
as PDF into `./output` instead of sending it to CUPS. Good for a first smoke test:
create a freeform note in the UI and look for `possy*.pdf` in `./output` after ~10 s.

## With an Epson TM-T20II

1. Open http://localhost:631 → *Administration* → *Add Printer*
2. *LPD/LPR Host or Printer*, e.g. `lpd://192.168.1.10:515/PASSTHRU`
3. Name the printer exactly like `CUPS_PRINTER_WHITE` (etc.), enable sharing
4. Driver *Epson TM-T20*, media size `80mm x 210mm`, cut option *feed 3mm*
5. Set `POSSY_REDIRECT_TO_FILE=false` in `.env`, then `docker compose up -d daemon`

Printer definitions are kept in the `cups-config` volume.

## What changed compared to the 2019 setup

- service/daemon Dockerfiles are multi-stage: no local JDK/Maven/Node needed
  (Kotlin 1.3 kapt does not run on JDK 16+, which is the usual local build failure).
- Runtime images: `eclipse-temurin:11-jre` instead of the removed `openjdk:11.0.4-jre-stretch`.
- CUPS image: Debian bookworm instead of the abandoned `monkeydri/cups-docker`;
  the Epson driver is pinned to a commit and built with `-fgnu89-inline`
  (current GCC otherwise fails with `undefined reference to initializeSettings`).
- Configuration through environment variables / `.env` instead of editing `application.yml`.
- pdfbox-layout removed: it was only available via jitpack.io (which can no longer
  build its Gradle 2.3 project), and possy only used its `RoundRect`. That is now
  `addRoundRect()` in the daemon, using the PDFBox core API only.

## Building locally (without Docker)

The modules are being upgraded one by one:

| Module  | Stack                                   | JDK for Maven |
|---------|-----------------------------------------|---------------|
| daemon  | Spring Boot 4.1, Kotlin 2.3, Java 25    | 25 or newer   |
| service | Spring Boot 2.2, Kotlin 1.3, Vaadin 14  | **11** (Kotlin 1.3 / kapt fail on JDK 16+) |

```
./mvnw -pl daemon clean verify                 # JDK 25+
./mvnw -pl service clean package -DskipTests   # JDK 11
```

## Using the CUPS server of the host

If the printers are already set up in the host's CUPS, skip the cups container:

```
docker compose -f docker-compose.yml -f docker-compose.host-cups.yml up --build -d service daemon
```

CUPS accepts requests from the loopback interface only with `Host: localhost`
(`ServerAlias` is ignored for those). Docker Desktop delivers `host.docker.internal`
via loopback, so the daemon runs in the network namespace of a small `socat`
forwarder (`cups-proxy`) and talks to `localhost:631`. No changes to the host CUPS
are needed; the printers do not have to be shared.

With plain Docker Engine (no Docker Desktop) enable the `extra_hosts` entry of
`cups-proxy` and let the host CUPS listen on the Docker bridge
(`Port 631`, `Allow 172.16.0.0/12` in `<Location />`).

## Blank paper above the notes

If notes come out at the end of a longer strip of paper, the printer driver uses
a default media that is longer than the note (e.g. `RP80x200`). Set
`CUPS_SEND_PAGE_SIZE=true` in `.env`: the daemon then sends the note size with each
job (`Custom.72x80mm`, `Custom.64x160mm`, ...). Check first that your driver handles it:
`lp -d <printer> -o media=Custom.72x80mm some.pdf`.

## Time zone

All containers use `TZ` from `.env` (default `Europe/Vienna`).
