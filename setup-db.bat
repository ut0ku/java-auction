@echo off
setlocal
set PSQL="C:\Program Files\PostgreSQL\18\bin\psql.exe"
set PGUSER=postgres
if "%PGPASSWORD%"=="" set PGPASSWORD=postgres

echo Creating database auction_db...
%PSQL% -c "CREATE DATABASE auction_db;" 2>nul

echo Applying schema...
%PSQL% -d auction_db -f "%~dp0sql\schema.sql"

echo Applying seed data...
%PSQL% -d auction_db -f "%~dp0sql\seed.sql"

echo Done. Update src\main\resources\db.properties if your postgres password differs.
endlocal
