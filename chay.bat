@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo Dang bien dich ma nguon...
if not exist out mkdir out
dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 -d out @sources.txt
if errorlevel 1 (
  del sources.txt
  echo Bien dich that bai. Hay cai JDK 11 tro len va kiem tra lenh javac.
  pause
  exit /b 1
)
del sources.txt
java -cp out qlsv.Main
