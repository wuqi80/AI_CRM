package com.kakarote.ai_crm;

import com.alipay.easysdk.factory.Factory;
import com.alipay.easysdk.kernel.Config;
import com.alipay.easysdk.payment.page.models.AlipayTradePagePayResponse;

public class PayTest {

    public static void main(String[] args) {
        try {
            Factory.setOptions(getOptions());
            AlipayTradePagePayResponse pay = Factory.Payment.Page()
                    .optional("qr_pay_mode", 4)
                    .optional("qrcode_width", 150)
                    .optional("integration_type", "PCWEB")
                    .optional("product_code", "FAST_INSTANT_TRADE_PAY")
                    .asyncNotify("https://aicrm-saas.5kcrm.cn/crmapi/tokenPurchase/notify/alipay")
                    .pay("subject", "811274425168181", "0.01", "");
            System.out.println(pay.body);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static Config getOptions() {
        Config config = new Config();
        // 协议类型
        config.protocol = "https";
        // 签名方式
        config.signType = "RSA2";
        //支付宝网关
        config.gatewayHost = "openapi.alipay.com";
        // AppId
        config.appId = "2021003113634777";
        // 应用私钥
        config.merchantPrivateKey = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCNmEJKVEyKZ3Dk3rkIGdZtHaqzfzVUkMv6gGrHRkFNhUEE5Li312JG4Cep0Ob41JjBMMepVT3ChhNCDnZEamOSo5HYps0aIRpdPOJXoLC0xD32G1zz7hsZdr36px4H4jjP1r3CLRtOy+e7n+8U6qscmefdCYvOSuSqLCOlT3sqI6xZSIHwUT6y2D7yi5Ewjyb5SzG7sHxwImBxoZXY55gcs7PGJMUj5DJLHe0uAskrQLpXviHtoLcovK6BmKbkTLkVDTB4hvdPirhSSimgZAk6AI7TuBchMqiNcwm/PGDOL8RxBnEq/LhQzWCKZ2pCqoqLS6gIPySnfkmqB17sXCg5AgMBAAECggEAWBh+Tg6q3IXFSLCGANNKhPMrTKh9vtKkBoQ1ayx73sZkkK3Ze5BHNf+qzc4KlpB3ReOapfFCDrAqdTPYQ3LJ5udcC6aj/3jPFWmXDZwNtTpPrrqRl7byJ2KoirXFSnRmknFx+Mjlr53P3P2B28qxkYb0KqMQ19Xi9TmnJ+gOokVIL+o1ubmmQyM6r7WDsPCrBOSIapbw1aw9pYN86LFTxkU2S3T+x+c8k8E4Hc5ccUyIEPzxT3krd2wRxJBOv3Ig5lOFLfHVcJSZFBq0bKrn+lhJeMkbR8/VaZClrBzJvtgfKYmzoSQgLWqMpZaXWJIjtw39Er2OXraq7aenkfHwMQKBgQDMCu1F3o9d+J88DjDH/OPAUVh+SXxTujYaO8QHsILnoMi8FpkY9sUNqidpyOyOtzt0mWIAgwTXESkUkTCgEMYnP2zfKkJ/99egkLcvqVQHNw7aEVF+SYgCE+ZO0wYUC3SXMSS1ooUXUyglz9K91pSZ8Q+nNfVs7V7qOk1Ve4gLDQKBgQCxpn7pGFJ37jEdPh8Os0zMiabJk5iY9rvkKBVnqGOBkYP2FJ55iSGAL6PPLKqg18Cg/W6kvpdpbWxng30rkzYA7Vl5ySDnNS5R+8aebjrdu8vRPmt3+DAR+545+RlDahzsqPHURu3XkK4TQYUdvT/O9J6VzWFvxn84bunDdvWW3QKBgHqsjx8Uwglzcvj+ZyG1z5BO1++FavRD3kdDa4Zb/zgXmNhMldtGcg7XID8fh+n+DAb8OMmp+/z52kL9T8jMjPYH0gMSO00Iy2FD4H8NRPHZjlpZUy+kMdtd73ews9TVVlGYQEPb5DyzyE2Dd2afXlR3dDvJ2zaUOB6G0vvz+0T5AoGBAIe/oVS++C2avr8IvDTAgBv/x4pbFY01ZSgYFRsrijX1GvBMmzhluEq6nKLwD8Zud0zNu+F5nju6mM6UpG5KxEZ7GBc+wLzMX4PYL9LainLrUPioiPiYlyr2qdbE6TAhgs643moGdTJ25P3j4DN0bLDZLeFsmlsORCK8ZjatzUalAoGBAKE3YqQOMeC0nuodF1Xn82mK/hLft4jJs6lHMl/fhiVhUjKz/jevP4sM0ccdie7VMmWpfF3U+/oJTYlW7us/Ce6nfyJRwEEViAobpQ6MwiIxut5B46Kju2dLGjPRXKt3gFuoeAn81CDiukuPNlhjtUepDuyzmQJK2NmI1NvVVEaq";
        // 支付宝公钥 如果采用非证书模式，则无需赋值三个证书路径，改为赋值支付宝公钥字符串即可
        config.alipayPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAm5qUEzzam69ZB/0DXAw8FmBEKhjUmFVls+N3FuhO6e7IBbp0LzAGvT0vclttMOeZtavfZuqmrZzgfK7vo1Lp8wnANQx9Qg8tzr3a5sdNLzupHqIae22eO1twG27YU0toQCqIx576JLpkriTY/4Sx7uV1cpAcChfIzMKvYdTIN608BKfB0D7FMX3UFL/+i4lcU1CUkDbxbWg1Tspbw17+RU1b8BwtjYPvhHjHCiADTs3QoPrvfQ7CcoGOXZ4XCOTV9aT9TP43YiP2QbLATXkHM618lJjzyxHcGkmBjxRyUMvbKvuylz9UPtUccUvwUWJ/BFj2Gg1b25YNHLiAf8pZxQIDAQAB";
        return config;
    }
}
