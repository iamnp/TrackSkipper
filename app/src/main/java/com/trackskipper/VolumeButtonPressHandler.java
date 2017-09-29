package com.trackskipper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.KeyEvent;

public class VolumeButtonPressHandler extends BroadcastReceiver {

    private static final String EXTRA_VOLUME_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE";
    private static final String EXTRA_PREV_VOLUME_STREAM_VALUE = "android.media.EXTRA_PREV_VOLUME_STREAM_VALUE";
    private static final String EXTRA_VOLUME_STREAM_VALUE = "android.media.EXTRA_VOLUME_STREAM_VALUE";
    public static int THRESHOLD_MS = 400;
    private static long lastPressTime = -1;
    private static int lastUpOrDown = -1;
    private static int prevPrevVolume = -1;

    private static int sign(int a) {
        return a > 0 ? 1 : (a < 0 ? -1 : 0);
    }

    private static void pressButton(long eventTime, int keyEvent, AudioManager am) {
        KeyEvent downEvent = new KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyEvent, 0);
        am.dispatchMediaKeyEvent(downEvent);
        KeyEvent upEvent = new KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyEvent, 0);
        am.dispatchMediaKeyEvent(upEvent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            reactOnDoublePress(context, intent);
        } catch (NullPointerException e) {
            // omitted
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void reactOnDoublePress(Context context, Intent intent) {
        int volumeType = (Integer) intent.getExtras().get(EXTRA_VOLUME_STREAM_TYPE);
        if (volumeType != AudioManager.STREAM_MUSIC) return;

        if (((PowerManager) context.getSystemService(Context.POWER_SERVICE)).isInteractive())
            return;

        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (!am.isMusicActive()) return;

        long pressTime = SystemClock.uptimeMillis();

        int prevVolume = (Integer) intent.getExtras().get(EXTRA_PREV_VOLUME_STREAM_VALUE);
        int currVolume = (Integer) intent.getExtras().get(EXTRA_VOLUME_STREAM_VALUE);
        int upOrDown = sign(currVolume - prevVolume);

        if (upOrDown == lastUpOrDown) {
            if (pressTime - lastPressTime < THRESHOLD_MS) {

                if (upOrDown > 0) {
                    pressButton(pressTime, KeyEvent.KEYCODE_MEDIA_NEXT, am);
                } else {
                    pressButton(pressTime, KeyEvent.KEYCODE_MEDIA_PREVIOUS, am);
                }

                if (prevPrevVolume >= 0 && prevPrevVolume <= am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)) {
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, prevPrevVolume, 0);
                } else {
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, prevVolume, 0);
                }
            }
        }

        prevPrevVolume = prevVolume;
        lastPressTime = pressTime;
        lastUpOrDown = upOrDown;
    }
}
