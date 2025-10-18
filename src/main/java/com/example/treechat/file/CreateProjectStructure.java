package com.example.treechat.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * 运行此 main 方法以在当前目录中创建 'minio-presign-demo' 
 * 项目的完整目录结构和所有空的 Java/配置 文件。
 *
 * 不会向文件中写入任何内容。
 */
public class CreateProjectStructure {

    public static void main(String[] args) {
        String baseDir = "D:\\file\\python\\java\\ai\\tree-chat\\minio-presign-demo";

        // 定义所有需要创建的文件（包含相对路径）
        // 目录将根据文件路径自动创建
        List<String> filesToCreate = Arrays.asList(
                baseDir + "/pom.xml",
                baseDir + "/src/main/java/com/example/miniodemo/MinioDemoApplication.java",
                baseDir + "/src/main/java/com/example/miniodemo/config/S3Config.java",
                baseDir + "/src/main/java/com/example/miniodemo/controller/UploadController.java",
                baseDir + "/src/main/java/com/example/miniodemo/controller/DownloadController.java",
                baseDir + "/src/main/java/com/example/miniodemo/dto/PresignReq.java",
                baseDir + "/src/main/java/com/example/miniodemo/dto/PresignResp.java",
                baseDir + "/src/main/java/com/example/miniodemo/entity/Image.java",
                baseDir + "/src/main/java/com/example/miniodemo/entity/ImageVariant.java",
                baseDir + "/src/main/java/com/example/miniodemo/mapper/ImageMapper.java",
                baseDir + "/src/main/java/com/example/miniodemo/service/UploadService.java",
                baseDir + "/src/main/java/com/example/miniodemo/service/ProcessingService.java",
                baseDir + "/src/main/java/com/example/miniodemo/util/Ids.java",
                baseDir + "/src/main/resources/application.yml",
                baseDir + "/src/main/resources/schema.sql",
                baseDir + "/dev/docker-compose.minio.yml",
                baseDir + "/dev/cors.json"
        );

        System.out.println("正在 '" + Paths.get("").toAbsolutePath() + "' 中创建项目结构...");

        int dirsCreated = 0;
        int filesCreated = 0;

        for (String filePath : filesToCreate) {
            try {
                Path path = Paths.get(filePath);
                
                // 1. 获取父目录
                Path parentDir = path.getParent();

                // 2. 如果父目录不为 null 且不存在，则创建它
                if (parentDir != null && !Files.exists(parentDir)) {
                    Files.createDirectories(parentDir);
                    System.out.println("  创建目录: " + parentDir.toString());
                    dirsCreated++;
                }

                // 3. 创建空文件（如果它还不存在）
                if (!Files.exists(path)) {
                    Files.createFile(path);
                    System.out.println("  创建文件: " + path.toString());
                    filesCreated++;
                }

            } catch (IOException e) {
                System.err.println("创建失败 " + filePath + ": " + e.getMessage());
            }
        }

        System.out.println("\n...结构创建完毕。");
        System.out.println("总计: 创建了 " + dirsCreated + " 个目录, " + filesCreated + " 个文件。");
    }
}