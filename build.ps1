$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
New-Item -ItemType Directory -Force build/classes, build/test-classes, dist | Out-Null
$sources = Get-ChildItem src/tictactoe/*.java | ForEach-Object FullName
& javac -encoding UTF-8 --release 17 -d build/classes $sources
if ($LASTEXITCODE -ne 0) { throw 'Compile failed' }
& javac -encoding UTF-8 --release 17 -cp build/classes -d build/test-classes test/tictactoe/GameModelTest.java
if ($LASTEXITCODE -ne 0) { throw 'Test compile failed' }
& java -cp 'build/classes;build/test-classes' tictactoe.GameModelTest
if ($LASTEXITCODE -ne 0) { throw 'Tests failed' }
$tempPath = (Resolve-Path build).Path
& jar "-J-Djava.io.tmpdir=$tempPath" --create --file dist/ssd_ip_cp4_2026f_252482_app.jar --main-class tictactoe.Main -C build/classes .
if ($LASTEXITCODE -ne 0) { throw 'Jar failed' }
& javadoc -quiet -encoding UTF-8 -docencoding UTF-8 -charset UTF-8 -public -d api_manual -sourcepath src -doctitle 'Expanding Tic-Tac-Toe Beta 1.0 API' tictactoe
if ($LASTEXITCODE -ne 0) { throw 'Javadoc failed' }
