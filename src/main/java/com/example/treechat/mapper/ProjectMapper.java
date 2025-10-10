package com.example.treechat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.treechat.entity.Project;
import org.apache.ibatis.annotations.Mapper;

/**
 * 项目 Mapper
 */
@Mapper
public interface ProjectMapper extends BaseMapper<Project> {
}
