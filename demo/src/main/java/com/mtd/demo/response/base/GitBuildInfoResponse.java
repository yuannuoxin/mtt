package com.mtd.demo.response.base;

import lombok.Data;

import java.io.Serializable;

/**
 * Git 和构建信息响应对象
 */
@Data
public class GitBuildInfoResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Git 分支名称
     */
    private String gitBranch;

    /**
     * Git 提交 ID（完整）
     */
    private String gitCommitId;

    /**
     * Git 提交时间
     */
    private String gitCommitTime;

    /**
     * 打包构建时间
     */
    private String buildTime;

    /**
     * 项目版本
     */
    private String projectVersion;
}
