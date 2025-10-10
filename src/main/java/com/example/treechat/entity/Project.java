package com.example.treechat.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.treechat.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("projects")
public class Project extends BaseEntity {

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 项目标题
     */
    private String title;
}
