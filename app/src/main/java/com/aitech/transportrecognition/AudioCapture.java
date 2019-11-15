package com.aitech.transportrecognition;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import java.util.ArrayList;


public class AudioCapture {
    private static final String TAG = "AudioCapture";

    private static AudioRecord mAudioRecord = null;
    private int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    private static int SAMPLE_RATE = 48000;
    private static int AUDIO_RECORD_TIME = 3;
    private final static int CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    private final static int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int AUDIO_BYTE_SIZES = AUDIO_RECORD_TIME * SAMPLE_RATE * 2 * 1;
    private int mBufferSizeInBytes = 0;
    private int payloadSize = 0;
    private Thread mAudioInputThread = null;
    private static final int STATE_RECORDING = 1;
    private static final int STATE_PAUSE = 2;
    private static final int STATE_STOP = 3;
    private int mRecorderState = STATE_STOP;
    private final Object mEncodeLock = new Object();

    private int getBufferSizeInBytes() {
        if (mBufferSizeInBytes == 0) {
            mBufferSizeInBytes = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE, CHANNEL, AUDIO_FORMAT);
        }

        return mBufferSizeInBytes;
    }

    private void initAudioRecorder() {
        if (null == mAudioRecord) {
            mAudioRecord = new AudioRecord(AUDIO_SOURCE, SAMPLE_RATE, CHANNEL,
                    AUDIO_FORMAT, getBufferSizeInBytes());
            payloadSize = 0;
            mBufferSizeInBytes = 0;
        }
    }

    public ArrayList<Float> captureSignal() throws InterruptedException {
        if (mAudioRecord == null) {
            initAudioRecorder();
        }
        if (mAudioRecord != null) {
            mAudioRecord.startRecording();
            mRecorderState = STATE_RECORDING;
        }
        final int buffersize = getBufferSizeInBytes();
        final byte[] buffer = new byte[buffersize];
        final int BufferSize = AUDIO_BYTE_SIZES;
        final byte[] Buffer = new byte[BufferSize];
        final ArrayList<Float> signal = new ArrayList<>(AUDIO_BYTE_SIZES / 2);
        mAudioInputThread = new Thread("audio input thread") {
            int count = 0;

            @Override
            public void run() {
                while (true) {
                    if (mRecorderState == STATE_RECORDING) {
                        int result = mAudioRecord.read(buffer, 0, getBufferSizeInBytes());
                        if (result > 0) {
                            if (payloadSize < AUDIO_BYTE_SIZES) {
                                System.arraycopy(buffer, 0, Buffer,
                                        count * (buffer.length), buffer.length);
                                count += 1;
                                payloadSize += result;
                            } else {
                                byte b1;
                                byte b2;
                                float s;
                                for (int i = 0; i < Buffer.length - 1; i += 2) {
                                    b1 = Buffer[i];
                                    b2 = Buffer[i + 1];
                                    s = ((short) ((b2 & 0x00FF) << 8 | b1 & 0x00FF)) / 32768f;
                                    signal.add(s);
                                }
                                AudioCapture.this.stop();
                            }
                        }
                    } else if (mRecorderState == STATE_STOP) {
                        break;
                    } else if (mRecorderState == STATE_PAUSE) {
                        synchronized (mEncodeLock) {
                            try {
                                if (mRecorderState == STATE_PAUSE) {
                                    mEncodeLock.wait();
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        };
        mAudioInputThread.start();
        mAudioInputThread.join();
        return signal;
    }


    private void stopAudioInput() {
        if (mAudioRecord != null) {
            mAudioRecord.stop();
            mAudioRecord.release();
        }
    }

    public boolean stop() {
        if (mRecorderState == STATE_STOP) {
            return true;
        }
        mRecorderState = STATE_STOP;
        synchronized (mEncodeLock) {
            mEncodeLock.notifyAll();
        }
        stopAudioInput();
        mAudioRecord = null;
        return true;
    }
}
