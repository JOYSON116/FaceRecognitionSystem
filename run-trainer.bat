@echo off
setlocal
cd /d "%~dp0"

call build.bat
if errorlevel 1 exit /b 1

echo Checking dataset quality...
java --enable-native-access=ALL-UNNAMED -Djava.library.path=native -cp "bin;lib\opencv-4120.jar" FaceTrainer
