package com.jzap.chordrecognizer_r;

import android.media.AudioRecord;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Justin on 11/15/2014.
 */
public class RecordAudio {

    private static final String TAG = "RecordAudio";
    public static final int VOLUME_THRESHOLD = 28000;
    private static final int GRAPH_PCP_INTERVAL = 80; // was 320

    private static int count = 1;

    private AudioRecord recorder;

    private MainActivity mMainActivity;
    private Handler mHandler;

    //Constructor
    public RecordAudio(MainActivity mainActivity, Handler handler) {
        recorder = new AudioRecord(AudioConfig.audioSrc, AudioConfig.frequency,
                AudioConfig.channelConfiguration, AudioConfig.audioEncoding, AudioConfig.bufferSize);
        mMainActivity =  mainActivity;
        mHandler = handler;
    }
    // End Constructor

    public boolean volumeThresholdMet() {
        ArrayList<Short> cumulativeTestAudioInput = new ArrayList<Short>();
        short[] testAudioInput = new short[100];
        recorder.startRecording();
        while(mMainActivity.getmRecording()) {
            int greatestSample = 0;
            recorder.read(testAudioInput, 0, testAudioInput.length);
            accumulateAudio(testAudioInput, cumulativeTestAudioInput);
            for(int i = 0; i < testAudioInput.length; i++) {
                if(Math.abs(testAudioInput[i]) > greatestSample) {
                    greatestSample = Math.abs(testAudioInput[i]);
                }
                if(Math.abs(testAudioInput[i]) >= VOLUME_THRESHOLD) {
                    //Log.i(TAG, String.valueOf(greatestSample));
                    recorder.stop();
                    mMainActivity.getmNgv_graph().setmVolumeThresholdMet(true);
                    // TODO : Don't just return true - Do Chord detection using the samples collected thus far, instead of throwing them out and wasting time re-recording.
                    // TODO : Edit - this didn't seem feasible
                    return true;
                }
            }//end for
            //Log.i(TAG, String.valueOf(greatestSample));
            showVolume(greatestSample);

            if (count >= GRAPH_PCP_INTERVAL) {
                new ProcessAudio(mMainActivity).detectChord(toPrimitive(cumulativeTestAudioInput), false, greatestSample);
                cumulativeTestAudioInput = new ArrayList<Short>();
                count = 0;
            }
            count++;

        }//end while
        //showVolume(-1);
        recorder.stop();
        return false;
    }

    public AudioAnalysis doChordDetection() {
        // short audioInput[] = new short[32000];
        short audioInput[] = new short[16000];
        recorder.startRecording();
        recorder.read(audioInput, 0, audioInput.length);
        recorder.stop();
        int greatestSample = 0;
        for(int i = 0; i < audioInput.length; i++) {
            if (audioInput[i] > greatestSample) {
                greatestSample = audioInput[i];
            }
        }
        // greatestSample is ignored when volume threshold is met, because since this sample is taken after
        // the volume threshold was initially met, this value may different than volumeThreshold
        return new ProcessAudio(mMainActivity).detectChord(audioInput, true, greatestSample);
    }

    private void accumulateAudio(short[] audio, List<Short> cumulativeAudio) {
        for(int i = 0; i < audio.length; i++) {
            cumulativeAudio.add(audio[i]);
        }
    }

    private short[] toPrimitive(ArrayList<Short> theList) {
        short[] result = new short[theList.size()];
        for(int i = 0; i < result.length; i++) {
            result[i] = theList.get(i);
        }
        return result;
    }

    public void destroyRecordAudio() {
        recorder.release();
    }

     private void showVolume(int greatestSample) {
        Integer i;
        int quarterVolThrshld = VOLUME_THRESHOLD/4;

        if(greatestSample < 0) {
            Log.i(TAG, "greatestSample = -1");
            mHandler.obtainMessage(MainWorkerRunnable.DISPLAY_BUTTON_OFF).sendToTarget();
            return;
        } else if((greatestSample > 0) && (greatestSample < 500)) {
            i = new Integer(0);
        } else if(greatestSample < (VOLUME_THRESHOLD - quarterVolThrshld * 3)) {
            i = new Integer(1);
        } else if(greatestSample < VOLUME_THRESHOLD - quarterVolThrshld * 2) {
            i = new Integer(2);
        } else {
            i = new Integer(3);
        }
       // mHandler.obtainMessage(MainWorkerRunnable.DISPLAY_VOLUME_STATUS, i).sendToTarget();
    }


}
