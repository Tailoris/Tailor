#!/bin/bash
# Tailor IS 告警响应手册 (Runbook)
# 用法: alert-response-handbook.sh [alert_name]

show_help() {
    echo "Tailor IS 告警响应手册"
    echo "用法: $0 [alert_name]"
    echo ""
    echo "可用告警:"
    echo "  database-down       - 数据库宕机处理"
    echo "  redis-down          - Redis缓存故障"
    echo "  high-cpu            - CPU使用率过高"
    echo "  high-memory         - 内存使用率过高"
    echo "  high-disk           - 磁盘空间不足"
    echo "  high-error-rate     - API错误率过高"
    echo "  slow-response       - 响应时间过长"
    echo ""
}

case ${1:-help} in
    database-down)
        echo "==== 数据库宕机应急响应 ===="
        echo "1. 确认状态: systemctl status mysql"
        echo "2. 检查错误日志: tail -100 /var/log/mysql/error.log"
        echo "3. 尝试重启: systemctl restart mysql"
        echo "4. 如无法启动, 检查磁盘空间: df -h /var/lib/mysql"
        echo "5. 检查配置文件: /etc/mysql/my.cnf"
        echo "6. 联系DBA或升级告警"
        ;;
    redis-down)
        echo "==== Redis缓存故障应急响应 ===="
        echo "1. 确认状态: redis-cli -h host ping"
        echo "2. 检查内存配置: redis-cli info memory"
        echo "3. 检查持久化文件: ls -lh /data/redis/"
        echo "4. 尝试重启服务"
        echo "5. 监控数据库压力 (Redis宕机后DB压力剧增)"
        ;;
    high-cpu)
        echo "==== CPU使用率过高 ===="
        echo "1. 定位高CPU进程: top -c"
        echo "2. 检查Java进程: jstack <pid> | head -100"
        echo "3. 检查慢查询: tail -100 /var/log/mysql/slow.log"
        echo "4. 检查GC日志"
        echo "5. 考虑扩容或优化热点代码"
        ;;
    high-memory)
        echo "==== 内存使用率过高 ===="
        echo "1. 定位高内存进程: ps aux --sort=-%mem | head -10"
        echo "2. 检查JVM堆内存: jmap -heap <pid>"
        echo "3. 分析GC日志"
        echo "4. 考虑调整JVM参数或增加内存"
        ;;
    high-disk)
        echo "==== 磁盘空间不足 ===="
        echo "1. 查看磁盘使用: df -h"
        echo "2. 定位大文件: du -h / | grep '[0-9\.]G' | sort -hk 2"
        echo "3. 清理日志: logrotate"
        echo "4. 清理旧备份: 按保留策略清理"
        echo "5. 考虑扩容"
        ;;
    high-error-rate)
        echo "==== API错误率过高 ===="
        echo "1. 检查错误日志: tail -100 /var/log/app/error.log"
        echo "2. 检查依赖服务状态"
        echo "3. 检查数据库连接"
        echo "4. 考虑回滚最近发布"
        echo "5. 如服务严重异常, 启动应急降级预案"
        ;;
    slow-response)
        echo "==== 响应时间过长 ===="
        echo "1. 检查慢查询日志"
        echo "2. 检查Redis命中率"
        echo "3. 分析应用性能: profiler / APM"
        echo "4. 检查网络延迟"
        echo "5. 考虑水平扩容"
        ;;
    *)
        show_help
        ;;
esac
