package com.mtd.tt.mapper;

import com.mtd.tt.model.entity.PostEntity;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PostMapper extends BaseMapper<PostEntity> {

    /**
     * 帖子热度递增（避免读后写，单条 UPDATE 累加）。
     */
    @Update("UPDATE posts SET hot_score = hot_score + #{delta} WHERE id = #{postId}")
    int updateHotScore(@Param("postId") Long postId, @Param("delta") long delta);
}
