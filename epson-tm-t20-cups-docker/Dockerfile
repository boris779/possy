# CUPS server with the open source Epson TM-T20 driver (https://github.com/nemik/epson-tm-t20-cups)
#
# The former base image (monkeydri/cups-docker) is no longer maintained, so this image
# builds on plain Debian. The driver is compiled in a separate stage.

# ---------- build the rastertozj filter ----------
FROM debian:bookworm-slim AS driver

ARG DRIVER_COMMIT=7919a1dcbfa0547105409202042791c711fe2cb3

RUN apt-get update \
  && apt-get install -y --no-install-recommends ca-certificates git gcc make libc6-dev libcups2-dev libcupsimage2-dev \
  && rm -rf /var/lib/apt/lists/*

WORKDIR /build
RUN git clone https://github.com/nemik/epson-tm-t20-cups.git . \
  && git checkout ${DRIVER_COMMIT} \
  && rm -f rastertozj *.o \
  # -fgnu89-inline: the driver uses GNU89 'inline' semantics; current GCC (C11 default) fails to link it otherwise
  && make CFLAGS="-fPIC -O2 -fgnu89-inline -w"

# ---------- runtime ----------
FROM debian:bookworm-slim

RUN apt-get update \
  && apt-get install -y --no-install-recommends cups cups-client cups-bsd cups-filters libcupsimage2 \
  && rm -rf /var/lib/apt/lists/*

COPY --from=driver /build/rastertozj /usr/lib/cups/filter/rastertozj
COPY --from=driver /build/tm20.ppd /usr/share/cups/model/Epson/tm20.ppd
COPY cupsd.conf /etc/cups/cupsd.conf
COPY entrypoint.sh /entrypoint.sh

RUN chmod 755 /usr/lib/cups/filter/rastertozj /entrypoint.sh \
  && chmod 644 /usr/share/cups/model/Epson/tm20.ppd

ENV CUPS_USER_ADMIN=admin \
    CUPS_USER_PASSWORD=admin

EXPOSE 631

HEALTHCHECK --interval=10s --timeout=5s --retries=6 CMD lpstat -h localhost:631 -r || exit 1

ENTRYPOINT ["/entrypoint.sh"]
