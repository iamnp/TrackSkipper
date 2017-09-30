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
    private static int prevVolumeDir = -1;
    private static int prevPrevVolume = -1;

    private static int getVolumeDir(int prevVolume, int curVolume, int maxVolume) {
        // simply compare values if volume did change
        if (curVolume > prevVolume) return 1;
        if (curVolume < prevVolume) return -1;

        // if volume didn't change - determine dir for extreme cases
        if (curVolume == 0) return -1;
        if (curVolume == maxVolume) return 1;

        // dir can't be determined
        return 0;
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
        // we react on volume changes for MUSIC stream
        if (((Integer) intent.getExtras().get(EXTRA_VOLUME_STREAM_TYPE)) != AudioManager.STREAM_MUSIC)
            return;

        // we react on volume changes when screen is off
        if (((PowerManager) context.getSystemService(Context.POWER_SERVICE)).isInteractive())
            return;

        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        // we react on volume changes when music is playing
        if (!am.isMusicActive()) return;

        long pressTime = SystemClock.uptimeMillis();
        int prevVolume = (Integer) intent.getExtras().get(EXTRA_PREV_VOLUME_STREAM_VALUE);
        int currVolume = (Integer) intent.getExtras().get(EXTRA_VOLUME_STREAM_VALUE);
        int maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int volumeDir = getVolumeDir(prevVolume, currVolume, maxVolume);

        if (volumeDir == prevVolumeDir) {
            if (pressTime - lastPressTime < THRESHOLD_MS) {

                if (volumeDir > 0) {
                    pressButton(pressTime, KeyEvent.KEYCODE_MEDIA_NEXT, am);
                } else {
                    pressButton(pressTime, KeyEvent.KEYCODE_MEDIA_PREVIOUS, am);
                }

                if (prevPrevVolume >= 0 && prevPrevVolume <= maxVolume) {
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, prevPrevVolume, 0);
                } else {
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, prevVolume, 0);
                }
            }
        }

        prevPrevVolume = prevVolume;
        lastPressTime = pressTime;
        prevVolumeDir = volumeDir;
    }
}
