@echo off
if not exist FAF-Chart.jar (
	echo Please unpack it and then execute it. Unable to find FAF-Chart.jar
	echo.
	echo Press any key to exit.
	pause>NUL
	goto exit
)

start javaw -jar FAF-Chart.jar
if %ERRORLEVEL% NEQ 0 (
	echo Please install java in order to use this tool. Unable to find javaw.exe
	echo.
	echo Press any key to close this
	pause>NUL
	goto exit
)




:exit