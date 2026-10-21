@echo off
setlocal
echo Iniciando Guitar Fretboard Trainer (Java 21)...

if exist "C:\Devtools\jdks\Corretto21\bin\java.exe" (
    set "JAVA_HOME=C:\Devtools\jdks\Corretto21"
    set "PATH=C:\Devtools\jdks\Corretto21\bin;%PATH%"
)

if exist "target\fretboard-trainer-1.0.0.jar" (
    java -jar target\fretboard-trainer-1.0.0.jar
) else (
    mvn exec:java -Dexec.mainClass="com.threadboardlearn.App"
)
