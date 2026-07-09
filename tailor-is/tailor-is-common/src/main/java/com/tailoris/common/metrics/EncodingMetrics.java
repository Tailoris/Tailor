package com.tailoris.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class EncodingMetrics {

    private final Counter invalidUtf8Requests;
    private final Counter mojibakeDetected;
    private final Counter charsetConversionErrors;
    private final Counter encodingValidationPassed;

    public EncodingMetrics(MeterRegistry registry) {
        this.invalidUtf8Requests = Counter.builder("encoding.invalid_utf8_requests_total")
                .description("Total number of requests containing invalid UTF-8 characters")
                .tags("service", "tailor-is")
                .register(registry);

        this.mojibakeDetected = Counter.builder("encoding.mojibake_detected_total")
                .description("Total number of requests containing mojibake (garbled characters)")
                .tags("service", "tailor-is")
                .register(registry);

        this.charsetConversionErrors = Counter.builder("encoding.charset_conversion_errors_total")
                .description("Total number of charset conversion errors")
                .tags("service", "tailor-is")
                .register(registry);

        this.encodingValidationPassed = Counter.builder("encoding.validation_passed_total")
                .description("Total number of requests that passed encoding validation")
                .tags("service", "tailor-is")
                .register(registry);
    }

    public void incrementInvalidUtf8Requests() {
        invalidUtf8Requests.increment();
    }

    public void incrementMojibakeDetected() {
        mojibakeDetected.increment();
    }

    public void incrementCharsetConversionErrors() {
        charsetConversionErrors.increment();
    }

    public void incrementEncodingValidationPassed() {
        encodingValidationPassed.increment();
    }
}