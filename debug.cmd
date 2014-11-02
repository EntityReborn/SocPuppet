@echo off
:loop
java -Xdebug -Xrunjdwp:transport=dt_socket,address=1000,server=y,suspend=n -XX:MaxPermSize=128M -Xmx1G -jar target\SocPuppet-0.0.0.jar
pause
goto loop