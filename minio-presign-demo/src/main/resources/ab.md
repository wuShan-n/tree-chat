下面是一套可直接运行的 **方案一（直传 MinIO + 预签名 URL）** 的最小可用 Demo。

---

## 1) 项目结构

```
minio-presign-demo/
├─ pom.xml
├─ src/main/java/com/example/miniodemo/
│  ├─ MinioDemoApplication.java
│  ├─ config/S3Config.java
│  ├─ controller/UploadController.java
│  ├─ controller/DownloadController.java
│  ├─ dto/PresignReq.java
│  ├─ dto/PresignResp.java
│  ├─ entity/Image.java
│  ├─ entity/ImageVariant.java
│  ├─ mapper/ImageMapper.java
│  ├─ service/UploadService.java
│  ├─ service/ProcessingService.java
│  └─ util/Ids.java
├─ src/main/resources/
│  ├─ application.yml
│  └─ schema.sql
└─ dev/
   ├─ docker-compose.minio.yml
   └─ cors.json
```

---

## 2) 运行 MinIO（本地开发）

**dev/docker-compose.minio.yml**

```yaml
version: "3.8"
services:
  minio:
    image: minio/minio:latest
    container_name: minio
    command: server /data --console-address ":9001"
    environment:
      - MINIO_ROOT_USER=minioadmin
      - MINIO_ROOT_PASSWORD=minioadmin
    ports:
      - "9000:9000"   # S3 API
      - "9001:9001"   # Web 控制台
    volumes:
      - ./data:/data
```

启动：

```bash
cd dev
docker compose -f docker-compose.minio.yml up -d
```

创建 Bucket 与 CORS（浏览器直传需要 CORS）：

```bash
# 安装 mc（MinIO Client），或使用容器运行：
docker run --rm -it --network host -v $(pwd):/work minio/mc sh

# 在容器里：
mc alias set local http://127.0.0.1:9000 minioadmin minioadmin
mc mb local/minio-demo
mc cors set local/minio-demo /work/cors.json
```

**dev/cors.json**（允许本地前端直传；生产请锁定域名）

```json
[
  {
    "AllowedOrigin": ["http://localhost:5173", "http://localhost:3000", "http://127.0.0.1:8080", "http://localhost:8080"],
    "AllowedMethod": ["GET", "PUT", "HEAD"],
    "AllowedHeader": ["*"],
    "ExposeHeader": ["ETag"],
    "MaxAgeSeconds": 3000
  }
]
```

---

## 3) Spring Boot 配置

**src/main/resources/application.yml**

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:h2:mem:imgdb;MODE=MySQL;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password: ""
  sql:
    init:
      mode: always
  h2:
    console:
      enabled: true

mybatis:
  configuration:
    map-underscore-to-camel-case: true

app:
  bucket: minio-demo
  s3:
    endpoint: http://127.0.0.1:9000
    region: us-east-1
    access-key: minioadmin
    secret-key: minioadmin
    path-style: true  # MinIO 推荐开启
```

**src/main/resources/schema.sql**

```sql
CREATE TABLE images (
  id            BIGINT PRIMARY KEY,
  owner_id      BIGINT      NOT NULL,
  original_name VARCHAR(255) NOT NULL,
  content_type  VARCHAR(100) NOT NULL,
  storage_key   VARCHAR(512) NOT NULL,
  size_bytes    BIGINT       NOT NULL,
  width         INT          NULL,
  height        INT          NULL,
  sha256_hex    CHAR(64)     NULL,
  status        VARCHAR(20)  NOT NULL,
  created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  updated_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE image_variants (
  id            BIGINT PRIMARY KEY,
  image_id      BIGINT NOT NULL,
  variant       VARCHAR(50) NOT NULL,
  width         INT        NULL,
  height        INT        NULL,
  format        VARCHAR(10) NOT NULL,
  storage_key   VARCHAR(512) NOT NULL,
  size_bytes    BIGINT      NULL,
  created_at    TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(image_id, variant)
);

CREATE INDEX idx_images_owner ON images(owner_id);
CREATE INDEX idx_images_status ON images(status);
```

---

## 4) 依赖管理（支持 WebP 生成）

**pom.xml**

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.example</groupId>
  <artifactId>minio-presign-demo</artifactId>
  <version>1.0.0</version>
  <properties>
    <java.version>17</java.version>
    <spring-boot.version>3.3.4</spring-boot.version>
    <aws.sdk.v2.version>2.25.66</aws.sdk.v2.version>
  </properties>

  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-dependencies</artifactId>
        <version>${spring-boot.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>

  <dependencies>
    <!-- Spring -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- DB & MyBatis & H2 -->
    <dependency>
      <groupId>org.mybatis.spring.boot</groupId>
      <artifactId>mybatis-spring-boot-starter</artifactId>
      <version>3.0.3</version>
    </dependency>
    <dependency>
      <groupId>com.h2database</groupId>
      <artifactId>h2</artifactId>
      <scope>runtime</scope>
    </dependency>

    <!-- AWS SDK v2 for S3/Presign (兼容 MinIO) -->
    <dependency>
      <groupId>software.amazon.awssdk</groupId>
      <artifactId>s3</artifactId>
      <version>${aws.sdk.v2.version}</version>
    </dependency>
    <dependency>
      <groupId>software.amazon.awssdk</groupId>
      <artifactId>sts</artifactId>
      <version>${aws.sdk.v2.version}</version>
    </dependency>

    <!-- ULID for ids -->
    <dependency>
      <groupId>com.github.f4b6a3</groupId>
      <artifactId>ulid-creator</artifactId>
      <version>5.2.3</version>
    </dependency>

    <!-- Image processing -->
    <dependency>
      <groupId>net.coobird</groupId>
      <artifactId>thumbnailator</artifactId>
      <version>0.4.20</version>
    </dependency>
    <!-- WebP ImageIO plugin -->
    <dependency>
      <groupId>com.twelvemonkeys.imageio</groupId>
      <artifactId>imageio-webp</artifactId>
      <version>3.11.0</version>
    </dependency>
    <!-- MIME 嗅探 -->
    <dependency>
      <groupId>org.apache.tika</groupId>
      <artifactId>tika-core</artifactId>
      <version>2.9.2</version>
    </dependency>

    <!-- Lombok（可选） -->
    <dependency>
      <groupId>org.projectlombok</groupId>
      <artifactId>lombok</artifactId>
      <optional>true</optional>
    </dependency>

    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
      </plugin>
    </plugins>
  </build>
</project>
```

---

## 5) 代码

### 5.1 启动类

**MinioDemoApplication.java**

```java
package com.example.miniodemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MinioDemoApplication {
  public static void main(String[] args) {
    SpringApplication.run(MinioDemoApplication.class, args);
  }
}
```

### 5.2 S3 客户端配置（MinIO 兼容）

**config/S3Config.java**

```java
package com.example.miniodemo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
public class S3Config {
  @Value("${app.s3.endpoint}") private String endpoint;
  @Value("${app.s3.region}") private String region;
  @Value("${app.s3.access-key}") private String accessKey;
  @Value("${app.s3.secret-key}") private String secretKey;
  @Value("${app.s3.path-style:true}") private boolean pathStyle;

  private StaticCredentialsProvider creds() {
    return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
  }

  @Bean
  public S3Client s3Client() {
    return S3Client.builder()
        .region(Region.of(region))
        .endpointOverride(URI.create(endpoint))
        .credentialsProvider(creds())
        .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(pathStyle).build())
        .build();
  }

  @Bean
  public S3Presigner s3Presigner() {
    return S3Presigner.builder()
        .region(Region.of(region))
        .endpointOverride(URI.create(endpoint))
        .credentialsProvider(creds())
        .build();
  }
}
```

### 5.3 实体

**entity/Image.java**

```java
package com.example.miniodemo.entity;

import lombok.Data;

@Data
public class Image {
  private Long id;
  private Long ownerId;
  private String originalName;
  private String contentType;
  private String storageKey;
  private Long sizeBytes;
  private Integer width;
  private Integer height;
  private String sha256Hex;
  private String status; // UPLOADING | READY | FAILED
}
```

**entity/ImageVariant.java**

```java
package com.example.miniodemo.entity;

import lombok.Data;

@Data
public class ImageVariant {
  private Long id;
  private Long imageId;
  private String variant;   // w1024 / thumb 等
  private Integer width;
  private Integer height;
  private String format;    // webp/jpg
  private String storageKey;
  private Long sizeBytes;
}
```

### 5.4 DTO

**dto/PresignReq.java**

```java
package com.example.miniodemo.dto;

import jakarta.validation.constraints.*;

public record PresignReq(
    @NotBlank String filename,
    @NotBlank String contentType,
    @Positive long size
) {}
```

**dto/PresignResp.java**

```java
package com.example.miniodemo.dto;

import java.util.Map;

public record PresignResp(String id, String uploadUrl, Map<String, String> requiredHeaders) {}
```

### 5.5 MyBatis Mapper（注解版）

**mapper/ImageMapper.java**

```java
package com.example.miniodemo.mapper;

import com.example.miniodemo.entity.Image;
import com.example.miniodemo.entity.ImageVariant;
import org.apache.ibatis.annotations.*;

@Mapper
public interface ImageMapper {
  @Insert("""
  INSERT INTO images(id, owner_id, original_name, content_type, storage_key, size_bytes, status)
  VALUES (#{id}, #{ownerId}, #{originalName}, #{contentType}, #{storageKey}, #{sizeBytes}, #{status})
  """)
  int insert(Image r);

  @Update("""
  UPDATE images SET status=#{status}, sha256_hex=#{sha256Hex}, width=#{width}, height=#{height}, updated_at=CURRENT_TIMESTAMP
  WHERE id=#{id}
  """)
  int updateAfterProcess(Image r);

  @Select("SELECT * FROM images WHERE id=#{id}")
  Image findById(long id);

  @Insert("""
  INSERT INTO image_variants(id, image_id, variant, width, height, format, storage_key, size_bytes)
  VALUES (#{id}, #{imageId}, #{variant}, #{width}, #{height}, #{format}, #{storageKey}, #{sizeBytes})
  """)
  int insertVariant(ImageVariant v);
}
```

### 5.6 工具类（ULID→Long 简化）

**util/Ids.java**

```java
package com.example.miniodemo.util;

import com.github.f4b6a3.ulid.UlidCreator;

public class Ids {
  public static long newId() {
    // 取 ULID 的前 8 字节转 long（演示用；生产可以直接用字符串主键）
    var ulid = UlidCreator.getUlid().toBytes();
    long v = 0;
    for (int i = 0; i < 8; i++) v = (v << 8) | (ulid[i] & 0xff);
    return v & 0x7fffffffffffffffL; // 保证正数
  }
}
```

### 5.7 上传服务（预签名 PUT）

**service/UploadService.java**

```java
package com.example.miniodemo.service;

import com.example.miniodemo.dto.PresignReq;
import com.example.miniodemo.dto.PresignResp;
import com.example.miniodemo.entity.Image;
import com.example.miniodemo.mapper.ImageMapper;
import com.example.miniodemo.util.Ids;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UploadService {
  private final S3Client s3;
  private final S3Presigner presigner;
  private final ImageMapper imageMapper;

  @Value("${app.bucket}")
  private String bucket;

  public PresignResp createPresignedPut(long ownerId, PresignReq req) {
    long id = Ids.newId();
    String key = "images/%d/original".formatted(id);

    // 确保 Bucket 存在（幂等）
    try { s3.createBucket(CreateBucketRequest.builder().bucket(bucket).build()); } catch (S3Exception ignored) {}

    Image record = new Image();
    record.setId(id);
    record.setOwnerId(ownerId);
    record.setOriginalName(req.filename());
    record.setContentType(req.contentType());
    record.setStorageKey(key);
    record.setSizeBytes(req.size());
    record.setStatus("UPLOADING");
    imageMapper.insert(record);

    PutObjectRequest put = PutObjectRequest.builder()
        .bucket(bucket).key(key)
        .contentType(req.contentType())
        .build();

    PutObjectPresignRequest preq = PutObjectPresignRequest.builder()
        .signatureDuration(Duration.ofMinutes(15))
        .putObjectRequest(put)
        .build();

    PresignedPutObjectRequest p = presigner.presignPutObject(preq);

    // 浏览器上传时至少带上 Content-Type，其他 header 由 SDK 计算
    return new PresignResp(String.valueOf(id), p.url().toString(), Map.of("Content-Type", req.contentType()));
  }
}
```

### 5.8 处理服务（校验 + 生成变体）

**service/ProcessingService.java**

```java
package com.example.miniodemo.service;

import com.example.miniodemo.entity.Image;
import com.example.miniodemo.entity.ImageVariant;
import com.example.miniodemo.mapper.ImageMapper;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

@Service
@RequiredArgsConstructor
public class ProcessingService {
  private final S3Client s3;
  private final ImageMapper imageMapper;
  private final Tika tika = new Tika();
  @Value("${app.bucket}") String bucket;

  @Async
  public void verifyAndProcess(long id) {
    Image img = imageMapper.findById(id);
    if (img == null) return;

    try (ResponseInputStream<?> in = s3.getObject(GetObjectRequest.builder()
        .bucket(bucket).key(img.getStorageKey()).build())) {

      // 1) MIME 嗅探
      String mime = tika.detect(in);
      if (mime == null || !mime.startsWith("image/")) throw new IllegalArgumentException("Not an image");

      // 需要重新获取流（上面 detect 消耗了流），再读入 BufferedImage
    }

    try (ResponseInputStream<?> in2 = s3.getObject(GetObjectRequest.builder()
        .bucket(bucket).key(img.getStorageKey()).build())) {
      BufferedImage bi = ImageIO.read(in2);
      int w = bi.getWidth(), h = bi.getHeight();

      // 2) 生成 1024 宽 WebP 变体（需要 TwelveMonkeys 插件）
      var resized = new java.io.ByteArrayOutputStream();
      // 用 Thumbnailator 简化缩放
      net.coobird.thumbnailator.Thumbnails.of(bi)
          .size(1024, 1024)
          .outputFormat("webp")
          .toOutputStream(resized);
      byte[] bytes = resized.toByteArray();
      String vKey = img.getStorageKey().replace("/original", "/w1024.webp");

      s3.putObject(PutObjectRequest.builder()
              .bucket(bucket).key(vKey)
              .contentType("image/webp").build(),
          RequestBody.fromBytes(bytes));

      // 3) 更新 DB
      img.setStatus("READY");
      img.setWidth(w); img.setHeight(h);
      imageMapper.updateAfterProcess(img);

      ImageVariant v = new ImageVariant();
      v.setId(System.nanoTime());
      v.setImageId(id);
      v.setVariant("w1024");
      v.setFormat("webp");
      v.setStorageKey(vKey);
      v.setSizeBytes((long) bytes.length);
      imageMapper.insertVariant(v);
    } catch (Exception e) {
      Image fail = new Image();
      fail.setId(id);
      fail.setStatus("FAILED");
      imageMapper.updateAfterProcess(fail);
      throw new RuntimeException(e);
    }
  }
}
```

### 5.9 控制器（预签名 & 完成回调 & 下载）

**controller/UploadController.java**

```java
package com.example.miniodemo.controller;

import com.example.miniodemo.dto.PresignReq;
import com.example.miniodemo.dto.PresignResp;
import com.example.miniodemo.service.UploadService;
import com.example.miniodemo.service.ProcessingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class UploadController {
  private final UploadService uploadService;
  private final ProcessingService processingService;

  // 模拟登录用户：演示用固定 ownerId=1
  @PostMapping("/uploads/presign")
  public PresignResp presign(@Valid @RequestBody PresignReq req) {
    return uploadService.createPresignedPut(1L, req);
  }

  @PostMapping("/uploads/{id}/complete")
  public void complete(@PathVariable long id) {
    processingService.verifyAndProcess(id);
  }
}
```

**controller/DownloadController.java**

```java
package com.example.miniodemo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URI;
import java.time.Duration;

@RestController
@RequiredArgsConstructor
public class DownloadController {
  private final S3Presigner presigner;
  @Value("${app.bucket}") String bucket;

  // 简化演示：根据 id 推导 key；生产建议从 DB 查询具体变体
  @GetMapping("/images/{id}/download")
  public ResponseEntity<Void> download(@PathVariable long id, @RequestParam(defaultValue = "original") String variant) {
    String key = "images/%d/%s".formatted(id, variant.equals("original") ? "original" : "w1024.webp");

    GetObjectRequest get = GetObjectRequest.builder()
        .bucket(bucket).key(key)
        .responseContentDisposition("inline")
        .build();
    GetObjectPresignRequest req = GetObjectPresignRequest.builder()
        .signatureDuration(Duration.ofMinutes(5))
        .getObjectRequest(get).build();
    PresignedGetObjectRequest url = presigner.presignGetObject(req);

    return ResponseEntity.status(302).location(URI.create(url.url().toString())).build();
  }
}
```

---

## 6) 前端/调用示例

### 6.1 原生 HTML + JS（可直接开一个静态页）

```html
<!doctype html>
<html>
  <body>
    <input type="file" id="file" />
    <button id="btn">Upload</button>
    <script>
      document.getElementById('btn').onclick = async () => {
        const f = document.getElementById('file').files[0];
        if (!f) return;

        // 1) 向后端申请预签名
        const presign = await fetch('http://localhost:8080/uploads/presign', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ filename: f.name, contentType: f.type || 'application/octet-stream', size: f.size })
        }).then(r => r.json());

        // 2) 直传 MinIO（务必带上签名时使用的 Content-Type）
        await fetch(presign.uploadUrl, {
          method: 'PUT',
          headers: presign.requiredHeaders, // 通常只需 Content-Type
          body: f
        });

        // 3) 通知后端处理
        await fetch(`http://localhost:8080/uploads/${presign.id}/complete`, { method: 'POST' });
        alert('Uploaded! id=' + presign.id);
      };
    </script>
  </body>
</html>
```

### 6.2 cURL 测试

```bash
# 1) 预签名
curl -s http://localhost:8080/uploads/presign \
  -H 'Content-Type: application/json' \
  -d '{"filename":"cat.jpg","contentType":"image/jpeg","size":12345}' | jq .
# => { id, uploadUrl, requiredHeaders }

# 2) 直传（确保 -H Content-Type 与上面一致）
curl -X PUT "<uploadUrl>" -H 'Content-Type: image/jpeg' --data-binary @cat.jpg

# 3) 完成回调（触发异步处理）
curl -X POST http://localhost:8080/uploads/<id>/complete

# 4) 下载原图或变体（302 到 MinIO）
curl -I "http://localhost:8080/images/<id>/download?variant=original"
curl -I "http://localhost:8080/images/<id>/download?variant=w1024.webp"
```

---

## 7) 关键注意事项（生产化）

* **CORS**：仅允许你的前端域；`AllowedMethod` 限制为 `PUT,GET,HEAD` 即可。
* **鉴权**：本 Demo 未做用户认证；生产应校验上传者身份，并将 `owner_id` 落库。
* **内容校验**：用 Tika/魔数检测 + 重新编码去 EXIF；超大图限制总像素，防止解码攻击。
* **ETag/哈希**：可取 MinIO ETag 或自行计算 SHA-256 做去重。
* **多段上传**：大文件建议改为 S3 Multipart Upload（预签每个 part）。
* **CDN**：生产用 CDN 回源 MinIO；后端返回 302 到 **短期签名**下载地址即可。
* **路径风格**：MinIO 默认 path-style；若用虚拟主机风格需配置 DNS。

---

## 8) 可切换到 MySQL/PG 的配置（可选）

将 `spring.datasource` 改成实际数据库连接；`schema.sql` 在启动时会初始化表结构，也可手动迁移。

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/minio_demo?useSSL=false&serverTimezone=UTC
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
```

并在 `pom.xml` 添加：

```xml
<dependency>
  <groupId>mysql</groupId>
  <artifactId>mysql-connector-j</artifactId>
  <scope>runtime</scope>
</dependency>
```

---

**到这里，你就可以：**

1. `docker compose` 启动 MinIO 并配置 CORS
2. `mvn spring-boot:run` 启动后端
3. 用上面的 HTML 或 cURL 直传 -> 完成回调 -> 下载变体（`w1024.webp`）

需要我再补一个基于 Multipart Upload 的版本（支持断点与大文件）或接入 CDN 的最佳实践模板吗？
http://localhost:8080/images/115391413070630775/download?variant=w1024.webp
http://localhost:8080/images/115391413070630775/download
