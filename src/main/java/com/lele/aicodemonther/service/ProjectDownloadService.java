package com.lele.aicodemonther.service;

import jakarta.servlet.http.HttpServletResponse;

public interface ProjectDownloadService {

    /**
     * 下载项目
     * @param projectPath 项目路径
     * @param name 项目名称
     * @param response 响应
     */
    void downloadProjectAsZip(String projectPath, String name, HttpServletResponse response);
}
