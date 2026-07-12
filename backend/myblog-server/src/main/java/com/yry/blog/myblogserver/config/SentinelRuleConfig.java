package com.yry.blog.myblogserver.config;

import com.yry.blog.myblogserver.service.SentinelRuleService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SentinelRuleConfig {

    @Autowired
    private SentinelRuleService sentinelRuleService;

    @PostConstruct
    public void init() {
        sentinelRuleService.syncToSentinel();
    }
}
