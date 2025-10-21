package com.example.mybatis.common;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;

public class CheckParamUtils {

    // 检查字符串是否为空
    public static void isNotNull(String field, String fieldDes) {
        Preconditions.checkArgument(!StringUtils.isBlank(field), fieldDes + " 不能为空");
    }

    // 检查 Long 值是否大于 0
    public static void isBiggerZero(Long field, String fieldDes) {
        Preconditions.checkArgument(field != null && field > 0, fieldDes + " 不能为空且必须大于 0");
    }

    // 检查 Integer 值是否大于 0
    public static void isBiggerZero(Integer field, String fieldDes) {
        Preconditions.checkArgument(field != null && field > 0, fieldDes + " 不能为空且必须大于 0");
    }

    // 检查是否为正数，通用方法
    public static void isPositive(Number field, String fieldDes) {
        Preconditions.checkArgument(field != null && field.doubleValue() > 0, fieldDes + " 必须大于 0");
    }
}
