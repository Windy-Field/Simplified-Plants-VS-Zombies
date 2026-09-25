param(
    [switch] $Test,
    [switch] $Editor,
    [int] $Level = 1
)

# 使用真正的 JDK，避免系统 javapath 启动器指向缺失的安装。
$jdk = 'C:\Program Files\Java\jdk-17\bin'
if ($env:JAVA_HOME) {
    $candidate = Join-Path $env:JAVA_HOME 'bin'
    if (Test-Path (Join-Path $candidate 'javac.exe')) {
        $jdk = $candidate
    }
}
if (-not (Test-Path (Join-Path $jdk 'javac.exe'))) {
    throw '需要 JDK 17 或更高版本，请安装后设置 JAVA_HOME。'
}
# 图片素材和关卡配置都放在 assets 目录里；它不随代码仓库分发，缺少时给出明确提示。
$project = Join-Path $PSScriptRoot 'assets'
if (-not (Test-Path (Join-Path $project 'levels'))) {
    throw '找不到素材目录 assets（或其中的 levels 关卡目录），请先获取资源包并放到 java\assets 下。'
}
$output = Join-Path $PSScriptRoot 'build\classes'
$library = Join-Path $PSScriptRoot 'build\gson-2.11.0.jar'
New-Item -ItemType Directory -Force -Path $output | Out-Null

# 下载固定版本的 JSON 解析器，避免自己拼凑 JSON 字符串解析。
if (-not (Test-Path $library)) {
    $address = 'https://repo.maven.apache.org/maven2/com/google/code/gson/gson/2.11.0/gson-2.11.0.jar'
    Invoke-WebRequest -Uri $address -OutFile $library
}
$expected = '57928D6E5A6EDEB2ABD3770A8F95BA44DCE45F3B23B7A9DC2B309C581552A78B'
$actual = (Get-FileHash -Path $library -Algorithm SHA256).Hash
if ($actual -ne $expected) {
    throw '下载的 Gson 文件校验失败，请删除 build\gson-2.11.0.jar 后重新运行。'
}

# 编译源文件与随项目保留的验证程序。
$sources = Get-ChildItem (Join-Path $PSScriptRoot 'src') -Recurse -Filter '*.java'
$paths = @()
foreach ($source in $sources) {
    $paths += $source.FullName
}
& (Join-Path $jdk 'javac.exe') '-encoding' 'UTF-8' '-cp' $library '-d' $output $paths
if ($LASTEXITCODE -ne 0) {
    throw 'Java 源码编译失败，请查看上方的错误信息。'
}
$classpath = "$output;$library"
if ($Test) {
    & (Join-Path $jdk 'java.exe') '-Djava.awt.headless=true' '-cp' $classpath 'pvz.SelfCheckTest' $project
} elseif ($Editor) {
    & (Join-Path $jdk 'java.exe') '-cp' $classpath 'pvz.EditorMain' $project $Level
} else {
    & (Join-Path $jdk 'java.exe') '-cp' $classpath 'pvz.Main' $project $Level
}
if ($LASTEXITCODE -ne 0) {
    throw '游戏、编辑器或自检程序异常退出，请查看上方的错误信息。'
}
