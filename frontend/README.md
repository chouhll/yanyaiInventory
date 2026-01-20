# 超级库存管理系统 - 前端

现代化的Vue 3库存管理系统前端，与Spring Boot后端无缝集成。

## 技术栈

- **Vue 3** - 渐进式JavaScript框架
- **Vite** - 下一代前端构建工具
- **Element Plus** - 基于Vue 3的组件库
- **Vue Router** - 官方路由管理器
- **Axios** - HTTP客户端

## 功能特性

### 1. 仪表盘
- 实时统计数据展示
- 产品、采购、订单、客户数量概览
- 最近产品和订单快速查看

### 2. 产品管理
- 产品列表查看
- 添加新产品
- 支持库存编码、规格型号、分类、单位等详细信息
- 库存数量实时展示

### 3. 采购管理
- 采购单创建和管理
- 支持待入库、已完成状态
- 自动计算采购总额
- 关联产品信息

### 4. 订单管理
- 创建销售订单
- 多商品订单支持
- 订单状态跟踪
- 自动库存扣减

### 5. 客户管理
- 客户信息维护
- 完整的联系信息管理

### 6. 库存报表
- 月度库存报表生成
- 期初、入库、出库、结存数据
- 多维度数据展示
- 汇总统计功能

## 项目结构

```
frontend/
├── src/
│   ├── api/              # API接口封装
│   │   └── index.js      # 统一的API调用
│   ├── router/           # 路由配置
│   │   └── index.js      # 路由定义
│   ├── views/            # 页面视图
│   │   ├── Dashboard.vue         # 仪表盘
│   │   ├── Products.vue          # 产品管理
│   │   ├── Purchases.vue         # 采购管理
│   │   ├── Orders.vue            # 订单管理
│   │   ├── Customers.vue         # 客户管理
│   │   └── InventoryReport.vue   # 库存报表
│   ├── App.vue           # 根组件
│   └── main.js           # 入口文件
├── index.html            # HTML模板
├── vite.config.js        # Vite配置
├── package.json          # 依赖配置
└── README.md             # 说明文档
```

## 安装和运行

### 前置要求

- Node.js 16+ 
- npm 或 yarn

### 安装依赖

```bash
cd frontend
npm install
```

### 开发模式运行

```bash
npm run dev
```

前端将在 `http://localhost:3000` 启动

### 生产构建

```bash
npm run build
```

构建文件将输出到 `dist` 目录

### 预览生产构建

```bash
npm run preview
```

## API配置

前端通过Vite代理与后端通信，配置在 `vite.config.js`：

```javascript
server: {
  port: 3000,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

确保后端服务运行在 `http://localhost:8080`

## 页面路由

| 路径 | 页面 | 说明 |
|------|------|------|
| `/dashboard` | 仪表盘 | 系统概览和统计 |
| `/products` | 产品管理 | 产品CRUD操作 |
| `/purchases` | 采购管理 | 采购单管理 |
| `/orders` | 订单管理 | 销售订单管理 |
| `/customers` | 客户管理 | 客户信息管理 |
| `/inventory-report` | 库存报表 | 月度库存报表 |

## 设计特点

### 现代化UI
- 使用Element Plus组件库
- 深色侧边栏 + 浅色内容区
- 响应式卡片布局
- 清晰的视觉层次

### 用户体验
- 加载状态提示
- 操作反馈（成功/失败消息）
- 表单验证
- 友好的错误提示

### 代码规范
- Vue 3 Composition API
- 组件化开发
- 统一的API调用封装
- 清晰的代码结构

## 与后端集成

### API调用示例

```javascript
// 获取产品列表
import { productApi } from '../api'

const products = await productApi.getAll()

// 创建产品
await productApi.create({
  inventoryCode: 'P00001',
  name: '产品名称',
  price: 100.00,
  stock: 50
})
```

### 数据格式

所有API调用返回JSON格式数据，与后端Controller直接对应。

## 开发指南

### 添加新页面

1. 在 `src/views/` 创建新组件
2. 在 `src/router/index.js` 添加路由
3. 在 `src/App.vue` 添加菜单项
4. 如需新API，在 `src/api/index.js` 添加

### 自定义样式

Element Plus主题可以在 `src/main.js` 中配置：

```javascript
app.use(ElementPlus, {
  locale: zhCn,
  // 自定义主题配置
})
```

## 常见问题

### 1. 无法连接后端

确保：
- 后端服务已启动在 `http://localhost:8080`
- 后端Controller有 `@CrossOrigin` 注解
- Vite代理配置正确

### 2. 依赖安装失败

尝试：
```bash
rm -rf node_modules
rm package-lock.json
npm install
```

### 3. 端口被占用

修改 `vite.config.js` 中的端口：
```javascript
server: {
  port: 3001  // 改为其他端口
}
```

## 浏览器支持

- Chrome (推荐)
- Firefox
- Safari
- Edge

## 后续优化建议

1. 添加用户认证和权限管理
2. 实现报表导出（Excel/PDF）
3. 添加数据可视化图表
4. 实现实时通知功能
5. 添加移动端适配
6. 实现离线缓存
7. 添加国际化支持

## 许可证

MIT License