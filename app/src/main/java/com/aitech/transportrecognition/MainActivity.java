package com.aitech.transportrecognition;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";

    private static final int SAMPLING_RATE = 44100;
    private static final int SAMPLES_PER_FRAME = 2048;
    private static final int HOP_LENGTH = SAMPLES_PER_FRAME / 4;
    private static final int N_MFCCS = 60;
    private static final int N_MELS = 128;
    private static final float MIN_FILTER_FREQUENCY = 0.0f;
    private static final float MAX_FILTER_FREQUENCY = SAMPLING_RATE / 2;

    private Button mStartButton;
    private AudioCapture mAudioCapture;
    private TextView mTvClass;
    private TextView mTvProbability;

    @BindView(R.id.img_activity)
    protected ImageView mImgActivity;

    private String MODEL_NAME = "AudioModel.tflite";
    private static final int N_CLASS = 6;
    private static final String[] CLASS = {"airplane", "bus", "car", "coach", "metro", "train"};
    private Interpreter tflite = null;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                Bundle bundle = msg.getData();

                int class_index = (int) bundle.get("class_index");
                float confidence = (float) bundle.get("confidence");
                String predict_label = CLASS[class_index];
                Log.d(TAG, predict_label);
                Log.d(TAG, String.valueOf(confidence));
                mStartButton.setText("Restart");
                mStartButton.setEnabled(true);
                switch (predict_label) {
                    case "airplane":
                        mImgActivity.setImageDrawable(getDrawable(R.drawable.ic_flight_takeoff));
                        mTvClass.setText("airplane");
                        mTvProbability.setText("confidence: " + confidence);
                        break;
                    case "bus":
                        mImgActivity.setImageDrawable(getDrawable(R.drawable.ic_directions_bus));
                        mTvClass.setText("bus");
                        mTvProbability.setText("confidence: " + confidence);
                        break;
                    case "car":
                        mImgActivity.setImageDrawable(getDrawable(R.drawable.car_1080));
                        mTvClass.setText("car");
                        mTvProbability.setText("confidence: " + confidence);
                        break;
                    case "coach":
                        mImgActivity.setImageDrawable(getDrawable(R.drawable.ic_airport_shuttle));
                        mTvClass.setText("coach");
                        mTvProbability.setText("confidence: " + confidence);
                        break;
                    case "metro":
                        mImgActivity.setImageDrawable(getDrawable(R.drawable.ic_subway));
                        mTvClass.setText("subway");
                        mTvProbability.setText("confidence: " + confidence);
                        break;
                    case "train":
                        mImgActivity.setImageDrawable(getDrawable(R.drawable.ic_train));
                        mTvClass.setText("train");
                        mTvProbability.setText("confidence: " + confidence);
                        break;
                }
            }
        }
    };

    private String[] permissions = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
    };

    private void showDialogTipUserRequestPermission() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("存储权限不可用")
                .setMessage("由于本应用需要获取存储空间，为你存储个人信息；\n否则，您将无法正常使用本应用")
                .setPositiveButton("立即开启", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startRequestPermission();
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setCancelable(false)
                .show();
    }

    private void startRequestPermission() {
        ActivityCompat.requestPermissions(this, permissions, 321);
    }

    private class ModelPredictThread extends Thread {
        @Override
        public void run() {
            try {
                ArrayList<Float> SIGNAL = mAudioCapture.captureSignal();
                int size = SIGNAL.size();
                int start = 0;
                for (int i = 0; i < size; i++) {
                    Float vol = SIGNAL.get(i);
                    start = i;
                    if (vol > 0 || vol < 0) {
                        break;
                    }
                }
                List<Float> subList = SIGNAL.subList(start, size);
                Python py = Python.getInstance();
                long time0 = System.currentTimeMillis();
                PyObject signal = py.getModule("feature").callAttr("convert_list", subList);
                long time1 = System.currentTimeMillis();

                PyObject mfcc = py.getModule("feature").callAttr("mean_mfcc", signal,
                        SAMPLING_RATE, SAMPLES_PER_FRAME, HOP_LENGTH, N_MFCCS,
                        N_MELS, MIN_FILTER_FREQUENCY, MAX_FILTER_FREQUENCY);
                long time2 = System.currentTimeMillis();
                Log.d(TAG, "time:" + (time2 - time1));

                List<PyObject> Features = mfcc.asList();
                float[] features = new float[Features.size()];
                for (int i = 0; i < Features.size(); i++) {
                    features[i] = Features.get(i).toFloat();
                }

                Object[] R = predict(features);
                Bundle bundle = new Bundle();
                bundle.putInt("class_index", (int) R[0]);
                bundle.putFloat("confidence", (float) R[1]);
                Message msg = new Message();
                msg.what = 0;
                msg.setData(bundle);
                mHandler.sendMessage(msg);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mStartButton = findViewById(R.id.start_button);
        mImgActivity = findViewById(R.id.img_activity);
        mTvClass = findViewById(R.id.tv_class);
        mTvProbability = findViewById(R.id.tv_probability);
        mStartButton.setOnClickListener(this);
        mAudioCapture = new AudioCapture();

        initPython();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int i = ContextCompat.checkSelfPermission(this, permissions[0]);
            if (i != PackageManager.PERMISSION_GRANTED) {
                showDialogTipUserRequestPermission();
            }
        }
    }

    void initPython() {
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_button:
                mImgActivity.setImageDrawable(getDrawable(R.drawable.unknown_1080));
                mTvClass.setText("");
                mTvProbability.setText("");
                mStartButton.setText("recognizing...");
                mStartButton.setEnabled(false);
                new ModelPredictThread().start();
                break;
        }
    }

    private MappedByteBuffer loadModelFile(String model_name) throws IOException {
        AssetFileDescriptor fileDescriptor = getApplicationContext().getAssets().openFd(model_name);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private void load_model(String model_name) {
        try {
            tflite = new Interpreter(loadModelFile(model_name));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Object[] predict(float[] features) {
        load_model(MODEL_NAME);

        float[][] ProbabilityArray = new float[1][N_CLASS];
        tflite.run(features, ProbabilityArray);
        tflite.close();

        return argmax(ProbabilityArray[0]);
    }

    public Object[] argmax(float[] array) {

        int best = -1;
        float best_confidence = Float.MIN_VALUE;
        for (int i = 0; i < array.length; i++) {
            float value = array[i];
            if (value > best_confidence) {
                best_confidence = value;
                best = i;
            }
        }
        return new Object[]{best, best_confidence};
    }

}
