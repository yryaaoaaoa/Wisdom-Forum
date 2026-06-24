package com.yry.blog.myblogadmin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yry.blog.myblogcommon.entity.config.SysConfig;

public interface SysConfigService extends IService<SysConfig> {
    String getConfigValue(String key);
    void setConfigValue(String key, String value);
}
