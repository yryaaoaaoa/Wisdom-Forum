package com.yry.blog.myblogadmin.service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yry.blog.myblogadmin.mapper.SysConfigMapper;
import com.yry.blog.myblogadmin.service.SysConfigService;
import com.yry.blog.myblogcommon.entity.config.SysConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements SysConfigService {

    @Override
    public String getConfigValue(String key) {
        SysConfig config = this.lambdaQuery()
                .eq(SysConfig::getConfigKey, key)
                .one();
        return config != null ? config.getConfigValue() : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setConfigValue(String key, String value) {
        SysConfig existing = this.lambdaQuery()
                .eq(SysConfig::getConfigKey, key)
                .one();

        if (existing != null) {
            existing.setConfigValue(value);
            this.updateById(existing);
        } else {
            SysConfig config = SysConfig.builder()
                    .configKey(key)
                    .configValue(value)
                    .build();
            this.save(config);
        }
    }
}
