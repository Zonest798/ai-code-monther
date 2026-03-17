package com.lele.aicodemonther.service;

import com.lele.aicodemonther.model.dto.app.AppAddRequest;
import com.lele.aicodemonther.model.dto.app.AppQueryRequest;
import com.lele.aicodemonther.model.entity.User;
import com.lele.aicodemonther.model.vo.AppVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.lele.aicodemonther.model.entity.App;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 应用 服务层。
 *
 * @author <a href="https://github.com/wyldsg666">ZoneSt</a>
 */
public interface AppService extends IService<App> {


    /**
     * 调用 AI 大模型生成应用代码
     *
     * @param appId     应用 id
     * @param message   用户提示词
     * @param loginUser 登录用户
     * @return 生成的代码
     */
    Flux<String> chatToGenCode(Long appId, String message, User loginUser);

    /**
     * 获取应用封装类
     *
     * @param app 应用
     * @return 应用封装类
     */
    AppVO getAppVO(App app);

    /**
     * 获取应用封装类列表
     *
     * @param appList 应用列表
     * @return 应用封装类列表
     */
    List<AppVO> getAppVOList(List<App> appList);

    /**
     * 构造应用查询条件
     *
     * @param appQueryRequest 查询条件
     * @return 查询条件包装器
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    /**
     * 应用部署
     *
     * @param appId     应用 id
     * @param loginUser 登录用户
     * @return 部署结果的 URL
     */
    String deployApp(Long appId, User loginUser);

    void generateAppScreenshotAsync(Long appId, String appDepLoyUrl);

    Long createApp(AppAddRequest appAddRequest, User loginUser);
}
