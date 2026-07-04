package com.tailoris.common.interceptor;

/**
 * 数据权限上下文 - 用于在线程内传递当前操作的表名
 * <p>
 * 作为 DataPermissionHandlerImpl 通过 mappedStatementId 获取表名的补充机制，
 * 当 MyBatis Plus 内部机制无法自动识别表名时，可通过此上下文手动设置。
 * </p>
 *
 * @author Tailoris
 */
public class DataPermissionContext {

    private static final ThreadLocal<String> CURRENT_TABLE = new ThreadLocal<>();

    /**
     * 设置当前线程操作的表名
     *
     * @param tableName 表名
     */
    public static void setCurrentTable(String tableName) {
        CURRENT_TABLE.set(tableName);
    }

    /**
     * 获取当前线程操作的表名
     *
     * @return 表名，可能为 null
     */
    public static String getCurrentTable() {
        return CURRENT_TABLE.get();
    }

    /**
     * 清除当前线程的表名，防止内存泄漏
     */
    public static void clear() {
        CURRENT_TABLE.remove();
    }
}