package com.example.treechat.file;

import org.apache.ibatis.annotations.Arg;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 *
 * Created by wuShan on 2025/10/17
 */
public class PathBasics {


    public static void main(String[] args) throws IOException {
        Path work = Paths.get("work");
        Files.createDirectories(work);
        //创建与写入
        Path p = work.resolve("hello.txt");
        Files.write(p,"你好\n".getBytes(), StandardOpenOption.CREATE);


        //追加写入
        Path tempFile = Files.createTempFile(work, "hello", ".tmp");

        Files.write(tempFile,"印花\n".getBytes(),StandardOpenOption.WRITE);
        Files.write(tempFile,"圣诞节\n".getBytes(),StandardOpenOption.APPEND);

        //读取与元数据
        List<String> lines = Files.readAllLines(p);
        lines.forEach(System.out::println);
        //安全删除
        Files.delete(p);
    }
}
