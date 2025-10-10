package com.example.treechat.dto.node;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 节点响应DTO
 */
@Data
public class NodeResponse {

    /**
     * 节点ID
     */
    private String id;

    /**
     * 项目ID
     */
    private String projectId;

    /**
     * 父节点ID
     */
    private String parentId;

    /**
     * 节点标题
     */
    private String title;

    /**
     * 最终结论
     */
    private String finalConclusion;

    /**
     * AI结论草稿
     */
    private String aiConclusionDraft;

    /**
     * 结论状态
     */
    private String conclusionState;

    /**
     * AI结论置信度
     */
    private String aiConclusionConfidence;

    /**
     * 同级排序
     */
    private Integer siblingOrder;

    /**
     * UI元数据
     */
    private UiMetadata uiMetadata;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    @Data
    public static class UiMetadata {
        private BigDecimal positionX;
        private BigDecimal positionY;
    }
}
