@echo off
chcp 65001 >nul
set JAVA_HOME=%USERPROFILE%\tools\jdk-17.0.12
set PATH=%JAVA_HOME%\bin;%PATH%
java -Dfile.encoding=UTF-8 -jar "%~dp0target\auction-platform-1.0.0.jar"
