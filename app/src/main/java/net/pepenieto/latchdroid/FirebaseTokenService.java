package net.pepenieto.latchdroid;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.elevenpaths.latch.LatchAuth;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.google.gson.JsonElement;

import java.util.HashMap;
import java.util.Map;

public class FirebaseTokenService extends FirebaseInstanceIdService {
    private static String TAG = FirebaseTokenService.class.getSimpleName();
    public static String FIREBASE_TOKEN = "FIREBASE_TOKEN";

    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Refreshed token: " + refreshedToken);


        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(FIREBASE_TOKEN, refreshedToken);
        editor.commit();

        try {
            sendRegistrationToServer(refreshedToken);
        } catch (Exception e) {
            Log.e(TAG,e.getMessage(),e);
        }
    }

    private void sendRegistrationToServer(String token) throws Exception {
        Log.d(TAG,"sendRegistrationToServer...");
        String url = "http://latch.pepenieto.net/updateFirebaseToken.php";
        Map<String, String> data = new HashMap<String, String>();
        data.put("token",token);

        LatchAuth la = new LatchAuth();
        JsonElement result = la.HTTP_POST(url,new HashMap<String, String>(),data);
        Log.d(TAG,"result: " + result.toString());
    }
}