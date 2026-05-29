package com.aiprreviewer.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * GitHub PR Diff 获取结果
 */
@Data
@AllArgsConstructor
public class GitHubDiffResult {

    /** git diff 原始文本 */
    private String diffText;

    /** PR 标题 */
    private String prTitle;

    /** 变更文件数量 */
    private int fileCount;
}
