package net.pepenieto.latchdroid;

import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.elevenpaths.latch.LatchApp;
import com.elevenpaths.latch.LatchResponse;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EService;
import org.androidannotations.annotations.Receiver;


@EService
public class LatchdroidService extends Service {
    public static String TAG = "LatchdroidService";

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
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        LatchApp latch = new LatchApp(LatchConfig.APP_ID,LatchConfig.SECRET_KEY);
        LatchResponse response = latch.operationStatus(sharedPref.getString(MainActivity.LATCH_ACCOUNT,""),LatchConfig.OPERATION_ID);
        Log.d(TAG,response.toJSON().toString());

        if (response.getError() != null) {
            Log.e(TAG,response.getError().getMessage());
            return;
        }

        if (response.toJSON().getAsJsonObject("data").getAsJsonObject("operations").getAsJsonObject(LatchConfig.OPERATION_ID).get("status").getAsString().equals("off")) {
            ComponentName componentName = new ComponentName(context, UnlockReceiver.class);
            DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            boolean active = dpm.isAdminActive(componentName);
            Log.i(context.getClass().getSimpleName(), "Active (in initiateDeviceLock) = " + String.valueOf(active));
            if (active) {
                dpm.lockNow();
            }
        }

    }
}
