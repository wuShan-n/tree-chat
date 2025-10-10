package com.example.treechat.dto.project;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 更新项目UI状态请求DTO
 */
@Data
public class UpdateProjectUiStateRequest {

    @NotNull(message = "缩放比例不能为空")
    private BigDecimal zoom;

    @NotNull(message = "panX不能为空")
    private BigDecimal panX;

    @NotNull(message = "panY不能为空")
    private BigDecimal panY;
}
