package com.yry.blog.myblogserver.controller;

import com.yry.blog.myblogcommon.annotation.RequiresPermission;
import com.yry.blog.myblogserver.entity.SentinelRule;
import com.yry.blog.myblogserver.service.SentinelRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sentinel")
public class SentinelController {

    @Autowired
    private SentinelRuleService sentinelRuleService;

    @RequiresPermission("system:admin")
    @GetMapping("/rules")
    public ResponseEntity<Map<String, Object>> listRules() {
        List<SentinelRule> rules = sentinelRuleService.list();
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", rules);
        return ResponseEntity.ok(result);
    }

    @RequiresPermission("system:admin")
    @PostMapping("/rules")
    public ResponseEntity<Map<String, Object>> createRule(@RequestBody SentinelRule rule) {
        sentinelRuleService.createRule(rule);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "创建成功");
        return ResponseEntity.ok(result);
    }

    @RequiresPermission("system:admin")
    @PutMapping("/rules/{id}")
    public ResponseEntity<Map<String, Object>> updateRule(@PathVariable Long id, @RequestBody SentinelRule rule) {
        rule.setId(id);
        rule.setUpdateTime(LocalDateTime.now());
        sentinelRuleService.updateRule(rule);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "更新成功");
        return ResponseEntity.ok(result);
    }

    @RequiresPermission("system:admin")
    @DeleteMapping("/rules/{id}")
    public ResponseEntity<Map<String, Object>> deleteRule(@PathVariable Long id) {
        sentinelRuleService.deleteRule(id);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "删除成功");
        return ResponseEntity.ok(result);
    }
}
