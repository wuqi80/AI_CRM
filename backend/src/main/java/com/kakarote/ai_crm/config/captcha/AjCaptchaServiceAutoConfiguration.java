package com.kakarote.ai_crm.config.captcha;

import com.anji.captcha.model.common.Const;
import com.anji.captcha.service.CaptchaService;
import com.anji.captcha.service.impl.CaptchaServiceFactory;
import com.anji.captcha.util.ImageUtils;
import com.anji.captcha.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.FileCopyUtils;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Slf4j
@Configuration
@EnableConfigurationProperties(AjCaptchaProperties.class)
public class AjCaptchaServiceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CaptchaService captchaService(AjCaptchaProperties prop) {
        Properties config = new Properties();
        config.put(Const.CAPTCHA_CACHETYPE, prop.getCacheType().name());
        config.put(Const.CAPTCHA_WATER_MARK, prop.getWaterMark());
        config.put(Const.CAPTCHA_FONT_TYPE, prop.getFontType());
        config.put(Const.CAPTCHA_TYPE, prop.getType().getCodeValue());
        config.put(Const.CAPTCHA_INTERFERENCE_OPTIONS, prop.getInterferenceOptions());
        config.put(Const.ORIGINAL_PATH_JIGSAW, prop.getJigsaw());
        config.put(Const.ORIGINAL_PATH_PIC_CLICK, prop.getPicClick());
        config.put(Const.CAPTCHA_SLIP_OFFSET, prop.getSlipOffset());
        config.put(Const.CAPTCHA_AES_STATUS, String.valueOf(prop.getAesStatus()));
        config.put(Const.CAPTCHA_WATER_FONT, prop.getWaterFont());
        config.put(Const.CAPTCHA_CACAHE_MAX_NUMBER, prop.getCacheNumber());
        config.put(Const.CAPTCHA_TIMING_CLEAR_SECOND, prop.getTimingClear());
        config.put(Const.HISTORY_DATA_CLEAR_ENABLE, prop.isHistoryDataClearEnable() ? "1" : "0");
        config.put(Const.REQ_FREQUENCY_LIMIT_ENABLE, prop.isReqFrequencyLimitEnable() ? "1" : "0");
        config.put(Const.REQ_GET_LOCK_LIMIT, String.valueOf(prop.getReqGetLockLimit()));
        config.put(Const.REQ_GET_LOCK_SECONDS, String.valueOf(prop.getReqGetLockSeconds()));
        config.put(Const.REQ_GET_MINUTE_LIMIT, String.valueOf(prop.getReqGetMinuteLimit()));
        config.put(Const.REQ_CHECK_MINUTE_LIMIT, String.valueOf(prop.getReqCheckMinuteLimit()));
        config.put(Const.REQ_VALIDATE_MINUTE_LIMIT, String.valueOf(prop.getReqVerifyMinuteLimit()));
        config.put(Const.CAPTCHA_FONT_SIZE, String.valueOf(prop.getFontSize()));
        config.put(Const.CAPTCHA_FONT_STYLE, String.valueOf(prop.getFontStyle()));
        config.put(Const.CAPTCHA_WORD_COUNT, String.valueOf(prop.getClickWordCount()));

        boolean isJigsaw = StringUtils.isNotBlank(prop.getJigsaw()) && prop.getJigsaw().startsWith("classpath:");
        boolean isPicClick = StringUtils.isNotBlank(prop.getPicClick()) && prop.getPicClick().startsWith("classpath:");
        if (isJigsaw || isPicClick) {
            config.put(Const.CAPTCHA_INIT_ORIGINAL, "true");
            initializeBaseMap(prop.getJigsaw(), prop.getPicClick());
        }
        return CaptchaServiceFactory.getInstance(config);
    }

    private static void initializeBaseMap(String jigsaw, String picClick) {
        ImageUtils.cacheBootImage(getResourcesImagesFile(jigsaw + "/original/*.png"),
                getResourcesImagesFile(jigsaw + "/slidingBlock/*.png"),
                getResourcesImagesFile(picClick + "/*.png"));
    }

    private static Map<String, String> getResourcesImagesFile(String path) {
        Map<String, String> imgMap = new HashMap<>();
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] resources = resolver.getResources(path);
            for (Resource resource : resources) {
                byte[] bytes = FileCopyUtils.copyToByteArray(resource.getInputStream());
                imgMap.put(resource.getFilename(), Base64.getEncoder().encodeToString(bytes));
            }
        } catch (Exception e) {
            log.warn("Load captcha base images failed: {}", e.getMessage());
        }
        return imgMap;
    }
}
