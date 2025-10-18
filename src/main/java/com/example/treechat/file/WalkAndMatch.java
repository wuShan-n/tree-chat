package com.example.treechat.file;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;

/**
 *
 * Created by wuShan on 2025/10/17
 */
public class WalkAndMatch {
    public static void main(String[] args) throws IOException {
        Path root = Paths.get("work/tree");
        Files.createDirectories(root.resolve("a/b"));
        Files.writeString(root.resolve("a/notes.md"), "#notes\n");
        Files.writeString(root.resolve("a/b/todo.txt"), "todo\n");
        Files.writeString(root.resolve("a/b/code.java"), "class X{}\n");

        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**/*.{md,txt}");
        ArrayList<Path> paths = new ArrayList<>();
        Path path = Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (matcher.matches(file)) paths.add(file);
                return FileVisitResult.CONTINUE;
            }
        });

        System.out.println("匹配到: " + paths);

        Path backup = Paths.get("work/backup");
        copyTree(root, backup);
        System.out.println("已复制" + backup);


    }

    private static void copyTree(Path from, Path to) throws IOException {
        Files.createDirectories(to);
        Files.walkFileTree(from, new SimpleFileVisitor<>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path resolve = to.resolve(from.relativize(dir));
                Files.createDirectories(resolve);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path resolve = to.resolve(from.relativize(file));
                Files.createFile(resolve);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
