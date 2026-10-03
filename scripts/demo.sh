#!/usr/bin/env bash
set -euo pipefail

BASE="${BASE_URL:-http://localhost:8080}"
AUTH="${AUTH_URL:-http://localhost:9000}"

esperar() {
  local url="$1"
  local nombre="$2"
  for _ in $(seq 1 60); do
    if curl -fsS "$url" >/dev/null 2>&1; then
      echo "$nombre listo"
      return 0
    fi
    sleep 3
  done
  echo "Tiempo agotado esperando $nombre ($url)" >&2
  return 1
}

esperar "$AUTH/actuator/health" "auth-service"
esperar "$BASE/actuator/health" "gateway"

TOKEN=$(curl -fsS -u bank-client:bank-secret \
  -d grant_type=client_credentials \
  -d scope="accounts.read transactions.read transactions.write events.read" \
  "$AUTH/oauth2/token" | python3 -c 'import json,sys; print(json.load(sys.stdin)["access_token"])')

echo
echo "Token obtenido."
echo
echo "--- cuentas ---"
curl -fsS -H "Authorization: Bearer $TOKEN" "$BASE/api/cuentas/resumen"
echo
echo "--- informe (transacciones + cuentas, con fallback si cuentas cae) ---"
curl -fsS -H "Authorization: Bearer $TOKEN" "$BASE/api/transacciones/informe"
echo
echo "--- nueva transacción (publica en Kafka) ---"
curl -fsS -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"fecha":"2024-12-01","monto":1500,"tipo":"credito"}' \
  "$BASE/api/transacciones"
echo
sleep 2
echo "--- eventos consumidos ---"
curl -fsS -H "Authorization: Bearer $TOKEN" "$BASE/api/eventos"
echo
