@echo off
echo LINE1
echo LINE2
java -javaagent:authlib-injector.jar=https://skin.prinzeugen.net/api/yggdrasil -jar paper.jar
echo LINE3
echo LINE4