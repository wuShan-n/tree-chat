package com.example.treechat.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 项目UI元数据实体
 */
@Data
@TableName("project_ui_metadata")
public class ProjectUiMetadata {

    /**
     * 项目ID（主键）
     */
    private String projectId;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 缩放比例
     */
    private BigDecimal zoom;

    /**
     * 平移X坐标
     */
    private BigDecimal panX;

    /**
     * 平移Y坐标
     */
    private BigDecimal panY;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
