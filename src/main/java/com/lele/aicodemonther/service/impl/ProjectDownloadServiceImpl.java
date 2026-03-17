package com.lele.aicodemonther.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import com.lele.aicodemonther.exception.BusinessException;
import com.lele.aicodemonther.exception.ErrorCode;
import com.lele.aicodemonther.exception.ThrowUtils;
import com.lele.aicodemonther.service.ProjectDownloadService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

@Service
@Slf4j
public class ProjectDownloadServiceImpl implements ProjectDownloadService {

    /**
     * 忽略的文件夹名称
     */
    private static final Set<String> IGNORED_NAMES = Set.of(
            "node_modules",
            ".git",
            "dist",
            "build",
            ".DS_Store",
            ".idea",
            ".evn",
            ".vscode",
            ".mvn",
            "target"
    );

    /**
     * 忽略的文件扩展名
     */
    private static final Set<String> IGNORED_EXTENSIONS = Set.of(
            ".log",
            ".tmp",
            ".cache"
    );

    @Override
    public void downloadProjectAsZip(String projectPath, String name, HttpServletResponse response) {

        // 基础校验
        ThrowUtils.throwIf(StrUtil.isBlank(projectPath), ErrorCode.PARAMS_ERROR, "项目路径不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(name), ErrorCode.PARAMS_ERROR, "项目名称不能为空");
        File projectDir = new File(projectPath);
        ThrowUtils.throwIf(!projectDir.exists(), ErrorCode.PARAMS_ERROR, "项目路径不存在");
        ThrowUtils.throwIf(!projectDir.isDirectory(), ErrorCode.PARAMS_ERROR, "项目路径不是一个目录");
        log.info("开始打包下载项目: {} -> {}.zip", projectPath, name);
        // 设置 HTTP 响应头
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/zip");
        response.addHeader("Content-Disposition",
                String.format("attachment; filename=\"%s.zip\"", name));
        // 定义文件过滤器
        FileFilter filter = file -> isPathAllowed(projectDir.toPath(), file.toPath());

        // 压缩
        try {
            ZipUtil.zip(response.getOutputStream(), StandardCharsets.UTF_8, false, filter, projectDir);
            log.info("打包下载项目成功：{} -> {}.zip", projectPath, name);
        } catch (IOException e) {
            log.error("打包下载项目失败: {}", projectPath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "打包下载项目失败");
        }
    }

    /**
     * 校验路径是否允许包含在压缩包中
     *
     * @param projectRoot 项目根路径
     * @param fullPath    项目的完整路径
     * @return 是否允许
     */
    private boolean isPathAllowed(Path projectRoot, Path fullPath) {
        // 获取相对路径
        Path relativePath = projectRoot.relativize(fullPath);
        // 检查文件名和扩展名是否否和要求
        for (Path part : relativePath) {
            String partName = part.toString();
            // 检查是否在忽略列表中
            if (IGNORED_NAMES.contains(partName)) {
                return false;
            }
            // 检查扩展名是否在忽略列表中
            if (partName.contains(".")) {
                String extension = partName.substring(partName.lastIndexOf("."));
                if (IGNORED_EXTENSIONS.contains(extension)) {
                    return false;
                }
            }
        }

        return true;
    }
}
