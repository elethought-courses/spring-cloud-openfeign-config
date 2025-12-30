#!/bin/bash

# Setup script for mitmproxy with Java truststore
# This script generates mitmproxy CA certificate and converts it to a Java truststore

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
CERTS_DIR="$PROJECT_DIR/certs"
MITMPROXY_DIR="$HOME/.mitmproxy"
TRUSTSTORE_FILE="$CERTS_DIR/mitmproxy-truststore.p12"
TRUSTSTORE_PASSWORD="changeit"

echo "=== mitmproxy Setup Script ==="

# Create certs directory
mkdir -p "$CERTS_DIR"

# Check if mitmproxy is installed
if ! command -v mitmproxy &> /dev/null; then
    echo "mitmproxy not found. Installing..."
    pip install mitmproxy
fi

# Generate mitmproxy CA certificate if it doesn't exist
if [ ! -f "$MITMPROXY_DIR/mitmproxy-ca-cert.pem" ]; then
    echo "Generating mitmproxy CA certificate..."
    # Start and immediately stop mitmproxy to generate certs
    timeout 2 mitmproxy --set confdir="$MITMPROXY_DIR" || true
fi

# Check if CA certificate exists
if [ ! -f "$MITMPROXY_DIR/mitmproxy-ca-cert.pem" ]; then
    echo "ERROR: mitmproxy CA certificate not found at $MITMPROXY_DIR/mitmproxy-ca-cert.pem"
    echo "Please run 'mitmproxy' once manually to generate the certificate."
    exit 1
fi

echo "Found mitmproxy CA certificate: $MITMPROXY_DIR/mitmproxy-ca-cert.pem"

# Convert PEM to PKCS12 truststore for Java
echo "Creating Java truststore..."
rm -f "$TRUSTSTORE_FILE"

keytool -importcert \
    -alias mitmproxy-ca \
    -file "$MITMPROXY_DIR/mitmproxy-ca-cert.pem" \
    -keystore "$TRUSTSTORE_FILE" \
    -storetype PKCS12 \
    -storepass "$TRUSTSTORE_PASSWORD" \
    -noprompt

echo ""
echo "=== Setup Complete ==="
echo ""
echo "Truststore created: $TRUSTSTORE_FILE"
echo "Truststore password: $TRUSTSTORE_PASSWORD"
echo ""
echo "To start mitmproxy with web interface:"
echo "  mitmweb --listen-port 8888"
echo ""
echo "To run the Spring Boot app with proxy:"
echo "  mvn spring-boot:run -Dspring-boot.run.profiles=proxy"
echo ""
echo "Then access the mitmproxy web interface at: http://localhost:8081"
