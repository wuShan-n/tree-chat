package com.example.treechat.file;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 *
 * Created by wuShan on 2025/10/17
 */
public class ChannelsAndMap {

    public static void main(String[] args) throws IOException {
        Path path = Paths.get("work/big.bin");
        Files.createFile(path);

        FileChannel channel = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        byte[] bytes = new byte[1024 * 1024];
        for (int i = 0; i < bytes.length; i++) bytes[i] = (byte) (i % 256);
        for (int i=0;i<10;i++){
            channel.write(ByteBuffer.wrap(bytes));
        }

        System.out.println("写入大文件完成: " + path.toAbsolutePath());


        // 内存映射读取与就地修改
        try (FileChannel ch = FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
            MappedByteBuffer map = ch.map(FileChannel.MapMode.READ_WRITE, 0, Math.min(ch.size(), 4 * 1024 * 1024)); // 映射前 4MB
            for (int i = 0; i < map.limit(); i += 4096) { // 每 4KB 修改一个字节
                byte b = map.get(i);
                map.put(i, (byte)(b ^ 0xFF));
            }
            map.force();
        }
        System.out.println("内存映射修改完成");

    }
}
