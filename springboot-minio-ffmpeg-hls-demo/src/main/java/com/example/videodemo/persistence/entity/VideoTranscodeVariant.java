package com.example.videodemo.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.*;

@TableName("video_transcode_variant")
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoTranscodeVariant extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long jobId;
    private Long videoId;
    private Integer variantLevel;
    private String resolution;
    private Integer bitrateKbps;
    private String playlistPath;
    private String segmentPathPrefix;
    private Integer durationSeconds;
    private String checksum;
}
