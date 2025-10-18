package com.example.miniodemo.mapper;

import com.example.miniodemo.entity.Image;
import com.example.miniodemo.entity.ImageVariant;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ImageMapper {
    @Insert("""
            INSERT INTO images(id, owner_id, original_name, content_type, storage_key, size_bytes, status)
            VALUES (#{id}, #{ownerId}, #{originalName}, #{contentType}, #{storageKey}, #{sizeBytes}, #{status})
            """)
    int insert(Image r);

    @Update("""
            UPDATE images SET status=#{status}, sha256_hex=#{sha256Hex}, width=#{width}, height=#{height}, updated_at=CURRENT_TIMESTAMP
            WHERE id=#{id}
            """)
    int updateAfterProcess(Image r);

    @Select("SELECT * FROM images WHERE id=#{id}")
    Image findById(long id);

    @Insert("""
            INSERT INTO image_variants(id, image_id, variant, width, height, format, storage_key, size_bytes)
            VALUES (#{id}, #{imageId}, #{variant}, #{width}, #{height}, #{format}, #{storageKey}, #{sizeBytes})
            """)
    int insertVariant(ImageVariant v);
}