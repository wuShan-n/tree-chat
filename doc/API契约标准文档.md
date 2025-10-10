最终版API契约标准文档 (v1.1.0)。

YAML

```
openapi: 3.0.3
info:
  title: "深度思考与知识生成工具 API"
  description: "为深度思考工具提供后端支持的API，遵循响应驱动和强一致性原则。"
  version: "1.1.0" # 版本号提升
servers:
  - url: "/api"
    description: "API 根路径"

# ------------------------------------------------------------
# 1. 安全性定义
# ------------------------------------------------------------
components:
  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT

security:
  - bearerAuth: []

# ------------------------------------------------------------
# 2. API 路由与端点定义 (Paths)
# ------------------------------------------------------------
paths:
  # ------------------------------------
  # 项目 (Project)
  # ------------------------------------
  /projects/{projectId}:
    get:
      tags: [ "Project" ]
      summary: "获取项目及其所有节点"
      description: "获取指定项目的完整信息，包括其下所有节点的层级结构化数据。"
      parameters:
        - name: projectId
          in: path
          required: true
          schema:
            type: string
            format: uuid
      responses:
        '200':
          description: "成功获取项目数据"
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ProjectDetail'
        '403':
          $ref: '#/components/responses/Forbidden'
        '404':
          $ref: '#/components/responses/NotFound'

  /projects/{projectId}/uistate:
    put:
      tags: [ "Project" ]
      summary: "保存画布的UI状态"
      description: "持久化存储画布的缩放、平移等UI状态。"
      parameters:
        - name: projectId
          in: path
          required: true
          schema:
            type: string
            format: uuid
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ProjectUIState'
      responses:
        '200':
          description: "UI状态更新成功"
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ProjectUIState' # 遵循响应驱动原则
        '400':
          $ref: '#/components/responses/BadRequest'
        '403':
          $ref: '#/components/responses/Forbidden'

  # ------------------------------------
  # 节点 (Node)
  # ------------------------------------
  /nodes:
    post:
      tags: [ "Node" ]
      summary: "创建新节点"
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                project_id:
                  type: string
                  format: uuid
                parent_id:
                  type: string
                  format: uuid
                title:
                  type: string
                  example: "新节点"
                ui_metadata:
                  type: object
                  description: "可选的初始UI位置信息"
                  properties:
                    position_x:
                      type: number
                      description: "建议的X坐标"
                    position_y:
                      type: number
                      description: "建议的Y坐标"
      responses:
        '201':
          description: "节点创建成功"
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Node' # 遵循响应驱动原则

  /nodes/search: # <--- [新增] 问题一的解决方案
    get:
      tags: [ "Node" ]
      summary: "搜索节点"
      description: "根据查询关键字，实时搜索节点标题和结论，用于支持 [[...]] 引用功能。"
      parameters:
        - name: q
          in: query
          required: true
          schema:
            type: string
            description: "搜索关键字"
      responses:
        '200':
          description: "成功返回匹配的节点列表"
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/NodeSearchResult'

  /nodes/positions:
    patch:
      tags: [ "Node" ]
      summary: "批量更新节点UI坐标"
      description: "一次性更新多个节点的XY坐标，用于支持前端的防抖批量保存策略，避免N+1请求。"
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: array
              items:
                type: object
                properties:
                  id:
                    type: string
                    format: uuid
                  ui_metadata:
                    type: object
                    properties:
                      position_x:
                        type: number
                      position_y:
                        type: number
      responses:
        '200':
          description: "批量更新成功"
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/Node' # <--- [修订] 问题三的解决方案
        '400':
          $ref: '#/components/responses/BadRequest'

  /nodes/{id}:
    put:
      tags: [ "Node" ]
      summary: "更新节点基础信息"
      description: "主要用于更新节点标题等核心业务字段。"
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                title:
                  type: string
      responses:
        '200':
          description: "更新成功"
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Node' # 遵循响应驱动原则
    delete:
      tags: [ "Node" ]
      summary: "删除节点"
      description: "删除一个节点及其所有子孙节点。"
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
      responses:
        '204':
          description: "删除成功，无返回内容"

  /nodes/{id}/structure:
    put:
      tags: [ "Node" ]
      summary: "移动节点或排序"
      description: "修改节点的父节点或其在同级中的顺序。后端根据邻居ID负责计算和维护sibling_order。"
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                parent_id:
                  type: string
                  format: uuid
                prev_sibling_id:
                  type: string
                  format: uuid
                  nullable: true
                next_sibling_id:
                  type: string
                  format: uuid
                  nullable: true
      responses:
        '200':
          description: "结构更新成功"
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Node' # 遵循响应驱动原则
        '400':
          description: "请求无效（例如，出现循环引用）"
          content:
            application/json:
              schema:
                allOf:
                  - $ref: '#/components/schemas/ApiError'
                  - type: object
                    properties:
                      errorCode:
                        type: string
                        example: "CIRCULAR_REFERENCE_DETECTED"
                      message:
                        type: string
                        example: "不能将节点移动到其子孙节点下"

  /nodes/{id}/backlinks: # <--- [新增] 问题一的解决方案
    get:
      tags: [ "Node" ]
      summary: "获取节点的反向链接"
      description: "获取所有引用了当前节点的其他节点列表。"
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
      responses:
        '200':
          description: "成功返回反向链接列表"
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/NodeSearchResult'

  /nodes/{id}/chat:
    post:
      tags: [ "Node" ]
      summary: "与节点AI对话"
      description: "向指定节点发送消息并发起AI对话。支持 application/json (常规) 和 text/event-stream (流式) 响应。"
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                content:
                  type: string
                  example: "请深入分析这个观点，并参考 [[另一个观点]]"
                references: # <--- [新增] 问题一的解决方案
                  type: array
                  items:
                    type: string
                    format: uuid
                  description: "消息内容中引用的目标节点ID列表"
      responses:
        '200':
          description: "对话成功"
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Message'
            text/event-stream:
              schema:
                type: string
                description: "流式响应的文本块"

  /nodes/{id}/conclusion/finalize:
    post:
      tags: [ "Node" ]
      summary: "用户终审并保存结论"
      description: "用户确认或修改AI草稿后，将最终结论固化。"
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                conclusion:
                  type: string
                references: # <--- [新增] 问题一的解决方案
                  type: array
                  items:
                    type: string
                    format: uuid
                  description: "结论内容中引用的目标节点ID列表"
      responses:
        '200':
          description: "结论保存成功"
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Node' # 返回更新后的完整Node

  # ------------------------------------
  # 用户 (User/Me)
  # ------------------------------------
  /me/settings:
    get:
      tags: [ "User" ]
      summary: "获取当前用户设置"
      responses:
        '200':
          description: "成功"
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/UserSettings'
    put:
      tags: [ "User" ]
      summary: "更新用户设置"
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/UserSettings'
      responses:
        '200':
          description: "成功"
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/UserSettings'

  /me/settings/events:
    post:
      tags: [ "User" ]
      summary: "追踪用户关键行为事件"
      description: "用于“自适应引导”，例如追踪用户无修改采纳结论的行为。"
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                event_type:
                  type: string
                  enum: [ "direct_conclusion_adoption" ]
      responses:
        '202':
          description: "事件已接受处理"

  /me/credits:
    get:
      tags: [ "User" ]
      summary: "获取用户点数余额"
      responses:
        '200':
          description: "成功"
          content:
            application/json:
              schema:
                type: object
                properties:
                  balance:
                    type: number
                    format: decimal

  # ------------------------------------
  # AI高级功能 (AI Insights)
  # ------------------------------------
  /insights/generate:
    post:
      tags: [ "AI Insights" ]
      summary: "启动“全局洞察”异步任务"
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                node_ids:
                  type: array
                  items:
                    type: string
                    format: uuid
                lens:
                  type: string
                  enum: ["CORE_CONFLICT", "INNOVATION_OPPORTUNITY"]
      responses:
        '202':
          description: "任务已接受，开始异步处理"
          content:
            application/json:
              schema:
                type: object
                properties:
                  task_id:
                    type: string
                    format: uuid

  /insights/tasks/{taskId}: # <--- [修订] 问题二的解决方案 (路径重命名)
    get:
      tags: [ "AI Insights" ]
      summary: "获取异步任务状态和结果" # <--- 名称更新
      parameters:
        - name: taskId
          in: path
          required: true
          schema:
            type: string
            format: uuid
      responses:
        '200':
          description: "获取到任务状态"
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/InsightTask'
    post: # <--- [新增] 问题二的解决方案
      tags: [ "AI Insights" ]
      summary: "与“全局洞察”的AI教练对话"
      description: "在全局洞察任务完成后，与其背后的苏格拉底式AI教练进行多轮对话。"
      parameters:
        - name: taskId
          in: path
          required: true
          schema:
            type: string
            format: uuid
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                content:
                  type: string
                  example: "你提到的第三个创新点很有趣，能详细展开吗？"
      responses:
        '200':
          description: "AI教练响应成功"
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Message' # 可以复用Message模型

# ------------------------------------------------------------
# 3. 可复用的数据模型 (Schemas)
# ------------------------------------------------------------
components:
  schemas:
    Project:
      type: object
      properties:
        id:
          type: string
          format: uuid
        title:
          type: string
        created_at:
          type: string
          format: date-time
        updated_at:
          type: string
          format: date-time

    ProjectDetail:
      allOf:
        - $ref: '#/components/schemas/Project'
        - type: object
          properties:
            nodes:
              type: array
              items:
                $ref: '#/components/schemas/Node' # 假设返回扁平列表，由前端构建树
            ui_metadata:
              $ref: '#/components/schemas/ProjectUIState'

    Node:
      type: object
      properties:
        id:
          type: string
          format: uuid
        project_id:
          type: string
          format: uuid
        parent_id:
          type: string
          format: uuid
          nullable: true
        title:
          type: string
        final_conclusion:
          type: string
          nullable: true
        ai_conclusion_draft:
          type: string
          nullable: true
        conclusion_state:
          type: string
          enum: [ DRAFT, FINALIZED ]
        ai_conclusion_confidence:
          type: string
          enum: [ high, medium, low ]
        sibling_order:
          type: integer
        ui_metadata: # 将UI元数据内联
          $ref: '#/components/schemas/NodeUIMetadata'
        created_at:
          type: string
          format: date-time
        updated_at:
          type: string
          format: date-time

    NodeUIMetadata:
      type: object
      properties:
        position_x:
          type: number
        position_y:
          type: number

    NodeSearchResult: # <--- [新增] 问题一的解决方案
      type: object
      description: "用于节点搜索和反向链接的轻量级节点信息"
      properties:
        id:
          type: string
          format: uuid
        title:
          type: string
        conclusion_summary:
          type: string
          description: "节点最终结论的摘要"
          nullable: true

    ProjectUIState:
      type: object
      properties:
        zoom:
          type: number
        pan_x:
          type: number
        pan_y:
          type: number

    Message:
      type: object
      properties:
        id:
          type: string
          format: uuid
        node_id:
          type: string
          format: uuid
        role:
          type: string
          enum: [ user, ai ]
        content:
          type: string
        created_at:
          type: string
          format: date-time

    UserSettings:
      type: object
      properties:
        # 定义具体的用户设置项
        enable_one_click_adoption:
          type: boolean
          default: false
        # tracking_data 在后端处理，不直接暴露

    InsightTask:
      type: object
      properties:
        id:
          type: string
          format: uuid
        status:
          type: string
          enum: [ PENDING, PROCESSING, COMPLETED, FAILED ]
        result:
          type: string
          nullable: true
        error_message:
          type: string
          nullable: true
        created_at:
          type: string
          format: date-time

    ApiError:
      type: object
      properties:
        timestamp:
          type: string
          format: date-time
        status:
          type: integer
          example: 404
        error:
          type: string
          example: "Not Found"
        message:
          type: string
          example: "项目 'abc-123' 未找到"
        path:
          type: string
          example: "/api/projects/abc-123"
        errorCode:
          type: string
          nullable: true
          description: "机器可读的错误代码，用于前端针对性处理"
          example: "CIRCULAR_REFERENCE_DETECTED"

# ------------------------------------------------------------
# 4. 可复用的响应体 (Responses)
# ------------------------------------------------------------
components:
  responses:
    BadRequest:
      description: "无效的请求"
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/ApiError'
    Unauthorized:
      description: "需要认证"
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/ApiError'
    Forbidden:
      description: "无权访问该资源"
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/ApiError'
    NotFound:
      description: "资源未找到"
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/ApiError'
```