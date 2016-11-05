package net.pepenieto.latchdroid;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.elevenpaths.latch.Latch;
import com.elevenpaths.latch.LatchResponse;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OnActivityResult;
import org.androidannotations.annotations.Trace;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

@EActivity(R.layout.activity_main)
public class MainActivity extends Activity {
    private static String TAG = "MainActivity";
    static String LATCH_ACCOUNT = "LATCH_ACCOUNT";
    private static final int ADMIN_RESULT = 41;

    @ViewById LinearLayout llPairToken;
    @ViewById EditText etPairToken;
    @ViewById Button btnStart;
    @ViewById Button btnMakeAdmin;
    @ViewById TextView tvError;
    @ViewById TextView tvIsAdmin;


    @AfterViews
    protected void updateScreen() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        ComponentName adminComponent = new ComponentName(this, UnlockReceiver.class);
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

        boolean isAdmin = devicePolicyManager.isAdminActive(adminComponent);
        boolean isLatchAccountID = sharedPref.contains(LATCH_ACCOUNT);

        if (isLatchAccountID) {
            Log.v(TAG,"Latch account: " + sharedPref.getString(LATCH_ACCOUNT,""));
            testLatch();
        }

        llPairToken.setVisibility(isLatchAccountID ? View.GONE : View.VISIBLE);
        tvIsAdmin.setText(getString(isAdmin?R.string.admin_msg:R.string.no_admin_msg));
        btnMakeAdmin.setEnabled(!isAdmin);
        btnStart.setEnabled(isLatchAccountID && isAdmin);
    }

    @UiThread
    public void setError(String message, boolean isPairError) {
        tvError.setText(message);
        if (isPairError) llPairToken.setEnabled(true);
        btnStart.setEnabled(false);
    }

    @Background
    public void testLatch() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        Latch latch = new Latch(LatchConfig.APP_ID,LatchConfig.SECRET_KEY);
        LatchResponse response = latch.operationStatus(sharedPref.getString(LATCH_ACCOUNT,""),LatchConfig.OPERATION_ID);

        if (response.getError() != null) {
            setError(response.getError().getMessage(),true);
        }
    }

    @Click
    public void btnMakeAdmin() {
        ComponentName adminComponent = new ComponentName(this, UnlockReceiver.class);
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
        startActivityForResult(intent,ADMIN_RESULT);
    }

    @Click
    @Background
    public void btnPairToken() {
        setError("",false);
        Latch latch = new Latch(LatchConfig.APP_ID,LatchConfig.SECRET_KEY);
        String pairCode = etPairToken.getText().toString();
        Log.v(TAG, "Pair code: " + pairCode);
        LatchResponse pairResponse = latch.pair(pairCode);
        Log.d(TAG, pairResponse.toJSON().toString());
        updateResult(pairResponse);
    }


    @UiThread
    public void updateResult(LatchResponse result) {
        if (result.getError() != null) {
            setError(result.toJSON().getAsJsonObject("error").get("message").getAsString(),true);
        } else {
            String accountID = result.toJSON().getAsJsonObject("data").get("accountId").getAsString();
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(LATCH_ACCOUNT, accountID);
            Log.d(TAG,"Put accountID = " + accountID);
            editor.commit();
            updateScreen();
        }
    }


    @Click
    @Trace
    public void btnStart() {
        LatchdroidService_.intent(getApplication())
                .start();

        /*
        Intent service = new Intent(getApplicationContext(), LatchdroidService.class);
        startService(service);
        */
        finish();
    }

    @OnActivityResult(ADMIN_RESULT)
    protected void onActivityResult() {
        updateScreen();
    }

}
