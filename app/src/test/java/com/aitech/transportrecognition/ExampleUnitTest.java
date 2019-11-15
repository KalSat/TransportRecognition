package com.aitech.transportrecognition;

import org.junit.Test;

import java.util.Arrays;


/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {

    private static final String TAG = "ExampleUnitTest";
    private static final int SAMPLES_PER_FRAME = 2048;                    // 每帧的样本数
    private static final int SAMPLE_RATE = 44100;                         // 每秒的采样频率
    private static final int N_MFCCS = 40;                                // MFCC(梅尔倒普系数)的数量
    private static final int N_MELS = 128;                                // 滤波器组的数量
    private static final float MIN_FILTER_FREQUENCY = 0.0f;               // mel滤波的最低频率
    private static final float MAX_FILTER_FREQUENCY = SAMPLE_RATE / 2;    // mel滤波的最高频率
    private String mFilePath = "W:\\DataSet\\AudioSet\\bus\\bus_electricCar_sitting_engineOn_2019-07-30-21-05-12_ShenZhen_141432552.wav";

    @Test
    public void addition_isCorrect() {

        String[] S = new String[]{"1", "2", "3"};
        String[] D = new String[]{"4", "5", "6", "7", "8", "9"};
        System.arraycopy(S, 0, D, 0 * (S.length), S.length);
        System.out.println(Arrays.toString(D));


    }
}


/**
 * public class MyTest {
 * public static void main(String[] args) {
 * System.out.print("my text java class");
 * }
 **/