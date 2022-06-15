package com.example.mltext;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.mlkit.ocr.impl.OcrEngineDelegate;
import com.huawei.hms.mlsdk.MLAnalyzerFactory;
import com.huawei.hms.mlsdk.common.LensEngine;
import com.huawei.hms.mlsdk.common.MLAnalyzer;
import com.huawei.hms.mlsdk.common.MLApplication;
import com.huawei.hms.mlsdk.common.MLException;
import com.huawei.hms.mlsdk.common.MLFrame;
import com.huawei.hms.mlsdk.text.MLLocalTextSetting;
import com.huawei.hms.mlsdk.text.MLRemoteTextSetting;
import com.huawei.hms.mlsdk.text.MLText;
import com.huawei.hms.mlsdk.text.MLTextAnalyzer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
//    public static final String apiKey = "DAEDAE+h2zFfXbwppWF7i6rAczAAVx+HnE5ZKTZJq9bLAaK/a+rJadx79FWIykFqSK+H+FAiVMgRhyppu41OaHvw5dOlkq8NJCFxlw==";
    private MLTextAnalyzer analyzer;
    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_main);
        this.mTextView = this.findViewById(R.id.text_result);
    }

    /**
     * 端侧文本识别
     */
    private void localAnalyzer() {

        MLLocalTextSetting setting = new MLLocalTextSetting.Factory()
                .setOCRMode(MLLocalTextSetting.OCR_TRACKING_MODE)
                //设置语种
                .setLanguage("zh")
                .create();
        String language = setting.getLanguage();
        Log.i(TAG, "localAnalyzer: =========="+language);
        this.analyzer = MLAnalyzerFactory.getInstance()
                .getLocalTextAnalyzer(setting);
        // Create an MLFrame by using android.graphics.Bitmap.
        Bitmap bitmap = BitmapFactory.decodeResource(this.getResources(), R.drawable.img);
        MLFrame frame = MLFrame.fromBitmap(bitmap);

        Task<MLText> task = this.analyzer.asyncAnalyseFrame(frame);
        task.addOnSuccessListener(new OnSuccessListener<MLText>() {
            @Override
            public void onSuccess(MLText text) {
//                Toast.makeText(ImageTextAnalyseActivity.this, "text+++++++:"+text, Toast.LENGTH_SHORT).show();
                MainActivity.this.displaySuccess(text);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                MainActivity.this.displayFailure();
            }
        });

    }
    /**
     * 云测文本识别
     */
    private void remoteAnalyzer() {

        List<String> languageList = new ArrayList();
        languageList.add("zh");
        languageList.add("en");
        MLRemoteTextSetting setting =
                new MLRemoteTextSetting.Factory()
                        .setTextDensityScene(MLRemoteTextSetting.OCR_COMPACT_SCENE)
                        .setLanguageList(languageList)
                        .setBorderType(MLRemoteTextSetting.ARC)
                        .create();
        this.analyzer = MLAnalyzerFactory.getInstance().getRemoteTextAnalyzer(setting);

        Bitmap bitmap = BitmapFactory.decodeResource(this.getResources(), R.drawable.img);
        MLFrame frame = MLFrame.fromBitmap(bitmap);

        MLApplication.getInstance().setApiKey(apiKey);

        Task<MLText> task = analyzer.asyncAnalyseFrame(frame);
        task.addOnSuccessListener(new OnSuccessListener<MLText>() {
            @Override
            public void onSuccess(MLText text) {
                Log.i(TAG, "onSuccess: "+text);
                Toast.makeText(MainActivity.this, "text+++++++:"+text, Toast.LENGTH_SHORT).show();
                MainActivity.this.remoteDisplaySuccess(text);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(MainActivity.this, "onFailure++++++0:"+e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.i(TAG, "onFailure: +++++"+e.getMessage());
                MainActivity.this.displayFailure(e);
            }
        });
    }

    public class OcrDetectorProcessor implements MLAnalyzer.MLTransactor<MLText.Block> {
        @Override
        public void transactResult(MLAnalyzer.Result<MLText.Block> results) {
            SparseArray<MLText.Block> items = results.getAnalyseList();
            // 开发者根据需要处理识别结果，需要注意，这里只对检测结果进行处理。
            // 不可调用ML Kit提供的其他检测相关接口。
            Log.i(TAG, "transactResult: +++++"+items.get(0));
        }
        @Override
        public void destroy() {
            // 检测结束回调方法，用于释放资源等。
        }
    }
    /**
     * 端侧识别失败
     */
    private void displayFailure() {
        this.mTextView.setText("Failure");
    }
    /**
     * 云测识别失败
     */
    private void displayFailure(Exception exception) {
        Log.i(TAG, "displayFailure:++++++ "+exception.getMessage());

        String error = "Failure. ";
        try {
            MLException mlException = (MLException) exception;
            error += "error code: " + mlException.getErrCode() + "\n" + "error message: " + mlException.getMessage();
        } catch (Exception e) {
            error += e.getMessage();
        }
        Log.i(TAG, "displayFailure:+++++++ "+error);
        this.mTextView.setText(error);
    }
    /**
     * 云测识别成功
     * */
    private void remoteDisplaySuccess(MLText mlTexts) {
        String result = "";
        List<MLText.Block> blocks = mlTexts.getBlocks();
        for (MLText.Block block : blocks) {
            List<MLText.TextLine> lines = block.getContents();
            for (MLText.TextLine line : lines) {
                List<MLText.Word> words = line.getContents();
                for (MLText.Word word : words) {
                    result += word.getStringValue() + " ";
                }
            }
            result += "\n";
        }
        Toast.makeText(this, "++++++"+result, Toast.LENGTH_SHORT).show();
        this.mTextView.setText(result);
    }
    /**
     * 端侧识别成功
     * */
    private void displaySuccess(MLText mlText) {

        String result = "";
        List<MLText.Block> blocks = mlText.getBlocks();
        for (MLText.Block block : blocks) {
            for (MLText.TextLine line : block.getContents()) {
                result += line.getStringValue() + "\n";
            }
        }
        this.mTextView.setText(result);
        Log.i(TAG, "displaySuccess: ==================端侧"+result);
        Toast.makeText(MainActivity.this, ""+result, Toast.LENGTH_SHORT).show();

    }
    /**
     * 销毁
     * */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (this.analyzer == null) {
            return;
        }
        try {
            this.analyzer.stop();
        } catch (IOException e) {
            Log.e(MainActivity.TAG, "Stop failed: " + e.getMessage());
        }

        if (analyzer != null) {
            try {
                analyzer.stop();
            } catch (IOException e) {
                // 异常处理。
            }
        }
    }

    public void onButton(View view) {
        localAnalyzer();
    }

}