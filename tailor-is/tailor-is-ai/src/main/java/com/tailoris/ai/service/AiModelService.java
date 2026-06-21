package com.tailoris.ai.service;

import com.tailoris.ai.dto.PatternGenerateResponse;
import com.tailoris.ai.model.PatternRequest;

public interface AiModelService {

    PatternGenerateResponse generatePattern(PatternRequest request);

    String checkStructure(byte[] pattern);

    PatternGenerateResponse iteratePattern(Long patternId, String feedback);

    String exportPattern(Long patternId, String format);
}