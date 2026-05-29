package com.aiprreviewer.model.dto;

import com.aiprreviewer.model.enums.ReviewStatus;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 评审结果响应，返回给前端的完整评审数据
 */
@Data
public class ReviewResultDTO {

    /** 任务ID */
    private Long id;

    /** 仓库地址 */
    private String repoUrl;

    /** PR编号 */
    private Integer prNumber;

    /** PR标题 */
    private String prTitle;

    /** 任务状态 */
    private ReviewStatus status;

    /** AI变更总结 */
    private String summary;

    /** 使用的AI模型 */
    private String aiModel;

    /** diff字符数 */
    private Integer diffSize;

    /** 变更文件数 */
    private Integer fileCount;

    /** 失败原因 */
    private String errorMessage;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 评审意见列表 */
    private List<CommentDTO> comments;
}
