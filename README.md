# AI Code Monther - AI 代码应用生成

用户只需输入自然语言描述，AI 即可自动生成完整的 Web 应用，支持单页 HTML、多文件 HTML/CSS/JS、完整 Vue 项目三种模式，并可一键部署和下载代码。

## 技术栈

### 后端

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 21 | 运行环境 |
| Spring Boot | 3.5.10 | Web 框架 |
| MyBatis-Flex | 1.11.5 | ORM 框架 |
| MySQL | - | 关系型数据库 |
| Redis | - | 会话存储 & AI 对话记忆 |
| LangChain4j | 1.1.0 | AI 对话框架（OpenAI 兼容接口） |
| Selenium | 4.33.0 | 网页截图 |
| 腾讯云 COS | 5.6.227 | 对象存储（截图上传） |
| Caffeine | - | 本地缓存 |
| Knife4j | 4.4.0 | API 文档 |
| Hutool | 5.8.40 | 工具库 |
| Lombok | 1.18.42 | 代码简化 |

### 前端

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.5 | 前端框架 |
| TypeScript | - | 类型系统 |
| Vite | 7 | 构建工具 |
| Ant Design Vue | 4 | UI 组件库 |
| Pinia | - | 状态管理 |
| Vue Router | 4 | 路由管理 |

## 项目结构

```
ai-code-monther/
├── pom.xml                              # Maven 配置
├── sql/                                 # 数据库脚本
│   └── creat_table.sql                  # 建表 SQL
├── src/main/java/com/lele/aicodemonther/
│   ├── AiCodeMontherApplication.java    # 启动类
│   ├── ai/                              # AI 服务层
│   │   ├── AiCodeGeneratorService.java  # AI 代码生成接口
│   │   ├── AiCodeGeneratorServiceFactory.java  # AI 服务工厂（Caffeine 缓存）
│   │   ├── AiCodeGenTypeRoutingService.java    # AI 路由服务（智能选择生成类型）
│   │   ├── model/                       # AI 模型 DTO
│   │   └── tools/                       # AI 工具调用
│   ├── annotation/                      # 自定义注解
│   ├── common/                          # 通用类（响应封装、分页等）
│   ├── config/                          # 配置类（CORS、Redis、模型等）
│   ├── controller/                      # 控制器
│   │   ├── AppController.java           # 应用管理 + 对话 + 部署 + 下载
│   │   ├── ChatHistoryController.java   # 聊天历史
│   │   ├── UserController.java          # 用户认证
│   │   ├── StaticResourceController.java # 静态资源服务
│   │   └── HealthController.java        # 健康检查
│   ├── core/                            # 核心代码生成逻辑
│   │   ├── AiCodeGeneratorFacade.java   # 门面：编排代码生成 + 保存
│   │   ├── builder/                     # Vue 项目构建
│   │   ├── handler/                     # 流式消息处理
│   │   ├── parser/                      # 代码解析器（HTML / 多文件）
│   │   └── saver/                       # 文件保存器（模板方法模式）
│   ├── exception/                       # 异常处理
│   ├── manager/                         # COS 上传管理
│   ├── mapper/                          # MyBatis Mapper
│   ├── model/                           # 数据模型
│   │   ├── dto/                         # 请求 DTO
│   │   ├── entity/                      # 数据库实体
│   │   ├── enums/                       # 枚举
│   │   └── vo/                          # 视图对象
│   ├── service/                         # 业务逻辑层
│   └── utils/                           # 工具类（网页截图）
├── src/main/resources/
│   ├── application.yml                  # 应用配置
│   └── prompt/                          # AI 系统提示词
│       ├── codegen-html-system-prompt.txt
│       ├── codegen-multi-file-system-prompt.txt
│       ├── codegen-routing-system-prompt.txt
│       └── codegen-vue-project-system-prompt.txt
└── ai-code-monther-frontend/            # 前端项目
    ├── package.json
    ├── vite.config.ts
    └── src/
        ├── api/                         # 接口（自动生成）
        ├── components/                  # 公共组件
        ├── pages/                       # 页面
        │   ├── HomePage.vue             # 首页（输入 Prompt）
        │   ├── app/
        │   │   ├── AppChatPage.vue      # AI 对话 + 实时预览
        │   │   └── AppEditPage.vue      # 应用编辑
        │   ├── admin/                   # 管理后台
        │   └── user/                    # 用户中心
        ├── router/                      # 路由配置
        ├── stores/                      # Pinia 状态
        └── access/                      # 权限控制
```

## 核心功能

### 1. 智能代码生成

- **单页 HTML**：简单页面，一次生成完整的 HTML 文件
- **多文件 HTML/CSS/JS**：多页面静态网站，拆分为多个文件
- **Vue 项目**：完整 Vue 项目，支持 AI 工具调用写文件 + 自动 npm 构建

AI 会根据用户 Prompt **自动路由**到最合适的代码生成类型，也可手动指定。

### 2. AI 流式对话（SSE）

- 基于 Server-Sent Events 的实时流式响应
- AI 回复逐字输出，Vue 项目模式下实时展示工具调用过程
- 对话历史持久化存储，支持上下文连续对话（最近 20 条记忆）

### 3. 实时预览

- 对话右侧嵌入 iframe，生成的代码实时渲染预览
- 支持刷新预览页面

### 4. 一键部署

- 生成的代码自动复制到部署目录，生成唯一访问链接
- Vue 项目自动执行 `npm install` + `npm run build`
- 部署后异步通过 Selenium 截图，上传至腾讯云 COS 作为应用封面

### 5. 代码下载

- 支持将生成的代码打包为 ZIP 下载

### 6. 用户系统 & 管理后台

- 注册 / 登录 / 会话管理（Redis Session）
- 管理员后台：用户管理、应用管理、对话管理
- 应用可设为精选展示在首页

## 快速开始

### 环境要求

- JDK 21+
- Node.js 18+
- MySQL 8.0+
- Redis 7.0+
- Maven 3.8+
- Chrome 浏览器（截图功能需要）

### 1. 初始化数据库

```bash
# 执行建表脚本
mysql -u root -p < sql/creat_table.sql
```

### 2. 配置后端

修改 `src/main/resources/application.yml`：

```yaml
server:
  port: 8123

spring:
  session:
    timeout: 2592000  # 30 天
    store-type: redis
  data:
    redis:
      host: localhost
      port: 6379

springai:
  openai:
    api-key: your-api-key        # AI API Key
    base-url: your-base-url      # AI 接口地址（OpenAI 兼容）
    chat:
      options:
        model: your-model-name   # 模型名称

mybatis-flex:
  datasource:
    url: jdbc:mysql://localhost:3306/ai-code-monther
    username: root
    password: 123456
```

### 3. 启动后端

```bash
mvn spring-boot:run
```

### 4. 启动前端

```bash
cd ai-code-monther-frontend
npm install
npm run dev
```

前端访问地址：`http://localhost:5173`  
后端 API 地址：`http://localhost:8123/api`  
Knife4j 文档：`http://localhost:8123/doc.html`

## 数据库设计

### user 表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键（雪花算法） |
| userAccount | varchar(256) | 用户账号（唯一） |
| userPassword | varchar(512) | 密码（MD5 + 盐值） |
| userName | varchar(256) | 用户昵称 |
| userAvatar | varchar(1024) | 头像 URL |
| userProfile | varchar(512) | 个人简介 |
| userRole | varchar(256) | 角色（user / admin） |

### app 表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键（雪花算法） |
| appName | varchar(256) | 应用名称 |
| cover | varchar(512) | 封面截图 URL |
| initPrompt | text | 初始提示词 |
| codeGenType | varchar(64) | 代码生成类型（html / multi_file / vue_project） |
| deployKey | varchar(64) | 部署唯一标识 |
| deployedTime | datetime | 部署时间 |
| priority | int | 优先级（99 = 精选） |
| userId | bigint | 所属用户 |

### chat_history 表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键（雪花算法） |
| message | text | 消息内容 |
| messageType | varchar(32) | 消息类型（user / ai） |
| appId | bigint | 所属应用 |
| userId | bigint | 所属用户 |

## API 接口

### 应用管理

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/app/add` | 创建应用 |
| GET | `/api/app/chat/gen/code` | AI 对话生成代码（SSE） |
| POST | `/api/app/deploy` | 部署应用 |
| GET | `/api/app/download/{appId}` | 下载应用代码 |
| POST | `/api/app/my/list/page/vo` | 我的应用列表 |
| POST | `/api/app/good/list/page/vo` | 精选应用列表 |

### 用户管理

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/user/register` | 注册 |
| POST | `/api/user/login` | 登录 |
| GET | `/api/user/get/login` | 获取当前登录用户 |
| POST | `/api/user/logout` | 退出登录 |

### 聊天记录

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/chatHistory/app/{appId}` | 获取应用聊天记录 |

## 设计模式

| 模式 | 应用场景 |
|------|----------|
| 模板方法 | `CodeFileSaveTemplate` 统一文件保存流程 |
| 策略模式 | `CodeParser` 接口，HTML / 多文件解析器 |
| 门面模式 | `AiCodeGeneratorFacade` 编排生成、解析、保存 |
| 工厂模式 | `AiCodeGeneratorServiceFactory` + Caffeine 缓存创建 AI 服务 |
| AOP | `AuthInterceptor` 基于 `@AuthCheck` 注解做权限校验 |
