package com.lele.aicodemonther.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.lele.aicodemonther.ai.AiCodeGenTypeRoutingService;
import com.lele.aicodemonther.core.AiCodeGeneratorFacade;
import com.lele.aicodemonther.core.builder.VueProjectBuilder;
import com.lele.aicodemonther.core.handler.StreamHandlerExecutor;
import com.lele.aicodemonther.coustant.AppConstant;
import com.lele.aicodemonther.exception.BusinessException;
import com.lele.aicodemonther.exception.ErrorCode;
import com.lele.aicodemonther.exception.ThrowUtils;
import com.lele.aicodemonther.model.dto.app.AppAddRequest;
import com.lele.aicodemonther.model.dto.app.AppQueryRequest;
import com.lele.aicodemonther.model.entity.User;
import com.lele.aicodemonther.model.enums.ChatHistoryMessageTypeEnum;
import com.lele.aicodemonther.model.enums.CodeGenTypeEnum;
import com.lele.aicodemonther.model.vo.AppVO;
import com.lele.aicodemonther.model.vo.UserVO;
import com.lele.aicodemonther.service.ChatHistoryService;
import com.lele.aicodemonther.service.ScreenshotService;
import com.lele.aicodemonther.service.UserService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.lele.aicodemonther.model.entity.App;
import com.lele.aicodemonther.mapper.AppMapper;
import com.lele.aicodemonther.service.AppService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 应用 服务层实现。
 *
 * @author <a href="https://github.com/wyldsg666">ZoneSt</a>
 */
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    private static final Logger log = LoggerFactory.getLogger(AppServiceImpl.class);
    @Resource
    private UserService userService;

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private StreamHandlerExecutor streamHandlerExecutor;

    @Resource
    private VueProjectBuilder vueProjectBuilder;

    @Resource
    private ScreenshotService screenshotService;

    @Resource
    private AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService;


    /**
     * 创建应用
     * @param appAddRequest 应用添加请求
     * @param loginUser 登录用户
     * @return 应用ID
     */
    @Override
    public Long createApp(AppAddRequest appAddRequest, User loginUser) {
        // 参数校验
        String initPrompt = appAddRequest.getInitPrompt();
        ThrowUtils.throwIf(StrUtil.isBlank(initPrompt), ErrorCode.PARAMS_ERROR, "初始化 prompt 不能为空");
        // 构造入库对象
        App app = new App();
        BeanUtil.copyProperties(appAddRequest, app);
        app.setUserId(loginUser.getId());
        // 应用名称暂时为 initPrompt 前 12 位
        app.setAppName(initPrompt.substring(0, Math.min(initPrompt.length(), 12)));
        // 使用 AI 智能选择代码生成类型
        CodeGenTypeEnum selectedCodeGenType = aiCodeGenTypeRoutingService.routeCodeGenType(initPrompt);
        app.setCodeGenType(selectedCodeGenType.getValue());
        // 插入数据库
        boolean result = this.save(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        log.info("应用创建成功，ID: {}, 类型: {}", app.getId(), selectedCodeGenType.getValue());
        return app.getId();
    }



    /**
     *
     * @param appId     应用 id
     * @param message   用户提示词
     * @param loginUser 登录用户
     * @return
     */
    @Override
    public Flux<String> chatToGenCode(Long appId, String message, User loginUser) {

        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 错误");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "提示词不能为空");
        // 2. 检查应用是否存在
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 校验权限，只能与自己的应用对话
        ThrowUtils.throwIf(!app.getUserId().equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR, "无权限访问该应用");
        // 4. 获取应用的代码生成类型
        String codeGenType = app.getCodeGenType();
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        ThrowUtils.throwIf(codeGenTypeEnum == null, ErrorCode.PARAMS_ERROR, "代码生成类型不能为空");
        // 5.调用AI服务前，向数据库插入用户输入的提示词
        chatHistoryService.addChatMessage(appId, message, ChatHistoryMessageTypeEnum.USER.getValue(), loginUser.getId());
        // 6. 调用代码生成器进行代码生成
        Flux<String> codeStream = aiCodeGeneratorFacade.generatorAndSaveCodeStream(message, codeGenTypeEnum, appId);
        // 7.收集Ai响应的内容完成后保存记录到对话历史
        return streamHandlerExecutor.doExecute(codeStream, chatHistoryService, appId, loginUser, codeGenTypeEnum);
    }

    /**
     *
     * @param app 应用
     * @return
     */
    @Override
    public AppVO getAppVO(App app) {
        if (app == null) {
            return null;
        }
        AppVO appVO = new AppVO();
        BeanUtil.copyProperties(app, appVO);
        // 关联查询用户信息
        Long userId = app.getUserId();
        if (userId != null) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            appVO.setUser(userVO);
        }
        return appVO;
    }


    /**
     *
     * @param appList 应用列表
     * @return
     */
    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
        // 批量获取用户信息，避免 N+1 查询问题
        Set<Long> userIds = appList.stream()
                .map(App::getUserId)
                .collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO));
        return appList.stream().map(app -> {
            AppVO appVO = getAppVO(app);
            UserVO userVO = userVOMap.get(app.getUserId());
            appVO.setUser(userVO);
            return appVO;
        }).collect(Collectors.toList());
    }


    /**
     * 根据查询条件获取查询构造器
     *
     * @param appQueryRequest 查询条件
     * @return 查询构造器
     */
    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String cover = appQueryRequest.getCover();
        String initPrompt = appQueryRequest.getInitPrompt();
        String codeGenType = appQueryRequest.getCodeGenType();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();
        Long userId = appQueryRequest.getUserId();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .like("appName", appName)
                .like("cover", cover)
                .like("initPrompt", initPrompt)
                .eq("codeGenType", codeGenType)
                .eq("deployKey", deployKey)
                .eq("priority", priority)
                .eq("userId", userId)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }

    /**
     * 部署应用
     *
     * @param appId     应用 id
     * @param loginUser 登录用户
     * @return 可访问的 URL 地址
     */
    @Override
    public String deployApp(Long appId, User loginUser) {

        // 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 错误");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.PARAMS_ERROR, "用户未登录");

        // 检查应用是否存在
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");

        // 检查用户是否有权限部署应用
        ThrowUtils.throwIf(!app.getUserId().equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR, "无权限部署该应用");

        // 检查是否已有 deployKey 如果没有则生成6位 deployKey （字母 + 数字）
        String deployKey = app.getDeployKey();
        if (StrUtil.isBlank(deployKey)) {
            deployKey = RandomUtil.randomString(6);
        }

        // 获取代码生成类型，获取原始代码生成路径
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;

        // 检查路径是否存在
        File sourceDir = new File(sourceDirPath);
        ThrowUtils.throwIf(!sourceDir.exists() || !sourceDir.isDirectory(), ErrorCode.NOT_FOUND_ERROR, "应用代码不存在，请先生成应用");

        // Vue 项目特殊处理
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if (codeGenTypeEnum == CodeGenTypeEnum.VUE_PROJECT) {
            // 构建项目
            boolean buildSuccess = vueProjectBuilder.buildProject(sourceDirPath);
            ThrowUtils.throwIf(!buildSuccess, ErrorCode.SYSTEM_ERROR, "Vue 项目构建失败,请重试");
            // 获取构建后的目录
            File distDir = new File(sourceDirPath, "dist");
            ThrowUtils.throwIf(!distDir.exists() || !distDir.isDirectory(), ErrorCode.SYSTEM_ERROR, "Vue 项目构建失败,请重试");
            // 构建完成后，则使用构建后的目录
            sourceDir = distDir;
        }

        // 复制文件到部署目录
        String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;
        try {
            FileUtil.copyContent(sourceDir, new File(deployDirPath), true);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "部署失败:" + e.getMessage());
        }

        // 更新数据库
        App appUpdate = new App();
        appUpdate.setId(appId);
        appUpdate.setDeployKey(deployKey);
        appUpdate.setDeployedTime(LocalDateTime.now());
        this.updateById(appUpdate);
        // 返回可访问的 URL 地址
        String appDepLoyUrl = String.format("%s/%s", AppConstant.CODE_DEPLOY_HOST, deployKey);

        generateAppScreenshotAsync(appId, appDepLoyUrl);
        return appDepLoyUrl;
    }

    /**
     * 异步生成应用截图
     *
     * @param appId        应用id
     * @param appDepLoyUrl 应用访问地址
     */
    @Override
    public void generateAppScreenshotAsync(Long appId, String appDepLoyUrl) {
        Thread.startVirtualThread(() -> {
            // 调用截图服务生成截图并上传
            String screenshotUrl = screenshotService.generateAndUploadScreenshot(appDepLoyUrl);

            // 更新数据库的封面
            App app = new App();
            app.setId(appId);
            app.setCover(screenshotUrl);
            boolean uploaded = this.updateById(app);
            if (!uploaded) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新应用封面失败");
            }
        });
    }


    /**
     * 删除应用时，关联删除应用对话记录
     *
     * @param id 数据主键
     * @return 删除是否成功
     */
    @Override
    public boolean removeById(Serializable id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "ID不能为空");
        }
        long appId = Long.parseLong(id.toString());
        if (appId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "ID错误");
        }

        // 先删除应用对话记录
        try {
            chatHistoryService.deleteByAppId(appId);
        } catch (Exception e) {
            log.error("删除应用对话记录失败:{}", e.getMessage());
        }

        // 删除应用
        return super.removeById(id);
    }
}
