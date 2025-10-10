package com.example.treechat.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.treechat.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 节点实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("nodes")
public class Node extends BaseEntity {

    /**
     * 用户ID
     */
    private String userId;

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
     * 结论状态 (DRAFT, FINALIZED)
     */
    private String conclusionState;

    /**
     * AI结论草稿
     */
    private String aiConclusionDraft;

    /**
     * AI结论置信度 (high, medium, low)
     */
    private String aiConclusionConfidence;

    /**
     * 最终结论
     */
    private String finalConclusion;

    /**
     * 同级排序
     */
    private Integer siblingOrder;
}
