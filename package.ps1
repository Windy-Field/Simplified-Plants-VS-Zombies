param(
    [string] $Name = 'MyPVZ',
    [switch] $SkipPackage
)

# 一键打包脚本：把游戏、素材和一份精简的 Java 运行时打进同一个文件夹。
# 打包出来的 MyPVZ 文件夹是"绿色免安装"的：朋友解压后双击 启动游戏.bat 就能玩，
# 电脑上不需要装任何 Java，也不需要联网。
# 用法：
#   .\package.ps1                正常打包，最后生成 dist\MyPVZ.zip
#   .\package.ps1 -SkipPackage   只组装文件夹，不压缩（调试时省时间）

$ErrorActionPreference = 'Stop'

# 编译需要 JDK 17 或更高版本。
$requiredJavaVersion = 17

# 输出目录一律固定在项目根目录下，避免受当前工作目录影响。
$root = $PSScriptRoot
$result = @{
    Root = $root
    Source = Join-Path $root 'src\main\java'
    Assets = Join-Path $root 'assets'
    Levels = Join-Path $root 'assets\levels'
    Classes = Join-Path $root 'build\classes'
    Library = Join-Path $root 'build\gson-2.11.0.jar'
    Target = Join-Path $root ('dist\' + $Name)
}

# 和 build.ps1 一样固定下载这个版本的 JSON 解析器，附带来历和完整性校验。
$libraryAddress = 'https://repo.maven.apache.org/maven2/com/google/code/gson/gson/2.11.0/gson-2.11.0.jar'
$librarySha256 = '57928D6E5A6EDEB2ABD3770A8F95BA44DCE45F3B23B7A9DC2B309C581552A78B'

# 启动脚本的内容随发行包一起生成，保证脚本和打包出的目录结构永远对得上。
# 这里刻意全部用 ASCII 文件名：中文文件名在部分解压工具下会乱码，
# 也会让命令行里的引号转义变复杂。中文提示放在文件内容里，玩家一样看得懂。
$gameLauncherName = 'play-game.bat'
$editorLauncherName = 'level-editor.bat'
$diagnoseLauncherName = 'diagnose.bat'
$readmeName = 'readme.txt'


<#
    检查一个目录里的 javac 是否达到要求的版本。
    参数 directory 是 JDK 的 bin 目录。
    返回 $true 表示这个 JDK 可以用，返回 $false 表示不存在或版本太低。
#>
function Check-JavaVersion {
    param([string] $Directory)

    $compiler = Join-Path $Directory 'javac.exe'
    if (-not (Test-Path $compiler)) {
        return $false
    }
    # javac -version 会把版本号打到标准错误流（stderr）而不是标准输出，所以要合并两个流再读。
    $text = (& $compiler '-version' 2>&1 | Out-String).Trim()
    $match = [regex]::Match($text, 'javac\s+(\d+)')
    if (-not $match.Success) {
        return $false
    }
    $version = [int] $match.Groups[1].Value
    return $version -ge $requiredJavaVersion
}

<#
    找一个满足版本要求的 JDK，返回它的 bin 目录。
    查找顺序：JAVA_HOME、Program Files 下的常见安装位置。
    找不到就抛出中文错误提示。
#>
function Find-JavaCompiler {
    $candidates = New-Object System.Collections.ArrayList

    if ($env:JAVA_HOME) {
        [void] $candidates.Add((Join-Path $env:JAVA_HOME 'bin'))
    }
    # JDK 可能装在 Program Files\Java、Program Files\Microsoft、
    # Program Files\Eclipse Adoptium 等位置，逐个扫过去。
    $vendors = @(
        'C:\Program Files\Java',
        'C:\Program Files\Microsoft',
        'C:\Program Files\Eclipse Adoptium',
        'C:\Program Files\Amazon Corretto',
        'C:\Program Files\Zulu'
    )
    foreach ($vendor in $vendors) {
        if (-not (Test-Path $vendor)) {
            continue
        }
        # 只挑名字里带 jdk / jre 的目录，避免把无关软件当成 JDK。
        $entries = Get-ChildItem -Path $vendor -Directory -ErrorAction SilentlyContinue
        foreach ($entry in $entries) {
            $lower = $entry.Name.ToLower()
            if ($lower.Contains('jdk') -or $lower.Contains('java')) {
                [void] $candidates.Add((Join-Path $entry.FullName 'bin'))
            }
        }
    }

    foreach ($candidate in $candidates) {
        if (Check-JavaVersion $candidate) {
            return $candidate
        }
    }
    throw ('找不到 JDK ' + $requiredJavaVersion + ' 或更高版本。请安装 JDK 17 及以上，并设置 JAVA_HOME 环境变量后再运行本脚本。')
}

<#
    下载一份 gson 到 build 目录，并校验它的 SHA256。
    参数 state 是保存路径的字典。
#>
function Get-JsonLibrary {
    param([hashtable] $state)

    $library = $state.Library
    if (-not (Test-Path $library)) {
        Write-Host '正在下载 Gson 解析库……'
        $parent = Split-Path -Parent $library
        New-Item -ItemType Directory -Force -Path $parent | Out-Null
        Invoke-WebRequest -Uri $libraryAddress -OutFile $library
    }
    $actual = (Get-FileHash -Path $library -Algorithm SHA256).Hash
    if ($actual -ne $librarySha256) {
        throw '下载的 Gson 文件校验失败，请删除 build\gson-2.11.0.jar 后重新运行。'
    }
}

<#
    编译 src\main\java 下的全部源码。
    只编译游戏本体，不带 src\test 里的自检程序，发行包不需要它。
    参数 state 是保存路径的字典。
#>
function Build-Classes {
    param([hashtable] $state)

    if (Test-Path $state.Classes) {
        Remove-Item -Recurse -Force $state.Classes
    }
    New-Item -ItemType Directory -Force -Path $state.Classes | Out-Null

    $sources = Get-ChildItem $state.Source -Recurse -Filter '*.java'
    if ($sources.Count -eq 0) {
        throw '在 src\main\java 下没有找到任何 Java 源码，请确认项目结构是否完整。'
    }
    $paths = @()
    foreach ($source in $sources) {
        $paths += $source.FullName
    }
    Write-Host ('正在编译 ' + $paths.Count + ' 个源码文件……')
    & (Join-Path $state.Jdk 'javac.exe') '-encoding' 'UTF-8' '-cp' $state.Library '-d' $state.Classes $paths
    if ($LASTEXITCODE -ne 0) {
        throw 'Java 源码编译失败，请查看上方的错误信息。'
    }
}

<#
    把编译结果打成一个可执行的 game.jar。
    清单里写好主类和依赖，这样游戏和编辑器共用同一个 jar。
    参数 state 是保存路径的字典。
#>
function New-GameJar {
    param([hashtable] $state)

    $jar = Join-Path $state.Target 'game.jar'
    if (Test-Path $jar) {
        Remove-Item -Force $jar
    }
    # Class-Path 写在清单里，删掉外层的 -cp 参数也能直接双击运行这个 jar。
    $manifest = @(
        'Manifest-Version: 1.0',
        'Main-Class: pvz.Main',
        'Class-Path: gson-2.11.0.jar',
        ''
    )
    $manifestPath = Join-Path $state.Classes 'MANIFEST.MF'
    # jar 的清单文件必须是 UTF-8 且以换行结尾，否则可能被当成空清单。
    [System.IO.File]::WriteAllLines($manifestPath, $manifest, (New-Object System.Text.UTF8Encoding($false)))

    & (Join-Path $state.Jdk 'jar.exe') '--create' '--file' $jar '--manifest' $manifestPath '-C' $state.Classes '.'
    if ($LASTEXITCODE -ne 0) {
        throw '生成 game.jar 失败，请查看上方的错误信息。'
    }
    Remove-Item -Force $manifestPath
    $state.Jar = $jar
}

<#
    用 jlink 裁出一份只含游戏所需模块的运行时。
    游戏和编辑器只用到 Swing / AWT / ImageIO，也就是 java.desktop 这一个模块，
    所以裁出来的运行时只要 46MB 左右，比完整 JDK 小得多。
    参数 state 是保存路径的字典。
#>
function New-Runtime {
    param([hashtable] $state)

    $runtime = Join-Path $state.Target 'jre'
    if (Test-Path $runtime) {
        Remove-Item -Recurse -Force $runtime
    }
    Write-Host '正在生成精简 Java 运行时……'
    & (Join-Path $state.Jdk 'jlink.exe') '--add-modules' 'java.desktop' '--strip-debug' '--no-header-files' '--no-man-pages' '--compress=zip-6' '--output' $runtime
    if ($LASTEXITCODE -ne 0) {
        throw '生成精简运行时失败，请确认当前使用的 JDK 自带 jlink 工具。'
    }
}

<#
    把朋友会用到的启动脚本写进发行目录。
    参数 state 是保存路径的字典。
#>
function New-Launchers {
    param([hashtable] $state)

    # 启动脚本的第一件事就是把工作目录切到自己所在的文件夹
    # （cd /d "%~dp0"），所以整个发行目录可以随便改名、搬去桌面或 U 盘。
    $common = @(
        '@echo off',
        'rem 这是给玩家用的启动脚本，双击运行即可。',
        'rem 第一件事是切换到脚本所在的文件夹，这样整个目录可以随意移动。',
        'cd /d "%~dp0"',
        '',
        'rem 用自带的精简 Java 运行时，不依赖电脑上是否装过 Java。',
        'set "JAVA=%~dp0jre\bin\javaw.exe"',
        'if not exist "%JAVA%" (',
        '    echo.',
        '    echo [错误] 找不到自带的 Java 运行时：jre\bin\javaw.exe',
        '    echo 请确认整个文件夹是完整解压出来的，而不是只复制了其中一部分。',
        '    echo.',
        '    pause',
        '    exit /b 1',
        ')',
        '',
        'rem 切到中文代码页，保证下面的中文提示能正常显示。',
        'chcp 936 > nul',
        ''
    )

    $gameTail = @(
        'rem -Dfile.encoding=UTF-8 保证游戏读中文关卡文件时不会乱码。',
        '"%JAVA%" -Dfile.encoding=UTF-8 -cp "game.jar;gson-2.11.0.jar" pvz.Main assets',
        'if errorlevel 1 (',
        '    echo.',
        '    echo [错误] 游戏启动失败。',
        '    echo 请双击同目录下的 diagnose.bat，它会显示详细的错误信息。',
        '    echo.',
        '    pause',
        ')',
        'exit /b 0',
        ''
    )

    $editorTail = @(
        'rem 编辑器用同一个 jar，只是换成 pvz.EditorMain 这个入口。',
        '"%JAVA%" -Dfile.encoding=UTF-8 -cp "game.jar;gson-2.11.0.jar" pvz.EditorMain assets',
        'if errorlevel 1 (',
        '    echo.',
        '    echo [错误] 编辑器启动失败。',
        '    echo 请双击同目录下的 diagnose.bat，它会显示详细的错误信息。',
        '    echo.',
        '    pause',
        ')',
        'exit /b 0',
        ''
    )

    # 诊断脚本用 java.exe（带控制台）前台运行，出错时能把堆栈信息显示出来。
    $diagnose = @(
        '@echo off',
        'rem 诊断脚本：和启动脚本一样，但会保留控制台窗口显示详细报错信息。',
        'rem 游戏或编辑器打不开时，请双击本脚本，把窗口里的内容发给开发者。',
        'cd /d "%~dp0"',
        'chcp 936 > nul',
        'echo ===== 环境检查 =====',
        'echo 当前目录：%CD%',
        'echo 系统版本：',
        'ver',
        'echo.',
        'echo ===== 开始启动游戏（前台运行） =====',
        'jre\bin\java.exe -Dfile.encoding=UTF-8 -cp "game.jar;gson-2.11.0.jar" pvz.Main assets',
        'echo.',
        'echo ===== 程序已退出，退出码：%errorlevel% =====',
        'pause',
        ''
    )

    # 批处理文件必须用 GBK（代码页 936）编码写出。
    # 原因是 cmd.exe 按系统代码页逐字节读取 .bat 文件，中文 Windows 默认就是 GBK；
    # 如果写成 UTF-8，中文提示会变成乱码，而且 chcp 也救不回来
    # （chcp 只影响之后的输出，改不了 cmd 已经按 GBK 解析过的那些行）。
    $encoding = [System.Text.Encoding]::GetEncoding(936)
    $game = $common + $gameTail
    $editor = $common + $editorTail

    [System.IO.File]::WriteAllLines((Join-Path $state.Target $gameLauncherName), $game, $encoding)
    [System.IO.File]::WriteAllLines((Join-Path $state.Target $editorLauncherName), $editor, $encoding)
    [System.IO.File]::WriteAllLines((Join-Path $state.Target $diagnoseLauncherName), $diagnose, $encoding)
}

<#
    给玩家看的一份简短说明，放在发行目录里。
    参数 state 是保存路径的字典。
#>
function New-Readme {
    param([hashtable] $state)

    $lines = @(
        '植物大战僵尸（Java 版）—— 免安装版',
        '',
        '怎么玩',
        '  1. 双击「play-game.bat」，等几秒就会弹出游戏窗口。',
        '  2. 想改关卡就双击「level-editor.bat」。',
        '',
        '说明',
        '  - 本文件夹已经自带 Java 运行环境，你的电脑不需要安装任何东西，也不需要联网。',
        '  - 请把整个文件夹一起解压出来再运行，不要只复制其中某几个文件。',
        '  - 从网上下载的压缩包，Windows 可能会提示「未知发布者」，点「仍要运行」即可。',
        '  - 整个文件夹可以随意改名、放到桌面或拷进 U 盘，不影响运行。',
        '  - 文件名用英文是为了兼容各种解压工具，内容是中文的，照着做就行。',
        '',
        '打不开怎么办',
        '  双击「diagnose.bat」，它会停留在黑窗口里显示出错信息。',
        '  把窗口里的内容截图发给开发者，就能定位问题。',
        '',
        '操作方式',
        '  游戏里点击卡片选择植物，再点草坪种下；编辑器里用鼠标拖动摆放植物和僵尸。',
        '  具体按键和玩法见项目 README。',
        ''
    )
    # 说明文件带 BOM：Windows 记事本靠 BOM 判断编码，缺了它中文会显示成乱码。
    $encoding = New-Object System.Text.UTF8Encoding($true)
    [System.IO.File]::WriteAllLines((Join-Path $state.Target $readmeName), $lines, $encoding)
}

<#
    把素材原样拷进发行目录。
    参数 state 是保存路径的字典。
#>
function Copy-Assets {
    param([hashtable] $state)

    Write-Host '正在复制素材（约 49MB，需要一点时间）……'
    # 用 robocopy 而不是 Copy-Item：素材里有几千个小文件，robocopy 快得多。
    # /NFL /NDL /NJH /NJS 是让 robocopy 安静些，不要刷满整个屏幕。
    $target = Join-Path $state.Target 'assets'
    & robocopy $state.Assets $target /E /NFL /NDL /NJH /NJS /NP | Out-Null
    # robocopy 用小于 8 的退出码表示成功（1 表示确实复制了文件），8 及以上才是真的出错。
    if ($LASTEXITCODE -ge 8) {
        throw '复制素材目录失败，请查看上方的 robocopy 输出。'
    }
    $global:LASTEXITCODE = 0
}

<#
    把发行目录压成一个 zip，方便发给朋友。
    参数 state 是保存路径的字典。
#>
function Compress-Result {
    param([hashtable] $state)

    $zip = $state.Target + '.zip'
    if (Test-Path $zip) {
        Remove-Item -Force $zip
    }
    Write-Host '正在压缩，这一步比较慢，请耐心等待……'
    Compress-Archive -Path $state.Target -DestinationPath $zip -CompressionLevel Optimal
    return $zip
}

<#
    把字节数换算成 MB，用于在最后打印体积。
#>
function Format-Size {
    param([long] $Bytes)

    $megabytes = $Bytes / 1MB
    return ('{0:N1} MB' -f $megabytes)
}


# ===== 以下是主流程，按顺序执行上面的各个步骤 =====

if (-not (Test-Path $result.Levels)) {
    throw '找不到素材目录 assets（或其中的 levels 关卡目录），请先获取资源包并放到 java\assets 下。'
}

$result.Jdk = Find-JavaCompiler
Write-Host ('使用 JDK：' + (Split-Path -Parent $result.Jdk))

Get-JsonLibrary -state $result
Build-Classes -state $result

# 组装发行目录：先从干净的目录开始，避免上一次打包的残留文件混进去。
if (Test-Path $result.Target) {
    Remove-Item -Recurse -Force $result.Target
}
New-Item -ItemType Directory -Force -Path $result.Target | Out-Null

New-GameJar -state $result
New-Runtime -state $result
Copy-Item -Path $result.Library -Destination (Join-Path $result.Target 'gson-2.11.0.jar')
Copy-Assets -state $result
New-Launchers -state $result
New-Readme -state $result

Write-Host ''
Write-Host '打包完成！'
Write-Host ('发行目录：' + $result.Target)

if ($SkipPackage) {
    Write-Host '（已按 -SkipPackage 跳过压缩步骤）'
    exit 0
}

$zipPath = Compress-Result -state $result
$folderSize = (Get-ChildItem $result.Target -Recurse -File | Measure-Object -Property Length -Sum).Sum
Write-Host ('文件夹大小：' + (Format-Size $folderSize))
Write-Host ('压缩包大小：' + (Format-Size (Get-Item $zipPath).Length))
Write-Host ('压缩包位置：' + $zipPath)
Write-Host ''
Write-Host ''
Write-Host '把上面这个 zip 发给朋友即可。对方解压后双击 play-game.bat 就能玩，不需要装 Java。'
