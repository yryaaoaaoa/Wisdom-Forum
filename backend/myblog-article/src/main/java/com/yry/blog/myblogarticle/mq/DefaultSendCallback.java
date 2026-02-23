package com.yry.blog.myblogarticle.mq;

import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import lombok.extern.slf4j.Slf4j;

// 通用的空实现 SendCallback
@Slf4j
public class DefaultSendCallback implements SendCallback {
    // 空实现 onSuccess，也可以加默认日志
    @Override
    public void onSuccess(SendResult sendResult) {
        log.debug("消息发送成功，msgId:{}", sendResult.getMsgId());
    }

    // 空实现 onException，也可以加默认日志
    @Override
    public void onException(Throwable e) {
        log.error("消息发送失败", e);
    }
}