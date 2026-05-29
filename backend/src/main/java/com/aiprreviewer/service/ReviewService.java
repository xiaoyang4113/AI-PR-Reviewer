package com.aiprreviewer.service;

import com.aiprreviewer.model.dto.ReviewCreateRequest;
import com.aiprreviewer.model.dto.ReviewResultDTO;

import java.util.List;

/**
 * 评审业务编排服务接口
 */
public interface ReviewService {

    /**
     * 创建并执行评审任务（同步阻塞，耗时较长）
     *
     * @param request 仓库地址 + PR 编号
     * @return 完整的评审结果
     */
    ReviewResultDTO createReview(ReviewCreateRequest request);

    /**
     * 根据任务ID查询评审详情
     */
    ReviewResultDTO getReviewById(Long taskId);

    /**
     * 分页查询评审历史列表
     */
    List<ReviewResultDTO> listReviews(int page, int size);
}
