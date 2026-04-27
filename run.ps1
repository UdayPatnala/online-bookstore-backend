$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$outMain = Join-Path $projectRoot "out\main"

if (Test-Path (Join-Path $projectRoot "out")) {
    Remove-Item -Recurse -Force (Join-Path $projectRoot "out")
}

New-Item -ItemType Directory -Force -Path $outMain | Out-Null

$mainSources = Get-ChildItem -Path (Join-Path $projectRoot "src\main\java") -Recurse -Filter *.java | Select-Object -ExpandProperty FullName
javac -d $outMain $mainSources

java -cp $outMain com.roadmap.bookstore.BookstoreApplication