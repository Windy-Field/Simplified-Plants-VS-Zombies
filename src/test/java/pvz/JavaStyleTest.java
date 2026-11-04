package pvz;

import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 检查项目 Java 源码的基础风格，识别字符串和注释后再检查，避免误报界面文字 */
public class JavaStyleTest {
    private static final Pattern TOKENS = Pattern.compile(
        "//[^\\r\\n]*|/\\*[\\s\\S]*?\\*/|\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'");
    private static final Pattern COMMENT_END = Pattern.compile("[。.]\\s*(?:\\*/|$)", Pattern.MULTILINE);
    private static final Pattern SHORTCUTS = Pattern.compile(
        "\\+\\+|--|\\+=|-=|\\*=|/=|%=|&=|\\|=|\\^=|<<=|>>=|->|::|\\.\\.\\.");
    private static final Pattern TERNARY = Pattern.compile("\\?(?!\\s*(?:extends|super)\\b|\\s*[,>])");
    private static final Pattern ANONYMOUS = Pattern.compile(
        "new\\s+[A-Za-z][A-Za-z0-9_.$]*(?:\\s*<[^;{}]*>)?\\s*\\([^;{}]*\\)\\s*\\{");

    /** 参数 source 是源码目录，递归检查所有 Java 文件，失败时报告文件和原因 */
    public static void run(Path source) throws Exception {
        checkRules();
        int count = checkDirectory(source);
        System.out.println("PASS: Java style checked for " + count + " source files");
    }

    /** 参数 directory 是当前目录，返回检查的文件数量，使用目录流确保读取后释放资源 */
    private static int checkDirectory(Path directory) throws Exception {
        int count = 0;
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(directory)) {
            for (Path entry : entries) {
                if (Files.isDirectory(entry)) {
                    count = count + checkDirectory(entry);
                } else {
                    Path fileName = entry.getFileName();
                    String name = fileName.toString();
                    if (name.endsWith(".java")) {
                        checkFile(entry);
                        count = count + 1;
                    }
                }
            }
        }
        return count;
    }

    /** 参数 path 是源码文件，只检查代码片段的快捷写法和注释的结尾格式 */
    private static void checkFile(Path path) throws Exception {
        String text = Files.readString(path, StandardCharsets.UTF_8);
        checkText(text, path.toString());
    }

    /** 参数 text 是源码，location 是报错位置；保留独立入口便于验证规则本身能否识别违规写法 */
    private static void checkText(String text, String location) {
        Matcher matcher = TOKENS.matcher(text);
        StringBuilder code = new StringBuilder();
        int previous = 0;
        while (matcher.find()) {
            code.append(text, previous, matcher.start());
            String token = matcher.group();
            if (token.startsWith("//") || token.startsWith("/*")) {
                Matcher commentEnd = COMMENT_END.matcher(token);
                if (commentEnd.find()) {
                    throw new IllegalStateException("注释结尾不要句号：" + location);
                }
            }
            code.append(" ");
            previous = matcher.end();
        }
        code.append(text.substring(previous));
        Matcher shortcuts = SHORTCUTS.matcher(code);
        if (shortcuts.find()) {
            throw new IllegalStateException("请使用完整语句：" + location + " / " + shortcuts.group());
        }
        Matcher anonymous = ANONYMOUS.matcher(code);
        if (anonymous.find()) {
            throw new IllegalStateException("请使用具名回调类：" + location);
        }
        Matcher ternary = TERNARY.matcher(code);
        if (ternary.find()) {
            throw new IllegalStateException("请用 if / else 代替三目运算符：" + location);
        }
    }

    /** 用少量代码片段检查新增规则，既要发现违规，也不能把字符串、注释和泛型通配符误判 */
    private static void checkRules() {
        String[] invalid = {
            "value++;", "value %= 2;", "value >>>= 1;", "value = ready ? 1 : 0;",
            "void print(String... names) {}", "Runnable task = () -> run();",
            "new java.lang.Runnable() { public void run() {} };",
            "new Handler<String>() { public void run() {} };", "// 注释结尾有句号。"
        };
        for (String text : invalid) {
            boolean rejected = false;
            try {
                checkText(text, "风格规则自检");
            } catch (IllegalStateException exception) {
                rejected = true;
            }
            if (!rejected) {
                throw new IllegalStateException("风格规则未发现违规片段：" + text);
            }
        }
        checkText("List<? extends Sprite> sprites; List<?> entries;", "泛型通配符");
        checkText("String text = \"++ -> ? ...\"; // 说明 -> ? ... 内容", "字符串和注释");
    }
}
