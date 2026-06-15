# SonarQube 代码质量检查指南

本文档介绍如何在 Tailor IS 项目中集成 SonarQube 进行代码质量分析。

## 概述

SonarQube 用于持续代码质量检测，覆盖以下维度：

- 代码异味（Code Smells）
- 安全漏洞（Vulnerabilities）
- Bug 检测
- 代码覆盖率
- 重复代码

## SonarQube 部署

```bash
docker run -d \
  --name sonarqube \
  -p 9000:9000 \
  -e SONAR_JDBC_URL=jdbc:postgresql://host:5432/sonar \
  -e SONAR_JDBC_USERNAME=sonar \
  -e SONAR_JDBC_PASSWORD=sonar \
  sonarqube:community
```

## Maven 集成

在 `pom.xml` 中配置 SonarQube 插件，或使用命令行扫描：

```bash
mvn sonar:sonar \
  -Dsonar.projectKey=tailor-is \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token=<YOUR_TOKEN>
```

## 质量门禁

建议配置以下质量门禁规则：

| 指标 | 阈值 |
|------|------|
| 覆盖率 | > 60% |
| 重复率 | < 3% |
| 安全漏洞（Blocker/Critical） | 0 |
| 代码异味（新增） | 0 |

## CI/CD 集成

在 CI 流程中添加 SonarQube 扫描步骤，确保合并前通过质量门禁检查。
