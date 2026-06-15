package com.tailoris.pattern.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.pattern.entity.Pattern;

import java.util.List;

public interface PatternService {

    Long createPattern(Pattern pattern);

    void updatePattern(Long id, Pattern pattern);

    void deletePattern(Long id);

    Pattern getPatternById(Long id);

    List<Pattern> listByMerchantId(Long merchantId);

    Page<Pattern> pagePatterns(int pageNum, int pageSize);
}