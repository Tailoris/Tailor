package com.tailoris.ai.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 模型路由枚举。
 *
 * <p>定义纸样生成请求的路由目标：本地轻量模型或云端分布式模型。</p>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum ModelRoute {

    /** 本地轻量模型：常规体型，低延迟 */
    LOCAL("local", "本地轻量模型"),

    /** 云端分布式模型：特殊体型/热门款式，高精度 */
    CLOUD("cloud", "云端分布式模型");

    private final String code;
    private final String description;
}
