package net.pepenieto.latchdroid;

import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.elevenpaths.latch.LatchApp;
import com.elevenpaths.latch.LatchResponse;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EService;
import org.androidannotations.annotations.Receiver;
import org.androidannotations.annotations.sharedpreferences.Pref;


@EService
public class LatchdroidService extends Service {
    public static String TAG = "LatchdroidService";

    @Pref LatchdroidPreferences_ preferences;


    @Receiver(actions = Intent.ACTION_USER_PRESENT)
    protected void onUserPresent(Context context) {
        Log.d(TAG,"onUserPresent");
        onDeviceLock(context);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Background
    public void onDeviceLock(Context context) {
        try {
            LatchApp latch = new LatchApp(LatchConfig.APP_ID, LatchConfig.SECRET_KEY);
            LatchResponse response = latch.status(preferences.latchAccountId().get(), LatchConfig.OPERATION_ID);
            Log.d(TAG, response.toJSON().toString());

            if (response.getError() != null) {
                Log.e(TAG, response.getError().getMessage());
                // No connection ? check preferences
                return;
            }

            String status = response.getData().getAsJsonObject("operations").getAsJsonObject(LatchConfig.OPERATION_ID).get("status").getAsString();
            preferences.lastStatus().put(status);
            if (status.equals("off")) {
                lock(context);
            }
        } catch (Exception e) {
            // All exceptions, usually network exceptions (no connection!!!)
            if (preferences.whenNoConnection().get() == 0) { // lock
                lock(context);
            }
            if (preferences.whenNoConnection().get() == 2) { // remember last status
                if (preferences.lastStatus().get().equals("off")) {
                    lock(context);
                }
            }
        }

    }

    private void lock(Context context) {
        ComponentName componentName = new ComponentName(context, UnlockReceiver.class);
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        boolean active = dpm.isAdminActive(componentName);
        Log.i(context.getClass().getSimpleName(), "Active (in initiateDeviceLock) = " + String.valueOf(active));
        if (active) {
            dpm.lockNow();
        }
    }
}
