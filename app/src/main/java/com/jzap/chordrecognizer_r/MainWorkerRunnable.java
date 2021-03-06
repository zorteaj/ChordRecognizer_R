package com.jzap.chordrecognizer_r;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

/**
 * Created by Justin on 11/15/2014.
 */
public class MainWorkerRunnable implements Runnable {

    private static final String TAG = "MainWorkerRunnable";
    private static final int DISPLAY_CHORD = 1;
    private static final int DISPLAY_CHORDAL_NOTES_TEXT = 2;
    public static final int DISPLAY_VOLUME_STATUS = 3;
    public static final int DISPLAY_BUTTON_OFF = 4;

    private MainActivity mMainActivity;
    private Handler mHandler;

    private Drawable mDrDormantButton;
    private Bitmap mBmpButton;

    private Bitmap mBmpLabeledButton;
    private Canvas mCanvas;
    private Paint mTextPaint;
    private Paint mButtonPaint;
    
    private boolean mEndRunnable = false;

    // Constructor
    public MainWorkerRunnable(MainActivity mainActivity) {

        mMainActivity = mainActivity;
        mDrDormantButton = mMainActivity.getResources().getDrawable(R.drawable.button);
        mBmpButton = BitmapFactory.decodeResource(mMainActivity.getResources(), R.drawable.button);

        //Log.i(TAG, "Button Height = " + mDrDormantButton.getHeight());

        mBmpLabeledButton = Bitmap.createBitmap(mBmpButton.getWidth(), mBmpButton.getHeight(), Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBmpLabeledButton);
        mButtonPaint = new Paint();
        mTextPaint = new Paint();

        mTextPaint.setAntiAlias(true);
        mTextPaint.setStrokeWidth(50);
        mTextPaint.setTextSize(20);
        mTextPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
                if(message.what == DISPLAY_CHORD) {
                    displayChord(message);
                } else if(message.what == DISPLAY_CHORDAL_NOTES_TEXT) {
                    displayChordalNotesText(message);
                } else if(message.what == DISPLAY_VOLUME_STATUS){
                    displayVolumeState(message);
                } else if(message.what == DISPLAY_BUTTON_OFF) {
                    displayButtonOff(message);
                }//end if/else
            }//end handleMessage()
        };//end mHandler initialization
    }
    // End Constructor

// Runnable Interface Implementations
    @Override
    public void run() {
        AudioAnalysis audioAnalysis;
        RecordAudio recordAudio = new RecordAudio(mMainActivity, mHandler);
        // Consider killing thread when record button is shut off, and restarting when turned back on (May save battery, may be good practice...)
        while(!mEndRunnable) {
            if(mMainActivity.getmRecording() && recordAudio.volumeThresholdMet() ) {

                // There's a tradeoff in getting the audioAnalysis here, instead of after the next line - app seems less responsive
                audioAnalysis = recordAudio.doChordDetection();
                mHandler.obtainMessage(DISPLAY_CHORD, audioAnalysis).sendToTarget();
               // This displays the results in plain text
               // mHandler.obtainMessage(DISPLAY_CHORDAL_NOTES_TEXT, audioAnalysis).sendToTarget();
                try {
                    Thread.sleep(2000);
                } catch(InterruptedException e) {
                    e.printStackTrace();
                }//end try/catch
              // ProcessAudio.setmNewPCP(false);
            }//end if
        }//end while
        recordAudio.destroyRecordAudio();
    }//end run()
// End Runnable Interface Implementations

// Accessors/Modifiers
    public MainActivity getmMainActivity() {
        return mMainActivity;
    }

    public void setmEndRunnable(boolean endRunnable) {
        mEndRunnable = endRunnable;
    }
//End Accessors/Modifiers

    private int lookupChordColor(String chord) {
        if((chord.contains("C") && !chord.contains("C#")) || (chord.contains("F") && !chord.contains("F#"))|| chord.contains("A#")) {
            return NotesGraphView.mOPAQUE_DARK_COLORS[0];
        } else if(chord.contains("C#") || chord.contains("F#") || chord.contains("B")) {
            return NotesGraphView.mOPAQUE_DARK_COLORS[1];
        } else if((chord.contains("D") && !chord.contains("D#")) || (chord.contains("G") && !chord.contains("G#"))) {
            return NotesGraphView.mOPAQUE_DARK_COLORS[2];
        } else if(chord.contains("D#") || chord.contains("G#")) {
            return NotesGraphView.mOPAQUE_DARK_COLORS[3];
        }else if(chord.contains("E") || (chord.contains("A")))   {
            return NotesGraphView.mOPAQUE_DARK_COLORS[4];
        }
        Log.i(TAG, "No Color");
        return -1;
    }

    // TODO : Most of this is lifted from NotesGraphView, which is to be made into common method - use that method
    private void displayChord(Message message) {
        int chordColor = lookupChordColor(((AudioAnalysis) message.obj).getmChord());
       // mButtonPaint.setColorFilter(new LightingColorFilter(010101, chordColor));
      //  mCanvas.drawBitmap(mBmpButton, 0, 0, mButtonPaint);

        int[] origin11 = new int[2];
        int[] origin2 = new int[2];

        mMainActivity.findViewById(R.id.rl_main).getLocationInWindow(origin11);
        mMainActivity.getmIv_button().getLocationInWindow(origin2);
        int halfViewLength = mMainActivity.getmIv_button().getHeight()/2;
        int halfViewWidth = mMainActivity.getmIv_button().getWidth()/2;

        Paint testPaint;
        testPaint = new Paint();
        testPaint.setStrokeWidth(10);
        // TODO : Make dynamic
        testPaint.setTextSize(50);
        testPaint.setColor(Color.WHITE);

        // TODO : Make dynamic
        mCanvas.drawText(((AudioAnalysis) message.obj).getmChord(), halfViewWidth - 40 , halfViewLength, testPaint);

      //  mMainActivity.getmIv_button().setImageBitmap(mBmpLabeledButton);
    }

    private void displayChordalNotesText(Message message) {
        mMainActivity.getmTv_chord().setText(((AudioAnalysis) message.obj).getmChord());
        mMainActivity.getmTv_mostIntenseNote().setText(((AudioAnalysis) message.obj).getmMostIntenseNote());
        mMainActivity.getmTv_secMostIntenseNote().setText(((AudioAnalysis) message.obj).getmSeconMostIntenseNote());
        mMainActivity.getmTv_thirdMostIntenseNote().setText(((AudioAnalysis) message.obj).getmThirdMostIntenseNote());
    }

    private void displayVolumeState(Message message) {
        int i = (Integer) message.obj;
        //Log.d(TAG, String.valueOf(i));
    }

    private void displayButtonOff(Message message) {
        Log.i(TAG, "************ Set Image View to Button ***********");
        mMainActivity.getmIv_button().setImageDrawable(mDrDormantButton);
    }
}
