#!/usr/bin/env sh
set -eu

JWT_PRIVATE_KEY_PATH="${JWT_PRIVATE_KEY_PATH:-auth-service/keys/jwt-private.pem}"
JWT_PUBLIC_KEY_PATH="${JWT_PUBLIC_KEY_PATH:-auth-service/keys/jwt-public.pem}"

if [ ! -r "$JWT_PRIVATE_KEY_PATH" ]; then
  echo "Cannot read FinTech JWT private key: $JWT_PRIVATE_KEY_PATH" >&2
  exit 1
fi

if ! command -v openssl >/dev/null 2>&1; then
  echo "OpenSSL is required to derive the JWT public key" >&2
  exit 1
fi

mkdir -p "$(dirname "$JWT_PUBLIC_KEY_PATH")"
umask 077
openssl pkey -in "$JWT_PRIVATE_KEY_PATH" -pubout -out "$JWT_PUBLIC_KEY_PATH"
chmod 0644 "$JWT_PUBLIC_KEY_PATH"
echo "Wrote matching RSA public key to $JWT_PUBLIC_KEY_PATH"
echo "The private key was not copied or modified."