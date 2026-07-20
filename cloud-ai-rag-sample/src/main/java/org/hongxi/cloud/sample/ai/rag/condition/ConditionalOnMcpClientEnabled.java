package org.hongxi.cloud.sample.ai.rag.condition;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnMcpClientEnabledCondition.class)
public @interface ConditionalOnMcpClientEnabled {
}
