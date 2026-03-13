# MTD Demo 项目

基于 Spring Boot 3.x 的多模块企业级开发框架

## 📋 项目简介

这是一个标准化的企业级开发框架，采用 Maven 多模块架构，集成了常用的技术组件，提供了统一的异常处理、日志记录、权限认证、数据持久化等功能。

## 🏗️ 项目结构

```
mtd/
├── demo/                              # 主应用模块
│   ├── src/main/
│   │   ├── java/com/mtd/demo/
│   │   │   ├── controller/           # 控制器层
│   │   │   ├── entity/               # 实体类
│   │   │   ├── mapper/               # MyBatis Mapper
│   │   │   ├── request/              # 请求参数封装
│   │   │   ├── service/              # 业务逻辑层
│   │   │   └── DemoApplication.java  # 启动类
│   │   └── resources/
│   │       └── application.yml       # 配置文件
│   └── pom.xml
├── mtd-common/                        # 公共模块
│   ├── mtd-common-bom/               # BOM 依赖管理
│   ├── mtd-common-core/              # 核心模块（异常、响应、常量等）
│   ├── mtd-common-mybatis/           # MyBatis-Plus 配置
│   ├── mtd-common-satoken/           # Sa-Token 权限认证
│   ├── mtd-common-web/               # Web 通用组件
│   └── mtd-common-redis/             # Redis 配置
└── pom.xml                           # 父 POM
```

## 🛠️ 技术栈

### 核心框架
- **Spring Boot 3.x** - 核心框架
- **Maven** - 构建工具（多模块管理）

### 持久层
- **MyBatis-Plus** - ORM 框架
- **MySQL** - 关系型数据库
- **Redis** - 缓存数据库

### Web 层
- **Spring Web MVC** - Web 框架
- **Knife4j** - API 文档工具（Swagger 增强）
- **Spring AOP** - 面向切面编程

### 认证授权
- **Sa-Token** - 轻量级权限认证框架

### 工具库
- **Lombok** - 简化代码
- **Jackson** - JSON 序列化/反序列化
- **Hutool** - Java 工具类库

### 测试
- **JUnit 5** - 单元测试框架
- **Spring Test** - Spring 测试支持

## 🚀 快速开始

### 环境要求
- JDK 17+
- MySQL 5.7+ / 8.0+
- Redis 6.0+
- Maven 3.6+

### 安装步骤

1. **克隆项目**
```bash
git clone <repository-url>
cd mtd
```

2. **导入数据库**
```bash
# 执行 demo/db.sql 初始化数据库
mysql -u root -p < demo/db.sql
```

3. **修改配置**
编辑 `demo/src/main/resources/application.yml`：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mtd_demo?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: your_username
    password: your_password
  
  data:
    redis:
      host: localhost
      port: 6379
      password: your_redis_password  # 如果有密码
```

4. **编译项目**
```bash
mvn clean install
```

5. **运行应用**
```bash
cd demo
mvn spring-boot:run
```

6. **访问应用**
- 应用地址：http://localhost:8080
- API 文档：http://localhost:8080/swagger-ui.html
- Knife4j 增强文档：http://localhost:8080/doc.html

## 📦 模块说明

### mtd-common-core
核心模块，提供：
- 统一响应体 `Result<T>`
- 全局异常处理
- 基础请求参数封装
- 分页响应封装
- 业务异常类

### mtd-common-web
Web 通用组件：
- RequestID 过滤器（链路追踪）
- 统一响应体增强（ResponseBodyAdvice）
- 接口日志切面（RequestLogAspect）

### mtd-common-mybatis
MyBatis-Plus 配置：
- 分页插件
- 自动填充处理器（创建时间、更新时间、操作人）
- 逻辑删除配置

### mtd-common-satoken
Sa-Token 集成：
- 登录认证
- 权限校验
- 用户 ID 提供者实现

### mtd-common-redis
Redis 配置：
- RedisTemplate 配置
- StringRedisTemplate 配置
- Jackson 序列化器（支持 Java 8 时间类型）

### mtd-common-bom
BOM 依赖管理，统一管理各模块版本

## 🔧 功能特性

### 1. 统一响应格式
所有接口返回统一的响应格式：
```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1234567890,
  "requestId": "xxx-xxx-xxx"
}
```

### 2. 全局异常处理
- 业务异常自动捕获
- 异常信息统一返回
- 异常日志自动记录

### 3. 接口日志记录
- 自动记录请求参数和响应
- 异步日志，不影响主流程
- 携带 RequestID 便于追踪

### 4. RequestID 链路追踪
- 每个请求自动生成唯一 ID
- 通过 MDC 在日志中传递
- 在响应头中返回给客户端

### 5. 字段自动填充
- 创建时间/更新时间自动填充
- 创建人/更新人自动填充（基于登录用户）
- 逻辑删除支持

### 6. API 文档
- 基于 Knife4j 的 Swagger 文档
- 支持在线调试
- 中文界面优化

## 📝 使用示例

### Redis 测试
项目包含 Redis 测试示例：

**Controller**: `RedisTestController`
```java
@RestController
@RequestMapping("/redis/test")
public class RedisTestController {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @PostMapping("/set")
    public Result<Void> set(@RequestParam String key, 
                           @RequestBody Object value) {
        redisTemplate.opsForValue().set(key, value);
        return Result.success();
    }
    
    @GetMapping("/get")
    public Result<Object> get(@RequestParam String key) {
        Object value = redisTemplate.opsForValue().get(key);
        return Result.success(value);
    }
}
```

**测试类**: `RedisTestControllerTest`
```java
@SpringBootTest
public class RedisTestControllerTest {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Test
    public void testRedisOperations() {
        // 存储对象到 Redis
        redisTemplate.opsForValue().set("user1", user);
        
        // 从 Redis 读取
        Object user = redisTemplate.opsForValue().get("user1");
    }
}
```

### 用户管理接口

**查询用户列表**
```bash
POST /user/list
Content-Type: application/json

{
  "current": 1,
  "size": 10,
  "sortField": "createTime",
  "sortOrder": "desc"
}
```

**保存用户**
```bash
POST /user/save
Content-Type: application/json

{
  "username": "test",
  "nickname": "测试用户",
  "email": "test@example.com"
}
```

## ⚙️ 配置说明

### 主要配置文件
- `application.yml` - 主配置文件
- `application-dev.yml` - 开发环境配置（可选）
- `application-prod.yml` - 生产环境配置（可选）

### 关键配置项

**数据库配置**
```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/mtd_demo
    username: root
    password: root
```

**Redis 配置**
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
```

**Sa-Token 配置**
```yaml
sa-token:
  token-name: Authorization
  timeout: 2592000  # 30 天
  is-log: false
```

**MyBatis-Plus 配置**
```yaml
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      logic-delete-field: deleted
```

## 🎯 开发规范

### 代码规范
- 变量命名使用 camelCase
- 常量使用大写字母 + 下划线
- 使用 Lombok 简化代码
- 避免字段硬编码，使用注解生成常量

### 接口规范
- 统一使用 POST 请求
- 入参使用专门的 Request 类
- 返回统一使用 `Result<T>` 包装
- 分页查询使用统一的分页参数

### 异常处理
- 业务异常使用 `BusinessException`
- 严重业务异常使用 `CriticalBusinessException`
- 系统异常自动捕获并返回友好提示

## 📊 数据库设计

### User 表
```sql
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(100) NOT NULL COMMENT '密码',
  `nickname` varchar(50) DEFAULT NULL COMMENT '昵称',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机',
  `status` tinyint DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `deleted` tinyint DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
```

## 🔍 常见问题

### 1. 启动失败：找不到 Redis
确保 Redis 服务已启动，并检查配置文件中的 Redis 连接信息。

### 2. 数据库连接失败
检查 MySQL 服务是否启动，数据库是否存在，用户名密码是否正确。

### 3. API 文档无法访问
确认应用已成功启动，访问正确的路径 `/swagger-ui.html` 或 `/doc.html`。

## 📄 License

本项目采用 MIT 许可证

## 👥 贡献指南

欢迎提交 Issue 和 Pull Request！

## 📧 联系方式

如有问题，请通过以下方式联系：
- Email: support@mtd.com
- Issues: GitHub Issues
