package com.example.treechat.controller;

import com.example.treechat.dto.project.ProjectDetailResponse;
import com.example.treechat.dto.project.UpdateProjectUiStateRequest;
import com.example.treechat.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 项目控制器
 */
@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
@Tag(name = "Project", description = "项目管理相关接口")
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping("/{projectId}")
    @Operation(summary = "获取项目详情", description = "获取项目及其所有节点的层级结构化数据")
    public ResponseEntity<ProjectDetailResponse> getProjectDetail(@PathVariable String projectId) {
        ProjectDetailResponse response = projectService.getProjectDetail(projectId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{projectId}/uistate")
    @Operation(summary = "保存画布UI状态", description = "持久化存储画布的缩放、平移等UI状态")
    public ResponseEntity<ProjectDetailResponse.UiMetadata> updateProjectUiState(
            @PathVariable String projectId,
            @Valid @RequestBody UpdateProjectUiStateRequest request) {
        ProjectDetailResponse.UiMetadata response = projectService.updateProjectUiState(projectId, request);
        return ResponseEntity.ok(response);
    }
}
