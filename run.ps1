$env:JAVA_HOME = "C:\Devtools\jdks\Corretto21"
$env:Path = "C:\Devtools\jdks\Corretto21\bin;$env:Path"

Write-Host "Iniciando Guitar Fretboard Trainer (Java 21)..." -ForegroundColor Cyan
if (Test-Path "target\fretboard-trainer-1.0.0.jar") {
    java -jar target\fretboard-trainer-1.0.0.jar
} else {
    mvn exec:java -Dexec.mainClass="com.threadboardlearn.App"
}
