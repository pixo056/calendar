@echo off
setlocal
cd /d "%~dp0"
set "SERVER_ADDRESS=0.0.0.0"
echo Phone access: connect the phone and this PC to the same Wi-Fi.
echo Open http://YOUR_PC_WIFI_IP:8080 on your phone. Find the address using ipconfig.
echo Stop the existing Calendar instance before starting this one.
call start.cmd
