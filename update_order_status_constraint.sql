-- 更新订单状态约束，添加 CONTRACT_DRAFT 状态
-- 用于支持合同拟定功能

-- 1. 先删除旧的检查约束
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_status_check;

-- 2. 添加新的检查约束，包含 CONTRACT_DRAFT
ALTER TABLE orders ADD CONSTRAINT orders_status_check 
    CHECK (status IN (
        'CREATED',
        'CONTRACT_DRAFT',
        'UNPAID', 
        'PAID',
        'PENDING_STOCK',
        'INVOICED',
        'SHIPPED',
        'COMPLETED',
        'CANCELLED'
    ));

-- 验证约束是否创建成功
SELECT conname, contype, pg_get_constraintdef(oid) as definition
FROM pg_constraint
WHERE conrelid = 'orders'::regclass AND conname = 'orders_status_check';
