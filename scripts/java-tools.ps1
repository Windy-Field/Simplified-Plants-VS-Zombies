# 构建和打包共用的参数放在这里，升级 JDK 要求或 Gson 时只修改一处
$requiredJavaVersion = 17
$libraryRelativePath = 'lib\gson-2.11.0.jar'
$librarySha256 = '57928D6E5A6EDEB2ABD3770A8F95BA44DCE45F3B23B7A9DC2B309C581552A78B'
$javaVendors = @(
    'C:\Program Files\Java',
    'C:\Program Files\Microsoft',
    'C:\Program Files\Eclipse Adoptium',
    'C:\Program Files\Amazon Corretto',
    'C:\Program Files\Zulu'
)

# 参数 Directory 是 JDK 的 bin 目录，返回编译器是否存在且版本达到要求
function Test-JavaVersion {
    param([string] $Directory)

    $compiler = Join-Path $Directory 'javac.exe'
    if (-not (Test-Path -LiteralPath $compiler)) {
        return $false
    }
    # javac 的版本可能写在错误流里，因此合并后一起读取
    $output = & $compiler '-version' 2>&1
    $text = $output | Out-String
    $match = [regex]::Match($text, 'javac\s+(\d+)')
    if (-not $match.Success) {
        return $false
    }
    $version = [int] $match.Groups[1].Value
    return $version -ge $requiredJavaVersion
}

# 返回可用 JDK 的 bin 目录，优先 JAVA_HOME，再检查常见安装位置
function Find-JavaCompiler {
    $candidates = New-Object 'System.Collections.Generic.List[string]'
    if ($env:JAVA_HOME) {
        $directory = Join-Path $env:JAVA_HOME 'bin'
        $candidates.Add($directory)
    }
    foreach ($vendor in $javaVendors) {
        if (-not (Test-Path -LiteralPath $vendor)) {
            continue
        }
        $entries = Get-ChildItem -LiteralPath $vendor -Directory -ErrorAction SilentlyContinue
        foreach ($entry in $entries) {
            $name = $entry.Name.ToLower()
            if ($name.Contains('jdk') -or $name.Contains('java')) {
                $directory = Join-Path $entry.FullName 'bin'
                $candidates.Add($directory)
            }
        }
    }
    foreach ($candidate in $candidates) {
        if (Test-JavaVersion $candidate) {
            return $candidate
        }
    }
    throw ('需要 JDK ' + $requiredJavaVersion + ' 或更高版本，请安装后设置 JAVA_HOME')
}

# 参数 Root 是项目目录，返回已核对 SHA256 的 Gson 路径，避免运行损坏的依赖
function Get-JsonLibrary {
    param([string] $Root)

    $library = Join-Path $Root $libraryRelativePath
    if (-not (Test-Path -LiteralPath $library)) {
        throw ('找不到 ' + $libraryRelativePath + '，请重新获取完整的项目文件夹')
    }
    $hash = Get-FileHash -LiteralPath $library -Algorithm SHA256
    if ($hash.Hash -ne $librarySha256) {
        throw 'Gson 文件校验失败，请使用完整项目中的依赖文件替换'
    }
    return $library
}

# 参数 Root 是项目目录，Target 是待清空的输出子目录；检查绝对路径后再删除旧产物
function Clear-GeneratedDirectory {
    param([string] $Root, [string] $Target)

    $rootPath = [System.IO.Path]::GetFullPath($Root)
    $rootPath = $rootPath.TrimEnd('\', '/')
    $prefix = $rootPath + [System.IO.Path]::DirectorySeparatorChar
    $targetPath = [System.IO.Path]::GetFullPath($Target)
    if (-not $targetPath.StartsWith($prefix, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw ('输出目录必须位于项目内部：' + $targetPath)
    }
    if (Test-Path -LiteralPath $targetPath) {
        Remove-Item -LiteralPath $targetPath -Recurse -Force
    }
    New-Item -ItemType Directory -Path $targetPath -Force | Out-Null
}

# 参数 Root、Jdk、Source、Library 指定编译输入，返回输出目录；先清除旧类，避免已删代码继续残留
function Build-JavaClasses {
    param([string] $Root, [string] $Jdk, [string] $Source, [string] $Library)

    $output = Join-Path $Root 'build\classes'
    $sources = Get-ChildItem -LiteralPath $Source -Recurse -Filter '*.java'
    $paths = New-Object 'System.Collections.Generic.List[string]'
    foreach ($sourceFile in $sources) {
        $paths.Add($sourceFile.FullName)
    }
    if ($paths.Count -eq 0) {
        throw ('没有找到 Java 源码：' + $Source)
    }
    Clear-GeneratedDirectory -Root $Root -Target $output
    $compiler = Join-Path $Jdk 'javac.exe'
    $arguments = $paths.ToArray()
    & $compiler '-encoding' 'UTF-8' '--release' $requiredJavaVersion '-cp' $Library '-d' $output $arguments
    if ($LASTEXITCODE -ne 0) {
        throw 'Java 源码编译失败，请查看上方的错误信息'
    }
    return $output
}
