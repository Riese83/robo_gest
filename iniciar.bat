@echo off
echo ========================================================
echo Compilando Robo_Gest y levantando contenedores Docker...
echo ========================================================
call mvn package -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo Error durante la compilacion con Maven.
    pause
    exit /b %ERRORLEVEL%
)

docker compose up -d
echo.
echo ========================================================
echo Sistema iniciado con exito!
echo Puedes acceder a: http://localhost:8080/robo_gest/
echo ========================================================
pause
