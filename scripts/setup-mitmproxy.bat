@echo off
setlocal enabledelayedexpansion

REM Setup script for mitmproxy with Java truststore (Windows)
REM This script generates mitmproxy CA certificate and converts it to a Java truststore

set SCRIPT_DIR=%~dp0
set PROJECT_DIR=%SCRIPT_DIR%..
set CERTS_DIR=%PROJECT_DIR%\certs
set MITMPROXY_DIR=%USERPROFILE%\.mitmproxy
set TRUSTSTORE_FILE=%CERTS_DIR%\mitmproxy-truststore.p12
set TRUSTSTORE_PASSWORD=changeit

echo === mitmproxy Setup Script ===

REM Create certs directory
if not exist "%CERTS_DIR%" mkdir "%CERTS_DIR%"

REM Check if mitmproxy CA certificate exists
if not exist "%MITMPROXY_DIR%\mitmproxy-ca-cert.pem" (
    echo mitmproxy CA certificate not found.
    echo Please run 'mitmproxy' or 'mitmweb' once manually to generate the certificate.
    echo.
    echo Installation:
    echo   pip install mitmproxy
    echo.
    echo Generate certificate:
    echo   mitmweb --listen-port 8888
    echo   ^(then press Ctrl+C to stop^)
    echo.
    pause
    exit /b 1
)

echo Found mitmproxy CA certificate: %MITMPROXY_DIR%\mitmproxy-ca-cert.pem

REM Remove existing truststore
if exist "%TRUSTSTORE_FILE%" del "%TRUSTSTORE_FILE%"

REM Convert PEM to PKCS12 truststore for Java
echo Creating Java truststore...
keytool -importcert ^
    -alias mitmproxy-ca ^
    -file "%MITMPROXY_DIR%\mitmproxy-ca-cert.pem" ^
    -keystore "%TRUSTSTORE_FILE%" ^
    -storetype PKCS12 ^
    -storepass %TRUSTSTORE_PASSWORD% ^
    -noprompt

if %errorlevel% neq 0 (
    echo ERROR: Failed to create truststore
    pause
    exit /b 1
)

echo.
echo === Setup Complete ===
echo.
echo Truststore created: %TRUSTSTORE_FILE%
echo Truststore password: %TRUSTSTORE_PASSWORD%
echo.
echo To start mitmproxy with web interface:
echo   mitmweb --listen-port 8888
echo.
echo To run the Spring Boot app with proxy:
echo   mvn spring-boot:run -Dspring-boot.run.profiles=proxy
echo.
echo Then access the mitmproxy web interface at: http://localhost:8081
echo.
pause
