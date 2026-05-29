package com.aiprreviewer.controller;

import com.aiprreviewer.model.constant.ReviewConstants;
import com.aiprreviewer.model.dto.ApiResponse;
import com.aiprreviewer.model.dto.ReviewCreateRequest;
import com.aiprreviewer.model.dto.ReviewResultDTO;
import com.aiprreviewer.service.ReviewService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * PR 评审 REST 控制器
 * <p>
 * 返回统一 ApiResponse 格式，不包装 ResponseEntity。
 * HTTP 状态码由 Spring 默认处理（成功 200，异常由 GlobalExceptionHandler 控制）。
 */
@Slf4j
@Validated
@RestController
@RequestMapping(ReviewConstants.API_REVIEW_PATH)
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /** 创建评审任务 */
    @PostMapping("/create")
    public ApiResponse<ReviewResultDTO> createReview(@Valid @RequestBody ReviewCreateRequest request) {
        log.info("收到评审请求: repo={} pr={}", request.getRepoUrl(), request.getPrNumber());
        ReviewResultDTO result = reviewService.createReview(request);
        return ApiResponse.success(result);
    }

    /** 查询评审详情 */
    @GetMapping("/{id}")
    public ApiResponse<ReviewResultDTO> getReview(@PathVariable @Min(1) Long id) {
        log.info("查询评审详情: id={}", id);
        ReviewResultDTO result = reviewService.getReviewById(id);
        return ApiResponse.success(result);
    }

    /** 分页查询评审历史 */
    @GetMapping("/list")
    public ApiResponse<List<ReviewResultDTO>> listReviews(
            @RequestParam(defaultValue = "" + ReviewConstants.DEFAULT_PAGE) @Min(1) int page,
            @RequestParam(defaultValue = "" + ReviewConstants.DEFAULT_PAGE_SIZE) @Min(1) int size) {
        log.info("查询评审列表: page={} size={}", page, size);
        List<ReviewResultDTO> results = reviewService.listReviews(page, size);
        return ApiResponse.success(results);
    }
}
