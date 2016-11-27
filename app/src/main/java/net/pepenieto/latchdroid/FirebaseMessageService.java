package net.pepenieto.latchdroid;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class FirebaseMessageService extends FirebaseMessagingService {
    private static String TAG = FirebaseMessageService.class.getSimpleName();

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // TODO(developer): Handle FCM messages here.
        // If the application is in the foreground handle both data and notification messages here.
        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
        Log.d(TAG, "From: " + remoteMessage.getFrom());
//        String body = remoteMessage.getNotification().getBody();
        String body = remoteMessage.getData().get("message");
        Log.d(TAG, "Notification Message Body: " + body);
        if (body.equals(LatchConfig.OPERATION_ID) || body.equals(LatchConfig.APP_ID)) {
            // do lock
            ComponentName componentName = new ComponentName(this, UnlockReceiver.class);
            DevicePolicyManager dpm = (DevicePolicyManager) this.getSystemService(Context.DEVICE_POLICY_SERVICE);
            boolean active = dpm.isAdminActive(componentName);
            Log.i(this.getClass().getSimpleName(), "Active (in initiateDeviceLock) = " + String.valueOf(active));
            if (active) {
                dpm.lockNow();
            }

        }
    }
}
