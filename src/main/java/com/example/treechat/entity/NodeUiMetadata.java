package com.example.treechat.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 节点UI元数据实体
 */
@Data
@TableName("node_ui_metadata")
public class NodeUiMetadata {

    /**
     * 节点ID（主键）
     */
    private String nodeId;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * X坐标
     */
    private BigDecimal positionX;

    /**
     * Y坐标
     */
    private BigDecimal positionY;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
