# 库存管理业务逻辑改进

## 📋 改进概述

实现了完整的订单-库存联动机制，确保库存在订单出库时才扣减，并在库存不足时给出明确提示。

---

## 🔄 业务流程

### 订单创建流程

```
1. 用户创建订单
   ↓
2. 系统检查所有商品库存
   ↓
3a. 库存充足？
   YES → 订单状态：PAID（已付款）
         不扣减库存，等待出库
   ↓
3b. 库存不足？
   YES → 订单状态：PENDING_STOCK（待备货）
         提示：商品[XXX]库存不足，当前库存：X，需求数量：Y
   ↓
4. 订单保存成功
```

### 订单出库流程

```
1. 用户点击"出库/发货"
   ↓
2. 检查订单状态
   - 只允许 PAID / INVOICED / PENDING_STOCK 状态出库
   ↓
3. 再次验证库存
   ↓
4a. 库存充足？
   YES → 扣减库存
         记录出库交易
         订单状态 → SHIPPED（已发货）
   ↓
4b. 库存不足？
   YES → 订单状态 → PENDING_STOCK
         提示：库存不足，无法出库
         禁止出库操作
```

---

## 🆕 新增功能

### 1. 新增订单状态

**OrderStatus.java** - 添加 `PENDING_STOCK` 状态

```java
public enum OrderStatus {
    CREATED,        // 已创建/下单
    UNPAID,         // 未付款
    PAID,           // 付款完成
    PENDING_STOCK,  // 待备货（库存不足）✨ 新增
    INVOICED,       // 已开票
    SHIPPED,        // 已发货
    COMPLETED,      // 已完成
    CANCELLED       // 已取消
}
```

### 2. 改进订单创建逻辑

**OrderService.save()** - 只检查库存，不扣减

**改进前：**
- ❌ 创建订单时立即扣减库存
- ❌ 没有待备货状态
- ❌ 库存不足直接失败

**改进后：**
- ✅ 创建订单时只检查库存
- ✅ 库存不足设置为 PENDING_STOCK
- ✅ 详细的库存不足提示信息
- ✅ 库存充足设置为 PAID，等待出库

### 3. 新增出库方法

**OrderService.shipOrder(orderId)** - 出库并扣减库存

功能：
- ✅ 验证订单状态
- ✅ 再次检查库存
- ✅ 库存充足则扣减库存
- ✅ 记录出库交易
- ✅ 更新订单状态为 SHIPPED

### 4. 新增库存检查方法

**OrderService.checkStockAvailability(orderId)** - 检查库存是否充足

返回：
- `true` - 所有商品库存充足
- `false` - 有商品库存不足

---

## 🔌 API 端点

### 1. 创建订单（改进）

```http
POST /api/orders
Content-Type: application/json

{
  "customer": { "id": "customer-id" },
  "items": [
    {
      "product": { "id": "product-id" },
      "quantity": 10,
      "unitPrice": 100
    }
  ]
}
```

**响应（库存充足）：**
```json
{
  "id": "order-id",
  "status": "PAID",
  "items": [...]
}
```

**响应（库存不足）：**
```json
{
  "error": "订单已创建，但库存不足，状态为待备货。商品[产品A]库存不足，当前库存：5，需求数量：10；"
}
```

### 2. 订单出库（新增）✨

```http
POST /api/orders/{orderId}/ship
```

**响应（成功）：**
```json
{
  "id": "order-id",
  "status": "SHIPPED",
  "items": [...]
}
```

**响应（库存不足）：**
```json
{
  "error": "库存不足，无法出库。商品[产品A]库存不足，当前库存：5，需求数量：10；"
}
```

### 3. 检查库存（新增）✨

```http
GET /api/orders/{orderId}/check-stock
```

**响应：**
```json
true  // 或 false
```

---

## 📊 状态转换图

```
订单创建
   ↓
[库存检查]
   ↓
┌──────────────┬──────────────┐
│  库存充足    │  库存不足    │
↓              ↓              
PAID          PENDING_STOCK
│              │
│ [补货后]     │
│              ↓
│         [库存检查]
│              │
└──────────────┴──────────────┘
        ↓
   [出库操作]
        ↓
   [库存验证]
        ↓
   [扣减库存]
        ↓
     SHIPPED
        ↓
    COMPLETED
```

---

## 🎯 关键改进点

### 1. 库存扣减时机

**改进前：**
- 订单创建时立即扣减
- 问题：订单未出库就占用库存

**改进后：**
- 订单出库时才扣减
- 优点：更真实反映库存状态

### 2. 库存不足处理

**改进前：**
- 直接拒绝创建订单
- 用户体验差

**改进后：**
- 允许创建订单，标记为待备货
- 详细提示缺货信息
- 补货后可继续出库

### 3. 出库验证

**改进前：**
- 无出库验证
- 可能超卖

**改进后：**
- 出库时再次验证库存
- 防止超卖
- 实时库存检查

### 4. 用户提示

**改进前：**
- 简单错误信息

**改进后：**
- 详细的库存信息
- 明确的缺货数量
- 友好的提示信息

---

## 📝 使用场景

### 场景1：库存充足的正常订单

```
1. 创建订单（10件商品A，库存20件）
   → 订单状态：PAID
   → 库存：仍为20件（未扣减）

2. 出库操作
   → 验证库存：充足
   → 扣减库存：20 - 10 = 10件
   → 订单状态：SHIPPED
```

### 场景2：库存不足的订单

```
1. 创建订单（10件商品A，库存5件）
   → 订单状态：PENDING_STOCK
   → 提示：商品[商品A]库存不足，当前库存：5，需求数量：10
   → 库存：仍为5件（未扣减）

2. 尝试出库
   → 验证库存：不足
   → 禁止出库
   → 提示：库存不足，无法出库

3. 补货（入库5件，库存变为10件）

4. 再次出库
   → 验证库存：充足
   → 扣减库存：10 - 10 = 0件
   → 订单状态：SHIPPED
```

### 场景3：部分库存不足

```
1. 创建订单（商品A 10件，商品B 5件）
   - 商品A库存：15件 ✅
   - 商品B库存：3件 ❌

2. 订单创建结果
   → 订单状态：PENDING_STOCK
   → 提示：商品[商品B]库存不足，当前库存：3，需求数量：5
   → 等待商品B补货
```

---

## 🔒 安全机制

### 1. 并发控制

```java
@Transactional  // 事务保证
public Order shipOrder(String orderId) {
    // 在事务内检查和扣减库存
    // 避免并发超卖
}
```

### 2. 二次验证

- 创建时检查：预警
- 出库时检查：真实验证
- 双重保险，防止超卖

### 3. 状态控制

- 只允许特定状态出库
- 防止重复出库
- 状态机保证流程正确

---

## 🎨 前端适配（待实现）

### Orders.vue 需要更新

1. **显示订单状态**
   - PENDING_STOCK → "待备货"标签（橙色）
   - 显示库存不足提示

2. **添加出库按钮**
   - 只对 PAID/INVOICED/PENDING_STOCK 状态显示
   - 点击调用 `/api/orders/{id}/ship`

3. **库存检查**
   - 出库前调用 `/api/orders/{id}/check-stock`
   - 显示库存状态

4. **错误处理**
   - 捕获库存不足异常
   - 友好提示用户

### 示例代码

```javascript
// 出库操作
async shipOrder(orderId) {
  try {
    // 先检查库存
    const hasStock = await api.get(`/orders/${orderId}/check-stock`)
    
    if (!hasStock) {
      this.$message.warning('库存不足，无法出库')
      return
    }
    
    // 执行出库
    await api.post(`/orders/${orderId}/ship`)
    this.$message.success('出库成功')
    this.loadOrders()
  } catch (error) {
    this.$message.error(error.response.data.error || '出库失败')
  }
}
```

---

## 📦 部署步骤

### 1. 构建后端

```bash
mvn clean package -DskipTests
```

### 2. 部署到BTP

```bash
cf login -a https://api.cf.us10-001.hana.ondemand.com --sso
cf push superinventory-backend
```

### 3. 更新前端

```bash
cd frontend
npm run build
cd ..
cf push superinventory-frontend
```

### 4. 验证部署

```bash
# 检查应用状态
cf apps

# 查看后端日志
cf logs superinventory-backend --recent

# 测试API
curl https://superinventory-backend.cfapps.us10-001.hana.ondemand.com/api/orders
```

---

## ✅ 测试清单

- [ ] 创建库存充足的订单
- [ ] 创建库存不足的订单
- [ ] 出库操作（库存充足）
- [ ] 出库操作（库存不足）
- [ ] 检查库存API
- [ ] 状态转换逻辑
- [ ] 并发订单测试
- [ ] 库存扣减准确性

---

## 📚 相关文件

### 后端文件
- `src/main/java/com/yantai/superinventory/model/OrderStatus.java`
- `src/main/java/com/yantai/superinventory/service/OrderService.java`
- `src/main/java/com/yantai/superinventory/controller/OrderController.java`
- `src/main/java/com/yantai/superinventory/service/InventoryService.java`

### 前端文件（待更新）
- `frontend/src/views/Orders.vue`
- `frontend/src/api/index.js`

---

## 🎯 总结

本次改进实现了完整的库存管理业务逻辑：

✅ **订单创建** - 只检查库存，不扣减
✅ **库存不足** - 标记为待备货，详细提示
✅ **订单出库** - 验证并扣减库存
✅ **防止超卖** - 二次验证 + 事务控制
✅ **状态管理** - 完整的状态转换流程
✅ **用户体验** - 友好的错误提示

这套逻辑确保了库存管理的准确性和业务流程的合理性！

---

*最后更新：2026-01-20 14:10*
