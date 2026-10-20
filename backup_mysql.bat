@echo off
REM ==============================================================================
REM SCRIPT DE RESPALDO PARA ROBO_GEST (WINDOWS / DOCKER DESKTOP)
REM ==============================================================================

set BACKUP_DIR=%~dp0backups
if not exist "%BACKUP_DIR%" mkdir "%BACKUP_DIR%"

for /f "tokens=2 delims==" %%I in ('wmic os get localdatetime /value') do set dt=%%I
set TIMESTAMP=%dt:~0,4%%dt:~4,2%%dt:~6,2%_%dt:~8,2%%dt:~10,2%%dt:~12,2%
set FILENAME=backup_robogest_%TIMESTAMP%.sql

echo [%date% %time%] Realizando respaldo de la base de datos MySQL en Docker...
docker exec mysql_club_robotica mysqldump -u root -proot_password --single-transaction --routines --triggers club_robotica > "%BACKUP_DIR%\%FILENAME%"

if exist "%BACKUP_DIR%\%FILENAME%" (
    echo [%date% %time%] Respaldo generado con exito en: %BACKUP_DIR%\%FILENAME%
) else (
    echo [%date% %time%] ERROR: No se pudo generar el archivo de respaldo.
)
