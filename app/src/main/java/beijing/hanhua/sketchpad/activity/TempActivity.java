package beijing.hanhua.sketchpad.activity;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.SynthesizerListener;

import beijing.hanhua.sketchpad.R;

/**
 * 科大讯飞语音播报测试
 */
public class TempActivity extends AppCompatActivity implements InitListener, SynthesizerListener {
    private static final String TAG = "TempActivity";

    private SpeechSynthesizer mSynthesizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_temp);
        SpeechUtility.createUtility(this, "557e3b39");

        mSynthesizer = SpeechSynthesizer.createSynthesizer(this, this);
        setParams();

        // 设置合成音频保存路径，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        mSynthesizer.setParameter(SpeechConstant.PARAMS, "tts_audio_path=" + Environment.getExternalStorageDirectory() + "/test.pcm");

    }

    private void setParams() {
        mSynthesizer.setParameter(SpeechConstant.VOICE_NAME, "");

        // 设置发音人
        mSynthesizer.setParameter(SpeechConstant.SPEED, "50");// 设置语速

        mSynthesizer.setParameter(SpeechConstant.VOLUME, "80");// 设置音量，范围0~100

        // mTts.setParameter(SpeechConstant.T, "80");

        mSynthesizer.setParameter(SpeechConstant.STREAM_TYPE, "3");
    }

    private void setParams2() {
        // 清空参数
        mSynthesizer.setParameter(SpeechConstant.PARAMS, null);
        mSynthesizer.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
        // 设置本地合成发音人 voicer为空，默认通过语音+界面指定发音人。
        mSynthesizer.setParameter(SpeechConstant.VOICE_NAME, "");
        //设置合成语速
        mSynthesizer.setParameter(SpeechConstant.SPEED, "40");
        //设置合成音调
        mSynthesizer.setParameter(SpeechConstant.PITCH, "50");
        //设置合成音量
        mSynthesizer.setParameter(SpeechConstant.VOLUME, "70");
        //设置播放器音频流类型
        mSynthesizer.setParameter(SpeechConstant.STREAM_TYPE, "3");

        // 设置播放合成音频打断音乐播放，默认为true
        mSynthesizer.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "true");

    }

  


    @Override
    public void onInit(int code) {
        if (code != ErrorCode.SUCCESS) {
            Log.d(TAG, "初始化失败,错误码：" + code);
        } else {

            // 初始化成功，之后可以调用startSpeaking方法
            // 注：有的开发者在onCreate方法中创建完合成对象之后马上就调用startSpeaking进行合成，
            // 正确的做法是将onCreate中的startSpeaking调用移至这里
        }
    }

    public void test(View view) {
        Log.d(TAG, "test: ");
        mSynthesizer.startSpeaking("0001", this);
    }

    @Override
    public void onSpeakBegin() {

    }

    @Override
    public void onBufferProgress(int i, int i1, int i2, String s) {

    }

    @Override
    public void onSpeakPaused() {

    }

    @Override
    public void onSpeakResumed() {

    }

    @Override
    public void onSpeakProgress(int i, int i1, int i2) {

    }

    @Override
    public void onCompleted(SpeechError speechError) {
        if (speechError != null) {
            Log.d(TAG, speechError.toString());
        }
    }

    @Override
    public void onEvent(int i, int i1, int i2, Bundle bundle) {

    }
}
