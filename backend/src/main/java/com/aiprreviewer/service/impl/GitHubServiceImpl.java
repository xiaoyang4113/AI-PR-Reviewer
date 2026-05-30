package com.aiprreviewer.service.impl;

import com.aiprreviewer.config.GitHubProperties;
import com.aiprreviewer.model.constant.ReviewConstants;
import com.aiprreviewer.model.dto.GitHubDiffResult;
import com.aiprreviewer.exception.BusinessException;
import com.aiprreviewer.service.CodePlatformService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GitHub 平台实现（策略模式）
 *
 * 核心流程：
 * 1. supports() 判断 URL 是否为 GitHub 域名
 * 2. fetchDiff() 调用 GitHub REST API 获取 PR 数据和 diff 文本
 *
 * 安全措施：
 * - URL 白名单校验，仅允许 github.com
 * - Token 不记录到日志
 * - 每次 API 调用创建独立的 HttpHeaders 实例
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubServiceImpl implements CodePlatformService {

    private final RestTemplate restTemplate;
    private final GitHubProperties gitHubProperties;

    /** 单次允许获取的最大 diff 大小（1MB） */
    private static final long MAX_DIFF_BYTES = 1_048_576L;

    private static final Pattern GITHUB_URL_PATTERN =
            Pattern.compile(ReviewConstants.GITHUB_URL_PATTERN, Pattern.CASE_INSENSITIVE);

    private static final Pattern DIFF_FILE_PATTERN =
            Pattern.compile("^" + ReviewConstants.DIFF_FILE_PREFIX, Pattern.MULTILINE);

    @Override
    public boolean supports(String repoUrl) {
        return repoUrl != null && GITHUB_URL_PATTERN.matcher(repoUrl.trim()).find();
    }

    @Override
    public GitHubDiffResult fetchDiff(String repoUrl, Integer prNumber) {
        if (prNumber == null || prNumber <= 0) {
            throw new BusinessException("PR 编号必须为正整数");
        }

        String[] ownerAndRepo = parseRepoUrl(repoUrl);
        String owner = ownerAndRepo[0];
        String repo = ownerAndRepo[1];

        String prUrl = String.format("%s/repos/%s/%s/pulls/%d",
                gitHubProperties.getApiBase(), owner, repo, prNumber);

        String prTitle = fetchPrTitle(prUrl);
        String diffText = fetchPrDiff(prUrl);
        int fileCount = countChangedFiles(diffText);

        log.info("GitHub PR Diff 获取成功: {}/{} PR:#{} 标题:{} 文件数:{} diff大小:{}字符",
                owner, repo, prNumber, prTitle, fileCount, diffText.length());

        return new GitHubDiffResult(diffText, prTitle, fileCount);
    }

    /** 解析仓库 URL，提取 owner 和 repo */
    private String[] parseRepoUrl(String repoUrl) {
        String normalizedUrl = repoUrl.trim().replaceAll("/$", "");
        Matcher matcher = GITHUB_URL_PATTERN.matcher(normalizedUrl);
        if (!matcher.find()) {
            throw new BusinessException("无效的 GitHub 仓库地址格式，示例: https://github.com/owner/repo");
        }
        String owner = matcher.group(1);
        String repo = matcher.group(2).replace(ReviewConstants.GIT_SUFFIX, "");
        return new String[]{owner, repo};
    }

    /**
     * 创建 HTTP 请求头
     * 设置 User-Agent，如果配置了 Token 则添加 Authorization 头
     *
     * @return 配置好的 HttpHeaders 实例
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(ReviewConstants.HEADER_USER_AGENT, ReviewConstants.USER_AGENT_VALUE);
        if (StringUtils.hasText(gitHubProperties.getToken())) {
            headers.set(ReviewConstants.HEADER_AUTHORIZATION,
                    ReviewConstants.BEARER_PREFIX + gitHubProperties.getToken());
        }
        return headers;
    }

    /**
     * 调用 GitHub REST API 获取 PR 标题
     * 使用 application/json 格式请求，从响应中提取 title 字段
     *
     * @param url GitHub PR API URL
     * @return PR 标题，获取失败时返回默认值 "Untitled PR"
     * @throws BusinessException PR 不存在（404）、API 限流（403）或网络异常时抛出
     */
    private String fetchPrTitle(String url) {
        try {
            HttpHeaders headers = createHeaders();
            headers.set(ReviewConstants.HEADER_ACCEPT, MediaType.APPLICATION_JSON_VALUE);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, JsonNode.class);
            if (response.getBody() != null && response.getBody().has(ReviewConstants.PR_TITLE_FIELD)) {
                return response.getBody().get(ReviewConstants.PR_TITLE_FIELD)
                        .asText(ReviewConstants.DEFAULT_PR_TITLE);
            }
            return ReviewConstants.DEFAULT_PR_TITLE;
        } catch (HttpClientErrorException.NotFound e) {
            throw new BusinessException("PR 不存在或仓库为私有仓库，请检查链接是否正确", e);
        } catch (HttpClientErrorException.Forbidden e) {
            throw new BusinessException("GitHub API 访问被拒绝，可能是 API 限流，建议配置 GITHUB_TOKEN", e);
        } catch (Exception e) {
            log.error("获取 PR 标题失败: {}", e.getMessage(), e);
            throw new BusinessException("获取 PR 信息失败: " + e.getMessage(), e);
        }
    }

    /**
     * 调用 GitHub REST API 获取 PR 的 diff 文本
     * 使用 application/vnd.github.v3.diff 格式，直接获取原始 diff
     * 包含大小检查，超过 1MB 时拒绝分析
     *
     * @param url GitHub PR API URL
     * @return git diff 原始文本
     * @throws BusinessException PR 不存在、无变更、diff 过大、API 限流或网络异常时抛出
     */
    private String fetchPrDiff(String url) {
        try {
            HttpHeaders headers = createHeaders();
            headers.set(ReviewConstants.HEADER_ACCEPT, ReviewConstants.GITHUB_DIFF_ACCEPT);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);
            String diffText = response.getBody();
            if (!StringUtils.hasText(diffText)) {
                throw new BusinessException("该 PR 没有代码变更（可能是空提交或已合并）");
            }
            if (diffText.getBytes(StandardCharsets.UTF_8).length > MAX_DIFF_BYTES) {
                throw new BusinessException("PR 变更内容过大（超过 1MB），暂不支持分析超大 PR");
            }
            return diffText;
        } catch (HttpClientErrorException.NotFound e) {
            throw new BusinessException("PR 不存在或仓库为私有仓库，请检查链接是否正确", e);
        } catch (HttpClientErrorException.Forbidden e) {
            throw new BusinessException("GitHub API 访问被拒绝，可能是 API 限流，建议配置 GITHUB_TOKEN", e);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取 PR Diff 失败: {}", e.getMessage(), e);
            throw new BusinessException("获取 PR Diff 失败: " + e.getMessage(), e);
        }
    }

    /**
     * 统计 diff 中涉及的变更文件数量
     * 通过匹配 "diff --git" 前缀行数来确定文件数
     *
     * @param diffText git diff 原始文本
     * @return 变更文件数量
     */
    private int countChangedFiles(String diffText) {
        if (!StringUtils.hasText(diffText)) return 0;
        Matcher matcher = DIFF_FILE_PATTERN.matcher(diffText);
        int count = 0;
        while (matcher.find()) count++;
        return count;
    }
}
