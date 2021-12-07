package com.example.sns;

import android.app.Application;
import android.util.Log;

import com.kakao.sdk.common.KakaoSdk;
import com.kakao.sdk.common.util.Utility;
import com.nhn.android.naverlogin.OAuthLogin;

public class KakaoApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        KakaoSdk.init(this, "4309433de6aa50ed57287c86ce90b7af");

        // String keyHash = Utility.INSTANCE.getKeyHash(this);
        // Log.d("KakaoApplication", keyHash);
    }
}
