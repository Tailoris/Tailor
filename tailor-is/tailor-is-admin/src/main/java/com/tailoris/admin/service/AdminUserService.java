package com.tailoris.admin.service;

import com.tailoris.api.admin.dto.UserQueryRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.api.user.entity.SysUser;

public interface AdminUserService {

    PageResponse<SysUser> listUsers(UserQueryRequest request);

    void freezeUser(Long userId);

    void unfreezeUser(Long userId);

    SysUser getUserDetail(Long userId);
}
