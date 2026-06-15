package com.tailoris.common.web;

/**
 * Tailor IS Common Web Module
 * 
 * 该模块提供 Web 相关的公共组件和配置。
 * 
 * @author Tailor IS Team
 * @since 1.0.0
 */
public class CommonWebModule {
    
    public static final String MODULE_NAME = "tailor-is-common-web";
    public static final String VERSION = "1.0.0";
    
    /**
     * 获取模块信息
     * @return 模块名称和版本
     */
    public static String getModuleInfo() {
        return MODULE_NAME + " v" + VERSION;
    }
}
