@echo off
setlocal
cd /d "%~dp0"

echo Building Face Recognition System...

echo Building with OpenCV recognition support...
javac -cp "lib\opencv-4120.jar" -d bin src\*.java

if errorlevel 1 (
    echo.
    echo Build failed.
    exit /b 1
)

echo Build complete.
