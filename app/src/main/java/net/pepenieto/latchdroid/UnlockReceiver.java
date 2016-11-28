package net.pepenieto.latchdroid;

import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.elevenpaths.latch.LatchApp;
import com.elevenpaths.latch.LatchResponse;

/**
 * Created by pepe on 25/10/16.
 */


public class UnlockReceiver extends BroadcastReceiver {

    private static String TAG = UnlockReceiver.class.getSimpleName();

    protected void initiateDeviceLock(final Context context) {
            // Latch here

            new AsyncTask<String, Void, Void>() {

                @Override
                protected Void doInBackground(String... params) {
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
                    LatchApp latch = new LatchApp(LatchConfig.APP_ID,LatchConfig.SECRET_KEY);
                    LatchResponse response = latch.status(sharedPref.getString(context.getString(R.string.LATCH_ACCOUNT),""),LatchConfig.OPERATION_ID);
                    if (response.toJSON().getAsJsonObject("data").getAsJsonObject("operations").getAsJsonObject("ZVRcCMGh28md7XjbxQTi").get("status").getAsString().equals("off")) {
                        doLock(context);
                    }


                    return null;
                }
            }.execute("");

    }

    private void doLock(Context context) {
        ComponentName componentName = new ComponentName(context, UnlockReceiver.class);
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        boolean active = dpm.isAdminActive(componentName);
        Log.i(context.getClass().getSimpleName(), "Active (in initiateDeviceLock) = " + String.valueOf(active));
        if (active) {
            dpm.lockNow();
        }
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_USER_PRESENT)) {
            Log.v(TAG, "In Method:  ACTION_USER_PRESENT");
            initiateDeviceLock(context);
            //Handle resuming events
        }
    }
}





