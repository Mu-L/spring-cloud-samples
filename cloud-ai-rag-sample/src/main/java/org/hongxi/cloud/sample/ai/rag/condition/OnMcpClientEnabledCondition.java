package org.hongxi.cloud.sample.ai.rag.condition;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class OnMcpClientEnabledCondition extends SpringBootCondition {
    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Boolean mcpClientEnabled = context.getEnvironment().getProperty("spring.ai.mcp.client.enabled", Boolean.class);
        if (Boolean.TRUE.equals(mcpClientEnabled)) {
            return ConditionOutcome.match("spring.ai.mcp.client.enabled is true");
        }
        return ConditionOutcome.noMatch("spring.ai.mcp.client.enabled is false");
    }
}
