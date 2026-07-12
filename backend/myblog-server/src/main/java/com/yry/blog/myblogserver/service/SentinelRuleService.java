package com.yry.blog.myblogserver.service;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yry.blog.myblogserver.entity.SentinelRule;
import com.yry.blog.myblogserver.mapper.SentinelRuleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class SentinelRuleService extends ServiceImpl<SentinelRuleMapper, SentinelRule> {

    private static final Logger log = LoggerFactory.getLogger(SentinelRuleService.class);

    /**
     * 将数据库中的已启用规则同步到 Sentinel FlowRuleManager
     */
    public void syncToSentinel() {
        List<SentinelRule> enabledRules = list(new LambdaQueryWrapper<SentinelRule>()
                .eq(SentinelRule::getEnabled, 1));

        List<FlowRule> flowRules = new ArrayList<>();
        for (SentinelRule rule : enabledRules) {
            FlowRule flowRule = new FlowRule();
            flowRule.setResource(rule.getResource());
            flowRule.setGrade(rule.getGrade());
            flowRule.setCount(rule.getCount());
            flowRule.setLimitApp(rule.getLimitApp());
            flowRule.setStrategy(rule.getStrategy());

            int behavior = rule.getControlBehavior() != null ? rule.getControlBehavior() : RuleConstant.CONTROL_BEHAVIOR_DEFAULT;
            flowRule.setControlBehavior(behavior);
            if (behavior == RuleConstant.CONTROL_BEHAVIOR_WARM_UP) {
                flowRule.setWarmUpPeriodSec(rule.getWarmUpPeriodSec() != null ? rule.getWarmUpPeriodSec() : 10);
            }
            if (behavior == RuleConstant.CONTROL_BEHAVIOR_RATE_LIMITER) {
                flowRule.setMaxQueueingTimeMs(rule.getMaxQueueingTimeMs() != null ? rule.getMaxQueueingTimeMs() : 500);
            }

            flowRules.add(flowRule);
        }

        FlowRuleManager.loadRules(flowRules);
        log.info("已同步 {} 条限流规则到 Sentinel", flowRules.size());
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean createRule(SentinelRule rule) {
        if (rule.getGrade() == null) rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        if (rule.getCount() == null) rule.setCount(10.0);
        if (rule.getLimitApp() == null) rule.setLimitApp("default");
        if (rule.getStrategy() == null) rule.setStrategy(RuleConstant.STRATEGY_DIRECT);
        if (rule.getControlBehavior() == null) rule.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_DEFAULT);
        if (rule.getWarmUpPeriodSec() == null) rule.setWarmUpPeriodSec(10);
        if (rule.getMaxQueueingTimeMs() == null) rule.setMaxQueueingTimeMs(500);
        if (rule.getEnabled() == null) rule.setEnabled(1);

        boolean saved = save(rule);
        if (saved) syncToSentinel();
        return saved;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean updateRule(SentinelRule rule) {
        boolean updated = updateById(rule);
        if (updated) syncToSentinel();
        return updated;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean deleteRule(Long id) {
        boolean removed = removeById(id);
        if (removed) syncToSentinel();
        return removed;
    }
}
