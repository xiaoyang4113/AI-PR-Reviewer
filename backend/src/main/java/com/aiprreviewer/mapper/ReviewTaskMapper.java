package com.aiprreviewer.mapper;

import com.aiprreviewer.model.entity.ReviewTask;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 评审任务 Mapper，继承 MyBatis-Plus BaseMapper 获得基础 CRUD 能力
 */
@Mapper
public interface ReviewTaskMapper extends BaseMapper<ReviewTask> {

    /**
     * 查询同一 PR 是否有未完成的任务（只查最近 N 分钟内的，超时任务视为失败）
     */
    @Select("SELECT COUNT(1) FROM review_tasks WHERE repo_url = #{repoUrl} AND pr_number = #{prNumber} AND status IN (#{pending}, #{processing}) AND created_at > DATE_SUB(NOW(), INTERVAL #{timeoutMinutes} MINUTE)")
    int countPendingByRepoAndPr(@Param("repoUrl") String repoUrl,
                                 @Param("prNumber") Integer prNumber,
                                 @Param("pending") String pendingStatus,
                                 @Param("processing") String processingStatus,
                                 @Param("timeoutMinutes") int timeoutMinutes);

    /**
     * 分页查询任务列表，按创建时间倒序
     */
    List<ReviewTask> selectTaskPage(@Param("offset") int offset, @Param("size") int size);

    /**
     * 查询任务总数
     */
    @Select("SELECT COUNT(1) FROM review_tasks")
    long countTasks();
}
