#!/usr/bin/env python3
"""
数据迁移脚本 - 将本地 PostgreSQL 数据迁移到 BTP
通过后端 REST API 导入数据
"""

import psycopg2
import requests
import json
from datetime import datetime

# 配置
LOCAL_DB = {
    'host': 'localhost',
    'port': 5432,
    'database': 'superyt',
    'user': 'I323583',
    'password': ''
}

API_BASE_URL = 'https://superinventory-backend.cfapps.us10-001.hana.ondemand.com/api'

def connect_local_db():
    """连接本地数据库"""
    try:
        conn = psycopg2.connect(**LOCAL_DB)
        return conn
    except Exception as e:
        print(f"❌ 连接本地数据库失败: {e}")
        return None

def export_table(conn, table_name):
    """从本地数据库导出表数据"""
    from datetime import date
    cursor = conn.cursor()
    cursor.execute(f"SELECT * FROM {table_name}")
    columns = [desc[0] for desc in cursor.description]
    rows = cursor.fetchall()
    
    data = []
    for row in rows:
        row_dict = {}
        for i, value in enumerate(row):
            if isinstance(value, (datetime, date)):
                row_dict[columns[i]] = value.isoformat()
            elif isinstance(value, (int, float, str, bool, type(None))):
                row_dict[columns[i]] = value
            else:
                # 其他类型转换为字符串
                row_dict[columns[i]] = str(value)
        data.append(row_dict)
    
    cursor.close()
    return data

def import_to_api(endpoint, data, transform_func=None):
    """通过 API 导入数据"""
    success_count = 0
    error_count = 0
    
    for idx, item in enumerate(data):
        try:
            # 移除 ID 字段，让后端自动生成
            item_copy = {k: v for k, v in item.items() if k != 'id'}
            
            # 如果有转换函数，应用它
            if transform_func:
                item_copy = transform_func(item_copy)
            
            # 打印第一条数据用于调试
            if idx == 0 and error_count == 0:
                print(f"    📝 Sample data: {json.dumps(item_copy, indent=2, ensure_ascii=False)[:200]}...")
            
            response = requests.post(
                f"{API_BASE_URL}/{endpoint}",
                json=item_copy,
                headers={'Content-Type': 'application/json'},
                timeout=10
            )
            
            if response.status_code in [200, 201]:
                success_count += 1
            else:
                error_count += 1
                if error_count == 1:  # 只打印第一个错误的详细信息
                    print(f"    ⚠️  导入失败 (#{idx+1}): {response.status_code}")
                    print(f"       Data: {json.dumps(item_copy, ensure_ascii=False)}")
                    print(f"       Response: {response.text[:300]}")
        except Exception as e:
            error_count += 1
            if error_count == 1:
                print(f"    ❌ 错误 (#{idx+1}): {e}")
                print(f"       Data: {json.dumps(item_copy, ensure_ascii=False)}")
    
    return success_count, error_count

def snake_to_camel(snake_str):
    """将蛇形命名转换为驼峰命名"""
    components = snake_str.split('_')
    return components[0] + ''.join(x.title() for x in components[1:])

def transform_keys_to_camel(data):
    """将字典的键从蛇形转换为驼峰"""
    return {snake_to_camel(k): v for k, v in data.items()}

def transform_purchase_data(item):
    """转换采购单数据格式"""
    # 先转换为驼峰命名
    transformed = transform_keys_to_camel(item)
    
    # 将 productId 和 supplierId 转换为对象引用
    if 'productId' in transformed:
        transformed['product'] = {'id': transformed.pop('productId')}
    if 'supplierId' in transformed:
        transformed['supplier'] = {'id': transformed.pop('supplierId')}
    return transformed

def main():
    print("=" * 60)
    print("🚀 SuperInventory 数据迁移工具")
    print("=" * 60)
    print()
    
    # 连接本地数据库
    print("📡 连接本地数据库...")
    conn = connect_local_db()
    if not conn:
        return
    print("✅ 连接成功")
    print()
    
    # 迁移顺序很重要（考虑外键依赖）
    # 先迁移主表，再迁移依赖表
    migrations = [
        ('product', 'products', transform_keys_to_camel),
        ('supplier', 'suppliers', transform_keys_to_camel),
        ('customer', 'customers', transform_keys_to_camel),
        ('warehouse', 'warehouses', transform_keys_to_camel),
        # 依赖表（需要先有product和supplier）
        ('purchase', 'purchases', transform_purchase_data),
    ]
    
    total_success = 0
    total_error = 0
    
    for migration_info in migrations:
        if len(migration_info) == 3:
            table_name, endpoint, transform_func = migration_info
        else:
            table_name, endpoint = migration_info
            transform_func = None
            
        print(f"📦 迁移 {table_name}...")
        try:
            # 导出数据
            data = export_table(conn, table_name)
            print(f"  ✅ 导出 {len(data)} 条记录")
            
            # 导入数据
            if data:
                success, error = import_to_api(endpoint, data, transform_func)
                total_success += success
                total_error += error
                print(f"  ✅ 成功导入 {success} 条，失败 {error} 条")
            else:
                print(f"  ℹ️  表为空，跳过")
        except Exception as e:
            print(f"  ❌ 迁移失败: {e}")
            total_error += len(data) if 'data' in locals() else 0
        print()
    
    # 关闭连接
    conn.close()
    
    # 验证结果
    print("=" * 60)
    print("📊 迁移结果验证")
    print("=" * 60)
    
    endpoints = ['products', 'suppliers', 'customers', 'warehouses', 'purchases']
    for endpoint in endpoints:
        try:
            response = requests.get(f"{API_BASE_URL}/{endpoint}", timeout=10)
            if response.status_code == 200:
                count = len(response.json())
                print(f"  {endpoint}: {count} 条记录")
            else:
                print(f"  {endpoint}: 无法验证")
        except Exception as e:
            print(f"  {endpoint}: 验证失败 - {e}")
    
    print()
    print("=" * 60)
    print(f"✅ 迁移完成！成功: {total_success}, 失败: {total_error}")
    print("=" * 60)

if __name__ == '__main__':
    main()