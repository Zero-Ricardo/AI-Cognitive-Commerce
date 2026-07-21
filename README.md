# AI 商品采购平台

当前已完成综合超市电商 V2：在用户注册登录、全品类商品、收藏、购物车和管理后台基础上，加入 Elasticsearch 商品搜索、Redis 热销榜、浏览历史，以及仅用于验证交互的前端 AI 导购 Mock。

## 技术栈

- C 端及后台：React 19、TypeScript、Vite、Ant Design
- 后端：Java 21、Spring Boot、Spring Security、Spring Data JPA、Flyway
- 数据与搜索：MySQL 8.4、Redis 7.4、Elasticsearch 8.17
- 部署：Docker Compose

## Docker 一键启动

```bash
cp .env.example .env
docker compose up --build
```

启动后访问：

- C 端商城：http://localhost:3000
- 管理后台：http://localhost:3001
- 后端健康检查：http://localhost:8080/actuator/health
- Elasticsearch：http://localhost:9200

演示账号：

- 普通用户：`demo` / `User@123456`
- 管理员：`admin` / `Admin@123456`

首次公开部署前必须通过 `.env` 修改管理员密码和数据库密码。

## 本地开发

先启动基础数据服务：

```bash
docker compose up -d mysql redis elasticsearch
```

启动后端：

```bash
cd backend
mvn -s settings.xml spring-boot:run
```

启动前端：

```bash
cd frontend
pnpm install
pnpm dev:mall
pnpm dev:admin
```

## 工程目录

```text
backend/                Spring Boot 业务后端
frontend/apps/mall-web  C端商城
frontend/apps/admin-web 管理后台
frontend/packages/      API 类型与设计变量
docs/                   需求和系统设计文档
```

设计文档：

- [需求文档](docs/需求文档.md)
- [第一阶段前端系统设计文档（V1）](docs/前端系统设计文档-V1.md)
- [第一阶段后端系统设计文档（V1）](docs/后端系统设计文档-V1.md)
- [第二阶段前端系统设计文档（V2）](docs/前端系统设计文档-V2.md)
- [第二阶段后端系统设计文档（V2）](docs/后端系统设计文档-V2.md)

V2 已接入 Redis、Elasticsearch、浏览历史和热销指标。热销分数综合近 30 天销量、近 7 天独立浏览人数及当前收藏人数；Python AI 服务延后，当前 AI 导购页面明确使用本地 Mock 数据。

初始化数据集位于 `backend/src/main/resources/seed/supermarket-v1`，包含分类、品牌、商品、测试用户、虚构地址及带授权记录的本地实拍图片。
