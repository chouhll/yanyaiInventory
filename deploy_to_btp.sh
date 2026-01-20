#!/bin/bash

# SuperInventory SAP BTP Deployment Script
# 部署到 SAP BTP Cloud Foundry Trial 环境

set -e  # 遇到错误立即退出

echo "========================================="
echo "🚀 SuperInventory SAP BTP 部署脚本"
echo "========================================="
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查 CF CLI
echo "📋 步骤 1: 检查 CF CLI..."
if ! command -v cf &> /dev/null; then
    echo -e "${RED}❌ CF CLI 未安装${NC}"
    echo ""
    echo "请安装 CF CLI:"
    echo "  方式1: brew install cloudfoundry/tap/cf-cli"
    echo "  方式2: 从 https://github.com/cloudfoundry/cli/releases 下载"
    echo ""
    exit 1
fi
echo -e "${GREEN}✅ CF CLI 已安装: $(cf version)${NC}"
echo ""

# 检查是否已登录
echo "📋 步骤 2: 检查 CF 登录状态..."
if ! cf target &> /dev/null; then
    echo -e "${YELLOW}⚠️  未登录到 CF${NC}"
    echo ""
    echo "请先登录到 SAP BTP:"
    echo "  cf login -a https://api.cf.us10-001.hana.ondemand.com"
    echo ""
    read -p "是否现在登录? (y/n) " -n 1 -r
    echo ""
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        cf login -a https://api.cf.us10-001.hana.ondemand.com
    else
        echo "请手动登录后重新运行此脚本"
        exit 1
    fi
fi

echo -e "${GREEN}✅ 已登录${NC}"
cf target
echo ""

# 检查/创建 PostgreSQL 服务实例
echo "📋 步骤 3: 检查数据库服务..."
if cf service superinventory-db &> /dev/null; then
    echo -e "${GREEN}✅ 数据库服务 'superinventory-db' 已存在${NC}"
else
    echo -e "${YELLOW}⚠️  数据库服务不存在，正在创建...${NC}"
    echo ""
    echo "可用的 PostgreSQL 服务计划:"
    cf marketplace -e postgresql-db
    echo ""
    read -p "请输入服务计划名称 (例如: trial): " SERVICE_PLAN
    cf create-service postgresql-db $SERVICE_PLAN superinventory-db
    echo -e "${GREEN}✅ 数据库服务创建中（可能需要几分钟）${NC}"
    echo "等待服务就绪..."
    sleep 30
fi
echo ""

# 构建后端
echo "📋 步骤 4: 构建后端应用..."
echo "运行: mvn clean package -DskipTests"
if mvn clean package -DskipTests; then
    echo -e "${GREEN}✅ 后端构建成功${NC}"
else
    echo -e "${RED}❌ 后端构建失败${NC}"
    exit 1
fi
echo ""

# 构建前端
echo "📋 步骤 5: 构建前端应用..."
cd frontend
echo "运行: npm install"
npm install
echo "运行: npm run build"
if npm run build; then
    echo -e "${GREEN}✅ 前端构建成功${NC}"
else
    echo -e "${RED}❌ 前端构建失败${NC}"
    exit 1
fi
cd ..
echo ""

# 部署到 Cloud Foundry
echo "📋 步骤 6: 部署到 SAP BTP..."
echo ""
echo -e "${YELLOW}准备部署两个应用:${NC}"
echo "  1. superinventory-backend (Spring Boot)"
echo "  2. superinventory-frontend (Vue.js)"
echo ""
read -p "确认部署? (y/n) " -n 1 -r
echo ""
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "部署已取消"
    exit 0
fi

echo ""
echo "🚀 开始部署..."
cf push

echo ""
echo "========================================="
echo -e "${GREEN}✅ 部署完成！${NC}"
echo "========================================="
echo ""
echo "📱 应用访问地址:"
echo ""
echo "前端: https://superinventory.cfapps.us10-001.hana.ondemand.com"
echo "后端: https://superinventory-backend.cfapps.us10-001.hana.ondemand.com"
echo ""
echo "🔍 查看应用状态:"
echo "  cf apps"
echo ""
echo "📊 查看日志:"
echo "  cf logs superinventory-backend --recent"
echo "  cf logs superinventory-frontend --recent"
echo ""
echo "🔄 重启应用:"
echo "  cf restart superinventory-backend"
echo "  cf restart superinventory-frontend"
echo ""