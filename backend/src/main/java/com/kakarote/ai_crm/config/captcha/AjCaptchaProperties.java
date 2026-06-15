package com.kakarote.ai_crm.config.captcha;

import com.anji.captcha.model.common.CaptchaTypeEnum;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.awt.Font;

@Getter
@Setter
@ToString
@ConfigurationProperties(AjCaptchaProperties.PREFIX)
public class AjCaptchaProperties {

    public static final String PREFIX = "wukong.captcha";

    private CaptchaTypeEnum type = CaptchaTypeEnum.DEFAULT;
    private String jigsaw = "";
    private String picClick = "";
    private String waterMark = "";
    private String waterFont = "WenQuanZhengHei.ttf";
    private String fontType = "WenQuanZhengHei.ttf";
    private String slipOffset = "5";
    private Boolean aesStatus = true;
    private String interferenceOptions = "0";
    private String cacheNumber = "1000";
    private String timingClear = "180";
    private StorageType cacheType = StorageType.redis;
    private boolean historyDataClearEnable = false;
    private boolean reqFrequencyLimitEnable = false;
    private int reqGetLockLimit = 5;
    private int reqGetLockSeconds = 300;
    private int reqGetMinuteLimit = 100;
    private int reqCheckMinuteLimit = 100;
    private int reqVerifyMinuteLimit = 100;
    private int fontStyle = Font.BOLD;
    private int fontSize = 25;
    private int clickWordCount = 4;
    private boolean loginRequired = false;

    public enum StorageType {
        local,
        redis,
        other
    }
}
