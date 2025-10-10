package com.example.treechat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.treechat.dto.node.NodeResponse;
import com.example.treechat.dto.project.ProjectDetailResponse;
import com.example.treechat.dto.project.UpdateProjectUiStateRequest;
import com.example.treechat.entity.Node;
import com.example.treechat.entity.NodeUiMetadata;
import com.example.treechat.entity.Project;
import com.example.treechat.entity.ProjectUiMetadata;
import com.example.treechat.exception.ResourceNotFoundException;
import com.example.treechat.mapper.NodeMapper;
import com.example.treechat.mapper.NodeUiMetadataMapper;
import com.example.treechat.mapper.ProjectMapper;
import com.example.treechat.mapper.ProjectUiMetadataMapper;
import com.example.treechat.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 项目服务
 */
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectMapper projectMapper;
    private final NodeMapper nodeMapper;
    private final ProjectUiMetadataMapper projectUiMetadataMapper;
    private final NodeUiMetadataMapper nodeUiMetadataMapper;

    /**
     * 获取项目详情（包含所有节点）
     */
    public ProjectDetailResponse getProjectDetail(String projectId) {
        String currentUserId = SecurityUtil.getCurrentUserId();

        // 查询项目
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new ResourceNotFoundException("项目不存在: " + projectId);
        }

        // 权限校验
        if (!project.getUserId().equals(currentUserId)) {
            throw new AccessDeniedException("无权访问该项目");
        }

        // 查询项目下所有节点
        List<Node> nodes = nodeMapper.selectList(
                new LambdaQueryWrapper<Node>().eq(Node::getProjectId, projectId)
        );

        // 查询所有节点的UI元数据
        List<String> nodeIds = nodes.stream().map(Node::getId).collect(Collectors.toList());
        Map<String, NodeUiMetadata> uiMetadataMap = null;
        if (!nodeIds.isEmpty()) {
            List<NodeUiMetadata> uiMetadataList = nodeUiMetadataMapper.selectList(
                    new LambdaQueryWrapper<NodeUiMetadata>().in(NodeUiMetadata::getNodeId, nodeIds)
            );
            uiMetadataMap = uiMetadataList.stream()
                    .collect(Collectors.toMap(NodeUiMetadata::getNodeId, m -> m));
        }

        // 转换为响应DTO
        Map<String, NodeUiMetadata> finalUiMetadataMap = uiMetadataMap;
        List<NodeResponse> nodeResponses = nodes.stream().map(node -> {
            NodeResponse response = new NodeResponse();
            response.setId(node.getId());
            response.setProjectId(node.getProjectId());
            response.setParentId(node.getParentId());
            response.setTitle(node.getTitle());
            response.setFinalConclusion(node.getFinalConclusion());
            response.setAiConclusionDraft(node.getAiConclusionDraft());
            response.setConclusionState(node.getConclusionState());
            response.setAiConclusionConfidence(node.getAiConclusionConfidence());
            response.setSiblingOrder(node.getSiblingOrder());
            response.setCreatedAt(node.getCreatedAt());
            response.setUpdatedAt(node.getUpdatedAt());

            // 设置UI元数据
            if (finalUiMetadataMap != null && finalUiMetadataMap.containsKey(node.getId())) {
                NodeUiMetadata uiMetadata = finalUiMetadataMap.get(node.getId());
                NodeResponse.UiMetadata uiMeta = new NodeResponse.UiMetadata();
                uiMeta.setPositionX(uiMetadata.getPositionX());
                uiMeta.setPositionY(uiMetadata.getPositionY());
                response.setUiMetadata(uiMeta);
            }

            return response;
        }).collect(Collectors.toList());

        // 查询项目UI元数据
        ProjectUiMetadata projectUiMetadata = projectUiMetadataMapper.selectById(projectId);

        // 构建响应
        ProjectDetailResponse response = new ProjectDetailResponse();
        response.setId(project.getId());
        response.setTitle(project.getTitle());
        response.setCreatedAt(project.getCreatedAt());
        response.setUpdatedAt(project.getUpdatedAt());
        response.setNodes(nodeResponses);

        if (projectUiMetadata != null) {
            ProjectDetailResponse.UiMetadata uiMeta = new ProjectDetailResponse.UiMetadata();
            uiMeta.setZoom(projectUiMetadata.getZoom());
            uiMeta.setPanX(projectUiMetadata.getPanX());
            uiMeta.setPanY(projectUiMetadata.getPanY());
            response.setUiMetadata(uiMeta);
        } else {
            // 默认值
            ProjectDetailResponse.UiMetadata uiMeta = new ProjectDetailResponse.UiMetadata();
            uiMeta.setZoom(BigDecimal.ONE);
            uiMeta.setPanX(BigDecimal.ZERO);
            uiMeta.setPanY(BigDecimal.ZERO);
            response.setUiMetadata(uiMeta);
        }

        return response;
    }

    /**
     * 更新项目UI状态
     */
    @Transactional
    public ProjectDetailResponse.UiMetadata updateProjectUiState(String projectId,
                                                                  UpdateProjectUiStateRequest request) {
        String currentUserId = SecurityUtil.getCurrentUserId();

        // 查询项目
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new ResourceNotFoundException("项目不存在: " + projectId);
        }

        // 权限校验
        if (!project.getUserId().equals(currentUserId)) {
            throw new AccessDeniedException("无权访问该项目");
        }

        // 更新或插入UI元数据
        ProjectUiMetadata uiMetadata = projectUiMetadataMapper.selectById(projectId);
        if (uiMetadata == null) {
            uiMetadata = new ProjectUiMetadata();
            uiMetadata.setProjectId(projectId);
            uiMetadata.setUserId(currentUserId);
            uiMetadata.setZoom(request.getZoom());
            uiMetadata.setPanX(request.getPanX());
            uiMetadata.setPanY(request.getPanY());
            projectUiMetadataMapper.insert(uiMetadata);
        } else {
            uiMetadata.setZoom(request.getZoom());
            uiMetadata.setPanX(request.getPanX());
            uiMetadata.setPanY(request.getPanY());
            projectUiMetadataMapper.updateById(uiMetadata);
        }

        // 返回更新后的UI状态
        ProjectDetailResponse.UiMetadata response = new ProjectDetailResponse.UiMetadata();
        response.setZoom(uiMetadata.getZoom());
        response.setPanX(uiMetadata.getPanX());
        response.setPanY(uiMetadata.getPanY());

        return response;
    }
}
