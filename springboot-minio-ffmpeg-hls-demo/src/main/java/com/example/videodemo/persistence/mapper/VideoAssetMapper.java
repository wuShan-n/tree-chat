package com.example.videodemo.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.videodemo.persistence.entity.VideoAsset;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface VideoAssetMapper extends BaseMapper<VideoAsset> {
}
