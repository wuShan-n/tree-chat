package com.example.treechat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.treechat.entity.Node;
import org.apache.ibatis.annotations.Mapper;

/**
 * 节点 Mapper
 */
@Mapper
public interface NodeMapper extends BaseMapper<Node> {
}
