package com.example.videodemo.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.*;

@TableName("video_upload_task")
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoUploadTask extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long videoId;
    private String uploadChannel;
    private String uploaderId;
    private String uploadPath;
    private String ingestStrategy;
    private Double progress;
    private String checksum;
    private String validationStatus;
    private String remark;
}
