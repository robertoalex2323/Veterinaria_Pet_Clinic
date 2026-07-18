@echo off
cd /d "%~dp0"
if not exist ".env" if exist ".env.example" copy ".env.example" ".env" >nul
if not exist ".venv\Scripts\python.exe" py -3 -m venv .venv
".venv\Scripts\python.exe" -m pip install --no-cache-dir -r requirements.txt
if errorlevel 1 exit /b %errorlevel%
".venv\Scripts\python.exe" app.py
