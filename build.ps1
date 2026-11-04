param(
    [switch] $Test,
    [switch] $Editor,
    [int] $Level = 0
)

$ErrorActionPreference = 'Stop'
$tools = Join-Path $PSScriptRoot 'scripts\java-tools.ps1'
. $tools

$project = Join-Path $PSScriptRoot 'assets'
$levels = Join-Path $project 'levels'
if (-not (Test-Path -LiteralPath $levels)) {
    throw '找不到 assets\levels，请确认项目素材完整'
}

# 自检与平时运行共用编译入口，清理旧类后再编译，避免已删代码继续残留
$jdk = Find-JavaCompiler
$library = Get-JsonLibrary -Root $PSScriptRoot
$source = Join-Path $PSScriptRoot 'src'
$output = Build-JavaClasses -Root $PSScriptRoot -Jdk $jdk -Source $source -Library $library
$java = Join-Path $jdk 'java.exe'
$classpath = "$output;$library"
if ($Test) {
    & $java '-Djava.awt.headless=true' '-cp' $classpath 'pvz.SelfCheckTest' $project
} elseif ($Editor) {
    & $java '-cp' $classpath 'pvz.EditorMain' $project $Level
} else {
    & $java '-cp' $classpath 'pvz.Main' $project $Level
}
if ($LASTEXITCODE -ne 0) {
    throw '游戏、编辑器或自检程序异常退出，请查看上方的错误信息。'
}
