package com.example.treechat.dto.project;

import com.example.treechat.dto.node.NodeResponse;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 项目详情响应DTO
 */
@Data
public class ProjectDetailResponse {

    /**
     * 项目ID
     */
    private String id;

    /**
     * 项目标题
     */
    private String title;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 项目下所有节点列表
     */
    private List<NodeResponse> nodes;

    /**
     * UI元数据
     */
    private UiMetadata uiMetadata;

    @Data
    public static class UiMetadata {
        private BigDecimal zoom;
        private BigDecimal panX;
        private BigDecimal panY;
    }
}
