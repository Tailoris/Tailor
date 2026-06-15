package com.tailoris.pattern.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tailoris.pattern.entity.Pattern;
import com.tailoris.pattern.mapper.PatternMapper;
import com.tailoris.pattern.service.PatternService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatternServiceImpl extends ServiceImpl<PatternMapper, Pattern> implements PatternService {

    private final PatternMapper patternMapper;

    @Override
    public Long createPattern(Pattern pattern) {
        patternMapper.insert(pattern);
        return pattern.getId();
    }

    @Override
    public void updatePattern(Long id, Pattern pattern) {
        pattern.setId(id);
        patternMapper.updateById(pattern);
    }

    @Override
    public void deletePattern(Long id) {
        patternMapper.deleteById(id);
    }

    @Override
    public Pattern getPatternById(Long id) {
        return patternMapper.selectById(id);
    }

    @Override
    public List<Pattern> listByMerchantId(Long merchantId) {
        LambdaQueryWrapper<Pattern> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Pattern::getMerchantId, merchantId);
        return patternMapper.selectList(wrapper);
    }

    @Override
    public Page<Pattern> pagePatterns(int pageNum, int pageSize) {
        Page<Pattern> page = new Page<>(pageNum, pageSize);
        return patternMapper.selectPage(page, new LambdaQueryWrapper<>());
    }
}