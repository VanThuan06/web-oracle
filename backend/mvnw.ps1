$ErrorActionPreference = 'Stop'
$MavenArgs = $args
$version = '3.9.16'
$cacheRoot = Join-Path $env:USERPROFILE ".m2/wrapper/dists/apache-maven-$version"
$mavenCommand = Get-ChildItem -Path $cacheRoot -Filter 'mvn.cmd' -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName

if (-not $mavenCommand) {
    New-Item -ItemType Directory -Path $cacheRoot -Force | Out-Null
    $archive = Join-Path $cacheRoot "apache-maven-$version-bin.zip"
    $url = "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$version/apache-maven-$version-bin.zip"
    Invoke-WebRequest -Uri $url -OutFile $archive
    Expand-Archive -LiteralPath $archive -DestinationPath $cacheRoot -Force
    $mavenCommand = Get-ChildItem -Path $cacheRoot -Filter 'mvn.cmd' -Recurse | Select-Object -First 1 -ExpandProperty FullName
}

if (-not $mavenCommand) { throw "Could not bootstrap Apache Maven $version." }
& $mavenCommand @MavenArgs
exit $LASTEXITCODE
