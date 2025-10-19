package com.example.videodemo.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.videodemo.persistence.entity.VideoTranscodeJob;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface VideoTranscodeJobMapper extends BaseMapper<VideoTranscodeJob> {
}
