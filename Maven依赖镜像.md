当前环境：Maven 3.8.7、Java 17、Maven 路径 /usr/share/maven，下面给出一键执行 + 完整配置，直接复制命令即可完成阿里云镜像加速，解决依赖下载慢问题。

# 写入阿里云镜像（适配 SpringBoot3 / SpringCloud）
sudo tee /usr/share/maven/conf/settings.xml > /dev/null << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>

<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.2.0 https://maven.apache.org/xsd/settings-1.2.0.xsd">

  <!-- 本地仓库目录（可自行修改路径） -->
  <localRepository>/opt/maven/repository</localRepository>

  <mirrors>
    <!-- 阿里云公共仓库 主镜像 -->
    <mirror>
      <id>aliyun-public</id>
      <name>Aliyun Public Maven</name>
      <url>https://maven.aliyun.com/repository/public</url>
      <mirrorOf>central</mirrorOf>
    </mirror>

    <!-- Spring 专属仓库（适配 SpringBoot / SpringCloud） -->
    <mirror>
      <id>aliyun-spring</id>
      <name>Aliyun Spring Maven</name>
      <url>https://maven.aliyun.com/repository/spring</url>
      <mirrorOf>spring</mirrorOf>
    </mirror>

    <!-- 华为云 备用镜像 -->
    <mirror>
      <id>huawei-maven</id>
      <name>Huawei Cloud Maven</name>
      <url>https://mirrors.huaweicloud.com/repository/maven/</url>
      <mirrorOf>external:*,!central,!spring</mirrorOf>
    </mirror>
  </mirrors>

</settings>
EOF

