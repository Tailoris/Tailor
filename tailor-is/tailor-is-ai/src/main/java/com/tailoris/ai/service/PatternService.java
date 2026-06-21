package com.tailoris.ai.service;

import com.tailoris.ai.dto.PatternCheckRequest;
import com.tailoris.ai.dto.PatternGenerateRequest;
import com.tailoris.ai.dto.PatternIterationRequest;
import com.tailoris.ai.entity.PatternIteration;
import com.tailoris.ai.entity.PatternRecord;
import com.tailoris.ai.entity.PatternVersion;

import java.util.List;

public interface PatternService {

    PatternRecord generatePattern(Long userId, PatternGenerateRequest request);

    String checkPattern(Long userId, PatternCheckRequest request);

    PatternIteration iteratePattern(Long userId, PatternIterationRequest request);

    PatternVersion saveVersion(Long userId, Long patternId, String versionName, String changeDescription);

    List<PatternVersion> listVersions(Long userId, Long patternId);

    String exportPattern(Long userId, Long patternId, String format);

    PatternRecord getPatternDetail(Long userId, Long patternId);

    List<PatternRecord> listUserPatterns(Long userId);
}
