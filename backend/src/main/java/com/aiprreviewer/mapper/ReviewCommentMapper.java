package com.aiprreviewer.mapper;

import com.aiprreviewer.model.entity.ReviewComment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 评审意见 Mapper，继承 MyBatis-Plus BaseMapper 获得基础 CRUD 能力
 */
@Mapper
public interface ReviewCommentMapper extends BaseMapper<ReviewComment> {

    /**
     * 根据任务ID查询所有评审意见，按风险等级排序（严重优先）
     */
    List<ReviewComment> selectByTaskId(@Param("taskId") Long taskId);

    /**
     * 批量插入评审意见，一条 SQL 完成（避免逐条插入的性能问题）
     */
    int batchInsert(@Param("comments") List<ReviewComment> comments);

    /**
     * 根据任务ID列表批量查询评论，解决 N+1 查询问题
     */
    List<ReviewComment> selectByTaskIds(@Param("taskIds") List<Long> taskIds);
}
