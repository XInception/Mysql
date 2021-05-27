package org.xinc.mysql;

import lombok.Getter;

/**
 * @author Admin
 */
@Getter
public enum Feature {
    //隐藏用户名密码 特性列表
    HIDE_USERNAME_AND_PASSWORD("隐藏用户名密码","true"),
    //记录请求响应日志
    RECORD_REQUEST_RESPONSE_LOG("记录请求响应日志","true");

    Feature(String desc, String defaultVal) {
        this.desc=desc;
        this.defaultVal=defaultVal;
    }

    String desc;

    String defaultVal;
}
