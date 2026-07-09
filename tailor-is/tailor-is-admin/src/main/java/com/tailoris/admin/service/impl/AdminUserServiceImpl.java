package com.tailoris.admin.service.impl;

import com.tailoris.admin.constant.AdminConstants;
import com.tailoris.api.admin.dto.UserQueryRequest;
import com.tailoris.admin.service.AdminUserService;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.api.user.entity.SysUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public PageResponse<SysUser> listUsers(UserQueryRequest request) {
        StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM tailor_is_user.sys_user WHERE 1=1");
        StringBuilder querySql = new StringBuilder("SELECT * FROM tailor_is_user.sys_user WHERE 1=1");
        
        List<Object> params = new java.util.ArrayList<>();

        if (request.getStatus() != null) {
            countSql.append(" AND status = ?");
            querySql.append(" AND status = ?");
            params.add(request.getStatus());
        }
        if (StringUtils.hasText(request.getKeyword())) {
            countSql.append(" AND (username LIKE ? OR phone LIKE ? OR nick_name LIKE ?)");
            querySql.append(" AND (username LIKE ? OR phone LIKE ? OR nick_name LIKE ?)");
            String likeKeyword = "%" + request.getKeyword() + "%";
            params.add(likeKeyword);
            params.add(likeKeyword);
            params.add(likeKeyword);
        }

        querySql.append(" ORDER BY created_at DESC LIMIT ?, ?");
        
        Long total = jdbcTemplate.queryForObject(countSql.toString(), Long.class, params.toArray());
        
        List<Object> queryParams = new java.util.ArrayList<>(params);
        queryParams.add((request.getPageNum() - 1) * request.getPageSize());
        queryParams.add(request.getPageSize());
        
        List<SysUser> records = jdbcTemplate.query(querySql.toString(), 
                new BeanPropertyRowMapper<>(SysUser.class), queryParams.toArray());

        return new PageResponse<>(records, total, request.getPageNum(), request.getPageSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void freezeUser(Long userId) {
        SysUser user = getUserDetail(userId);
        if (user.getStatus().equals(AdminConstants.USER_STATUS_FROZEN)) {
            throw new BusinessException("用户已处于冻结状态");
        }
        jdbcTemplate.update("UPDATE tailor_is_user.sys_user SET status = ?, update_time = NOW() WHERE id = ?",
                AdminConstants.USER_STATUS_FROZEN, userId);
        stringRedisTemplate.delete("tailoris:token:" + userId);
        log.info("用户已冻结, userId: {}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfreezeUser(Long userId) {
        SysUser user = getUserDetail(userId);
        if (!user.getStatus().equals(AdminConstants.USER_STATUS_FROZEN)) {
            throw new BusinessException("用户未处于冻结状态");
        }
        jdbcTemplate.update("UPDATE tailor_is_user.sys_user SET status = ?, update_time = NOW() WHERE id = ?",
                AdminConstants.USER_STATUS_NORMAL, userId);
        log.info("用户已解冻, userId: {}", userId);
    }

    @Override
    public SysUser getUserDetail(Long userId) {
        List<SysUser> users = jdbcTemplate.query(
                "SELECT * FROM tailor_is_user.sys_user WHERE id = ?",
                new BeanPropertyRowMapper<>(SysUser.class), userId);
        if (users.isEmpty()) {
            throw new BusinessException("用户不存在");
        }
        return users.get(0);
    }
}
