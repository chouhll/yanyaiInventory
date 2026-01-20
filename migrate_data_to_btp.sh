#!/bin/bash

# 数据迁移脚本 - 将本地数据库数据迁移到 BTP
# 通过后端 API 导入数据

API_URL="https://superinventory-backend.cfapps.us10-001.hana.ondemand.com/api"

echo "🚀 开始数据迁移到 BTP..."
echo ""

# 1. 导出本地数据库数据为 JSON
echo "📤 步骤 1: 导出本地数据..."

# 导出产品数据
echo "  - 导出产品数据..."
psql -h localhost -p 5432 -U I323583 -d superyt -t -A -F"," -c "SELECT row_to_json(t) FROM (SELECT * FROM product) t" > /tmp/products.json

# 导出客户数据
echo "  - 导出客户数据..."
psql -h localhost -p 5432 -U I323583 -d superyt -t -A -F"," -c "SELECT row_to_json(t) FROM (SELECT * FROM customer) t" > /tmp/customers.json

# 导出供应商数据
echo "  - 导出供应商数据..."
psql -h localhost -p 5432 -U I323583 -d superyt -t -A -F"," -c "SELECT row_to_json(t) FROM (SELECT * FROM supplier) t" > /tmp/suppliers.json

# 导出仓库数据
echo "  - 导出仓库数据..."
psql -h localhost -p 5432 -U I323583 -d superyt -t -A -F"," -c "SELECT row_to_json(t) FROM (SELECT * FROM warehouse) t" > /tmp/warehouses.json

echo "✅ 数据导出完成"
echo ""

# 2. 通过 API 导入数据
echo "📥 步骤 2: 通过 API 导入数据到 BTP..."

# 导入产品
echo "  - 导入产品..."
while IFS= read -r line; do
  if [ ! -z "$line" ]; then
    curl -s -X POST "$API_URL/products" \
      -H "Content-Type: application/json" \
      -d "$line" > /dev/null
  fi
done < /tmp/products.json

# 导入客户
echo "  - 导入客户..."
while IFS= read -r line; do
  if [ ! -z "$line" ]; then
    curl -s -X POST "$API_URL/customers" \
      -H "Content-Type: application/json" \
      -d "$line" > /dev/null
  fi
done < /tmp/customers.json

# 导入供应商
echo "  - 导入供应商..."
while IFS= read -r line; do
  if [ ! -z "$line" ]; then
    curl -s -X POST "$API_URL/suppliers" \
      -H "Content-Type: application/json" \
      -d "$line" > /dev/null
  fi
done < /tmp/suppliers.json

# 导入仓库
echo "  - 导入仓库..."
while IFS= read -r line; do
  if [ ! -z "$line" ]; then
    curl -s -X POST "$API_URL/warehouses" \
      -H "Content-Type: application/json" \
      -d "$line" > /dev/null
  fi
done < /tmp/warehouses.json

echo ""
echo "✅ 数据迁移完成！"
echo ""
echo "📊 验证迁移结果:"
echo "  产品数量: $(curl -s $API_URL/products | jq '. | length')"
echo "  客户数量: $(curl -s $API_URL/customers | jq '. | length')"
echo "  供应商数量: $(curl -s $API_URL/suppliers | jq '. | length')"
echo "  仓库数量: $(curl -s $API_URL/warehouses | jq '. | length')"
echo ""

# 清理临时文件
rm -f /tmp/products.json /tmp/customers.json /tmp/suppliers.json /tmp/warehouses.json

echo "✨ 迁移任务完成！"