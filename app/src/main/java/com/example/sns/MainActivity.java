package com.example.sns;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.kakao.sdk.auth.model.OAuthToken;
import com.kakao.sdk.user.UserApi;
import com.kakao.sdk.user.UserApiClient;
import com.kakao.sdk.user.model.User;
import com.nhn.android.naverlogin.OAuthLogin;
import com.nhn.android.naverlogin.OAuthLoginHandler;
import com.nhn.android.naverlogin.ui.view.OAuthLoginButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import kotlin.Unit;
import kotlin.jvm.functions.Function2;
import retrofit2.http.Url;

public class MainActivity extends AppCompatActivity {
    String TAG = "MainActivity";

    private UserApiClient mKakao;           // 카카오톡 모듈
    private OAuthLogin mOAuthLoginModule;   // 네이버 모듈
    private Context mContext;

    private ImageView   btnLoginWithKakao;
    private OAuthLoginButton      btnLoginWithNaver;
    private Button      btnLogout;
    private ImageView   profile;
    private TextView    nickname;

    private String      accessToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mOAuthLoginModule = OAuthLogin.getInstance();
        mOAuthLoginModule.init(this
                ,getString(R.string.naver_client_id)
                ,getString(R.string.naver_client_secret)
                ,getString(R.string.app_name)
        );

        mContext = this;

        setValues();
        setupEvents();

        btnLogout.setVisibility(View.INVISIBLE);
    }

    private void setValues() {
        btnLoginWithKakao       = findViewById(R.id.btnLoginWithKakao);
        btnLoginWithNaver       = findViewById(R.id.btnLoginWithNaver);
        btnLoginWithNaver.setOAuthLoginHandler(mOAuthLoginHandler);

        btnLogout               = findViewById(R.id.btnLogout);
        profile                 = findViewById(R.id.profile);
        nickname                = findViewById(R.id.nickname);

        mKakao                  = UserApiClient.getInstance();
    }

    private void setupEvents() {
        // 카카오 로그인
        btnLoginWithKakao.setOnClickListener(view -> {
            Log.d(TAG, "[Logging] 카카오톡 로그인 버튼 클릭");

            if( mKakao.isKakaoTalkLoginAvailable(this) ) {
                mKakao.loginWithKakaoTalk(this, callback);
            } else {
                mKakao.loginWithKakaoAccount(this, callback);
            }

        });

        // 네이버 로그인
        btnLoginWithNaver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "[Logging] 네이버 로그인 버튼 클릭");
                mOAuthLoginModule.startOauthLoginActivity(MainActivity.this, mOAuthLoginHandler);
            }
        });

        // 로그아웃
        btnLogout.setOnClickListener(view -> {
            btnLoginWithKakao.setVisibility(View.VISIBLE);
            btnLogout.setVisibility(View.INVISIBLE);

            Glide.with(profile).clear(profile);
            nickname.setText("");

            mKakao.logout(throwable -> {
                if(throwable != null) {
                    Log.e(TAG, "[Logging] LOGOUT throwable : " + throwable.getMessage());
                }

                return null;
            });
        });
    }

    private final OAuthLoginHandler mOAuthLoginHandler = new OAuthLoginHandler() {
        @Override
        public void run(boolean success) {
            if (success) {
                accessToken = mOAuthLoginModule.getAccessToken(mContext);
                String refreshToken = mOAuthLoginModule.getRefreshToken(mContext);
                long expiresAt = mOAuthLoginModule.getExpiresAt(mContext);
                String tokenType = mOAuthLoginModule.getTokenType(mContext);

                Log.d(TAG, "[Logging] accessToken : " + accessToken);
                Log.d(TAG, "[Logging] refreshToken : " + refreshToken);
                Log.d(TAG, "[Logging] expiresAt : " + expiresAt);
                Log.d(TAG, "[Logging] tokenType : " + tokenType);

                new GetUserTask().execute(accessToken);



            } else {
                String errorCode = mOAuthLoginModule.getLastErrorCode(mContext).getCode();
                String errorDesc = mOAuthLoginModule.getLastErrorDesc(mContext);

                Log.e(TAG, "[Logging] errorCode : " + errorCode);
                Log.e(TAG, "[Logging] errorDesc : " + errorDesc);

            }
        }
    };

    private class GetUserTask extends AsyncTask {

        @Override
        protected Object doInBackground(Object[] objects) {
            String header   = "Bearer " + accessToken;
            String apiURL   = "https://openapi.naver.com/v1/nid/me";

            Log.d(TAG,"header : " + header);

            Map<String, String> requestHeaders = new HashMap<>();
            requestHeaders.put("Authorization", header);
            String responseBody = get(apiURL,requestHeaders);

            Log.d(TAG,"[Loggind] 네이버 유저 조회 : " + responseBody);

            return responseBody;
        }

        @Override
        protected void onPostExecute(Object o) {
            try {
                JSONObject jsonObject = new JSONObject(String.valueOf(o));
                if(jsonObject.getString("resultcode").equals("00")){
                    JSONObject object = new JSONObject(jsonObject.getString("response"));
                    String nickname = object.getString("nickname");

                    Log.d(TAG,"[Loggind] 네이버 nickname 조회 : " + nickname);
                    // model = new NaverUserModel(nickname, email, gender, birthday);
                }
                // loginDoneActivity();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        private String get(String apiUrl, Map<String, String> requestHeaders){
            HttpURLConnection con = connect(apiUrl);
            try {
                con.setRequestMethod("GET");
                for(Map.Entry<String, String> header :requestHeaders.entrySet()) {
                    con.setRequestProperty(header.getKey(), header.getValue());
                }

                int responseCode = con.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) { // 정상 호출
                    return readBody(con.getInputStream());
                } else { // 에러 발생
                    return readBody(con.getErrorStream());
                }
            } catch (IOException e) {
                throw new RuntimeException("API 요청과 응답 실패", e);
            } finally {
                con.disconnect();
            }
        }

        private String readBody(InputStream body){
            InputStreamReader streamReader = new InputStreamReader(body);

            try (BufferedReader lineReader = new BufferedReader(streamReader)) {
                StringBuilder responseBody = new StringBuilder();

                String line;
                while ((line = lineReader.readLine()) != null) {
                    responseBody.append(line);
                }

                return responseBody.toString();
            } catch (IOException e) {
                throw new RuntimeException("API 응답을 읽는데 실패했습니다.", e);
            }
        }

        private HttpURLConnection connect(String apiUrl){
            try {
                URL url = new URL(apiUrl);
                return (HttpURLConnection)url.openConnection();
            } catch (MalformedURLException e) {
                throw new RuntimeException("API URL이 잘못되었습니다. : " + apiUrl, e);
            } catch (IOException e) {
                throw new RuntimeException("연결이 실패했습니다. : " + apiUrl, e);
            }
        }

    }



    Function2<OAuthToken, Throwable, Unit> callback = new Function2<OAuthToken, Throwable, Unit>() {
        @Override
        public Unit invoke(OAuthToken oAuthToken, Throwable throwable) {
            if(oAuthToken != null) {
                Log.d(TAG, "[Logging] oAuthToken : " + oAuthToken.getAccessToken());
            }

            if(throwable != null) {
                Log.e(TAG, "[Logging] throwable : " + throwable.getMessage());
            }
            loginWithKakao();
            return null;
        }
    };
    private void loginWithKakao() {
        mKakao.me((user, throwable) -> {
            if(user != null) {
                Log.d(TAG,"[Logging] 카톡 정보 있음!!");
                btnLoginWithKakao.setVisibility(View.INVISIBLE);
                btnLogout.setVisibility(View.VISIBLE);

                Log.d(TAG,"카톡 ID : " + user.getId());
                Log.d(TAG,"카톡 닉네임 : " + user.getKakaoAccount().getProfile().getNickname());
                nickname.setText(user.getKakaoAccount().getProfile().getNickname());

                String thumbnailImageUrl = user.getKakaoAccount().getProfile().getThumbnailImageUrl();
                Glide.with(profile).load(thumbnailImageUrl).circleCrop().into(profile);
            } else {
                btnLoginWithKakao.setVisibility(View.VISIBLE);
                btnLogout.setVisibility(View.INVISIBLE);
            }
            return null;
        });
    }
}