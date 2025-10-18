package com.example.treechat.file;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * Created by wuShan on 2025/10/17
 */
public class CharsetAndText {

    public static void main(String[] args) throws IOException {
        Path path = Paths.get("work/charset.txt");
        Files.createDirectories(path.getParent());
        List<String> list = List.of("Java 文件处理 高级示例", "Java NIO 流式处理", "编码 UTF-8 测试");
        Files.write(path,list, StandardCharsets.UTF_8);

        Map<String, Long> map = Files.lines(path).flatMap(s -> Arrays.stream(s.toLowerCase().split("[^a-zA-Z\\u4e00-\\u9fa5]+")))
                .filter(w -> !w.isBlank())
                .collect(Collectors.groupingBy(w -> w, Collectors.counting()));

        Path paths = Paths.get("work/gdb.txt");
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        Files.write(paths, lines, Charset.forName("GBK"));
    }
}
