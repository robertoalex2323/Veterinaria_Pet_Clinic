$ErrorActionPreference = "Stop"

Set-Location $PSScriptRoot
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
