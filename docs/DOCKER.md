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
- `pdfbox2-layout` 1.0.0 is vendored as module `pdfbox2-layout/` (MIT, Ralf Stuckert):
  jitpack.io can no longer build the original Gradle 2.3 project, so the daemon
  dependency could not be resolved anymore. The jitpack repository was removed.

## Building locally (without Docker)

Use **JDK 11** for Maven (in IntelliJ: *Settings → Build Tools → Maven → Runner → JRE*).
Kotlin 1.3.61 / kapt does not work on JDK 16 or newer.

```
./mvnw -U clean package -DskipTests
```

`-U` is needed once if your local repository still has the failed jitpack lookup cached.

## Using the CUPS server of the host

If the printers are already set up in the host's CUPS, skip the cups container:

```
docker compose -f docker-compose.yml -f docker-compose.host-cups.yml up --build -d service daemon
```

The daemon reaches the host via `host.docker.internal`. The host CUPS must accept that
host name: add `ServerAlias *` to `/etc/cups/cupsd.conf` and restart CUPS
(`sudo systemctl restart cups`). With plain Docker Engine (not Docker Desktop), CUPS must
also listen on the Docker bridge, e.g. `Port 631` instead of `Listen localhost:631`,
and allow that network (`Allow 172.16.0.0/12` in `<Location />`).

## Time zone

All containers use `TZ` from `.env` (default `Europe/Vienna`).
