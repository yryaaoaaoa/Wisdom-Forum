package com.yry.blog.myblogcommon.enums;
import lombok.Getter;

@Getter
public enum BizType {

    // 文章相关
    ARTICLE_COVER("ARTICLE_COVER", "文章封面图"),
    ARTICLE_CONTENT("ARTICLE_CONTENT", "文章正文文件"),
    ARTICLE_ATTACHMENT("ARTICLE_ATTACHMENT", "文章附件"),

    // 用户相关
    USER_AVATAR("USER_AVATAR", "用户头像"),
    USER_BACKGROUND("USER_BACKGROUND", "用户背景图"),

    // 商品相关
    PRODUCT_IMAGE("PRODUCT_IMAGE", "商品图片"),

    // 其他
    COMMON_FILE("COMMON_FILE", "通用文件");

    private final String value;
    private final String description;

    BizType(String value, String description) {
        this.value = value;
        this.description = description;
    }

    // 提供一个根据 value 获取枚举实例的静态方法（非常有用）
    public static BizType getByValue(String value) {
        for (BizType type : BizType.values()) {
            if (type.getValue().equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的业务类型: " + value);
    }
}
