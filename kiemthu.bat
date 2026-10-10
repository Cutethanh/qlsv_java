@echo off
chcp 65001 >nul
cd /d "%~dp0"
if not exist out mkdir out
dir /s /b src\*.java test\*.java > sources.txt
javac -encoding UTF-8 -d out @sources.txt
if errorlevel 1 (
  del sources.txt
  pause
  exit /b 1
)
del sources.txt
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp out qlsv.SelfTest
pause
