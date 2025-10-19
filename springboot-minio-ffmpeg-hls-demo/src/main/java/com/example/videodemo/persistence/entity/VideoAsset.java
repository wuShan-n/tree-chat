package com.example.videodemo.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

import lombok.*;

@TableName("video_asset")
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoAsset extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String description;
    private String fileName;
    private Long fileSize;
    private Integer durationSeconds;
    private String checksum;
    private String sourceBucket;
    private String sourceObject;
    private String coverImage;
    private String status;
    private String transcodeProfile;
    private LocalDateTime readyAt;
    private String playbackUrl;
}
