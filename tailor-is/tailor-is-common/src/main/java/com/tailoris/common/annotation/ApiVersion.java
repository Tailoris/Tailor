package com.tailoris.common.annotation;

import org.springframework.web.bind.annotation.Mapping;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Mapping
public @interface ApiVersion {

    String value() default "1.0";

    String deprecatedSince() default "";

    String replacement() default "";

    boolean deprecated() default false;
}