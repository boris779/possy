#!/bin/sh
set -e

# Create/update the CUPS admin user (member of lpadmin = @SYSTEM in cupsd.conf)
if ! id "$CUPS_USER_ADMIN" >/dev/null 2>&1; then
  useradd --no-create-home --groups lpadmin --shell /usr/sbin/nologin "$CUPS_USER_ADMIN"
fi
echo "$CUPS_USER_ADMIN:$CUPS_USER_PASSWORD" | chpasswd

# Named volumes on /etc/cups keep printers.conf; make sure our cupsd.conf is present
[ -f /etc/cups/cupsd.conf ] || { echo "missing /etc/cups/cupsd.conf" >&2; exit 1; }

exec /usr/sbin/cupsd -f
