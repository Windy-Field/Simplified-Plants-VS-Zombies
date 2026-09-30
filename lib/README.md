# 项目依赖

本目录保存 Gson 2.11.0，供 IntelliJ IDEA、`build.ps1` 和 `package.ps1` 共同使用。分发源码时，请保留整个目录。

- 原始文件：[Maven Central 的 Gson 2.11.0](https://repo.maven.apache.org/maven2/com/google/code/gson/gson/2.11.0/gson-2.11.0.jar)。
- SHA256：`57928D6E5A6EDEB2ABD3770A8F95BA44DCE45F3B23B7A9DC2B309C581552A78B`。
- Gson 采用 Apache License 2.0，原始许可文本见 `GSON-LICENSE`。本项目的源代码许可不替代第三方依赖的许可。

`pom.xml` 使用 Maven 的 `system` 依赖直接引用项目内的 JAR。这适用于当前独立桌面程序的源码分发；如果以后把本项目发布为供其他项目引用的 Maven 库，应改用普通仓库依赖。
