package net.pepenieto.latchdroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.elevenpaths.latch.LatchApp;
import com.elevenpaths.latch.LatchAuth;
import com.elevenpaths.latch.LatchResponse;
import com.google.gson.JsonElement;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OnActivityResult;
import org.androidannotations.annotations.Trace;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;

import java.util.HashMap;
import java.util.Map;

@EActivity(R.layout.activity_main)
public class MainActivity extends Activity {
    @Pref
    LatchdroidPreferences_ preferences;

    private static String TAG = MainActivity.class.getSimpleName();

    private static final int ADMIN_RESULT = 41;

    @ViewById LinearLayout llPairToken;
    @ViewById LinearLayout llUnpairToken;
    @ViewById EditText etPairToken;
    @ViewById Button btnStart;
    @ViewById Button btnMakeAdmin;
    @ViewById TextView tvError;
    @ViewById TextView tvIsAdmin;
    @ViewById CheckBox cbRemoteLock;
    @ViewById RadioButton rbOn;
    @ViewById RadioButton rbOff;
    @ViewById RadioButton rbLast;

    private boolean isLatched = false;
    private boolean isAdmin = false;

    private String latchAccountID;

    private boolean isLatched() {
        isLatched = preferences.latchAccountId().exists();
        if (isLatched) {
            latchAccountID = preferences.latchAccountId().get();
        }
        Log.d(TAG,"isLatched: " + isLatched);
        return isLatched;
    }

    private boolean isAdmin() {
        ComponentName adminComponent = new ComponentName(this, UnlockReceiver.class);
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        isAdmin = devicePolicyManager.isAdminActive(adminComponent);
        Log.d(TAG,"isAdmin: " + isAdmin);
        return isAdmin;
    }

    @AfterViews
    protected void updateScreen() {
        isLatched();
        isAdmin();

        if (isLatched) {
            testLatched();
        }

        updateUI();
    }

    @UiThread
    public void updateUI() {
        Log.d(TAG,"updateUI");
        llPairToken.setVisibility(isLatched ? View.GONE : View.VISIBLE);
        llUnpairToken.setVisibility(isLatched? View.VISIBLE : View.GONE);

        cbRemoteLock.setChecked(preferences.remoteLock().getOr(false));
        tvIsAdmin.setText(getString(isAdmin?R.string.admin_msg:R.string.no_admin_msg));
        btnMakeAdmin.setEnabled(!isAdmin);
        btnStart.setEnabled(isAdmin && isAdmin);

        rbOn.setChecked(preferences.whenNoConnection().get()==0);
        rbOff.setChecked(preferences.whenNoConnection().get()==1);
        rbLast.setChecked(preferences.whenNoConnection().get()==2);


        hideKeyboard();
    }

    @UiThread
    public void setError(String message, boolean isPairError) {
        tvError.setText(message);
        if (isPairError) llPairToken.setEnabled(true);
        btnStart.setEnabled(false);
    }

    @Background
    public void testLatched() {
        LatchApp latch = new LatchApp(LatchConfig.APP_ID,LatchConfig.SECRET_KEY);
        LatchResponse response = latch.status(latchAccountID,LatchConfig.OPERATION_ID);
        Log.d(TAG,"Test Latch: " + response.toJSON().toString());
        if (response.getError() != null) {
            setError(response.getError().getMessage(),true);
            isLatched = false;
            updateUI();
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
        LatchApp latch = new LatchApp(LatchConfig.APP_ID,LatchConfig.SECRET_KEY);
        String pairCode = etPairToken.getText().toString();
        Log.v(TAG, "Pair code: " + pairCode);
        LatchResponse pairResponse = latch.pair(pairCode);
        Log.d(TAG, pairResponse.toJSON().toString());

        if (pairResponse.getError() != null) {
            if (pairResponse.getError().getCode() != 205) {
                // 205 is already latched, use accountID as regular pair
                setError(pairResponse.getError().getMessage(), true);
                return;
            }
        }

        String accountID = pairResponse.getData().get("accountId").getAsString();
        updateLatchAccountID(accountID);
        updateScreen();
    }

    @Click
    @Background
    public void btnUnpairToken() {
        setError("",false);
        LatchApp latch = new LatchApp(LatchConfig.APP_ID,LatchConfig.SECRET_KEY);
        LatchResponse result = latch.unpair(preferences.latchAccountId().get());
        Log.d(TAG,"unpair result: " + result.toJSON().toString());
        updateLatchAccountID(null);
        updateScreen();
    }

    private void updateLatchAccountID(String latchAccountID) {
        if (latchAccountID != null) {
            preferences.latchAccountId().put(latchAccountID);
            Log.d(TAG, "Saving accountID = " + latchAccountID);
        } else {
            preferences.latchAccountId().remove();
            Log.d(TAG, "Remove LATCH_ACCOUNT from sharedPref");
        }
        this.latchAccountID = latchAccountID;
    }


    @Click
    @Trace
    public void btnStart() {
        try {
            preferences.whenNoConnection().put(rbOn.isChecked() ? 0 : rbOff.isChecked() ? 1 : 2);
            preferences.remoteLock().put(cbRemoteLock.isChecked());
            sendNotifRegistrationToServer(cbRemoteLock.isChecked());

            Log.d(TAG,"rbon: " + rbOn.isChecked());
            Log.d(TAG,"rboff: " + rbOff.isChecked());
            Log.d(TAG,"rblast: " + rbLast.isChecked());

            if (rbOn.isChecked() || rbLast.isChecked()) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(getString(R.string.alert))
                        .setMessage(getString(R.string.permanentLockAlert))
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                LatchdroidService_.intent(getApplication()).start();
                                finish();
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                return;
                            }
                            })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();

            } else {
                LatchdroidService_.intent(getApplication()).start();
                finish();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @OnActivityResult(ADMIN_RESULT)
    protected void onActivityResult() {
        updateScreen();
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Background
    public void sendNotifRegistrationToServer(boolean register)  {
        try {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            String firebaseToken = sharedPref.getString(FirebaseTokenService.FIREBASE_TOKEN, "");

            Log.d(TAG, "sendRegistrationToServer(" + register + ") " + firebaseToken);
            String url = "http://latch.pepenieto.net/updateFirebaseToken.php";
            Map<String, String> data = new HashMap<String, String>();
            data.put("firebaseToken", firebaseToken);
            data.put("latchAccountID", this.latchAccountID);
            if (!register) {
                data.put("unregister","true");
            }

            LatchAuth latchAuth = new LatchAuth();
            JsonElement result = latchAuth.HTTP_POST(url, new HashMap<String, String>(), data);
            Log.d(TAG, "result: " + result.toString());
        } catch (Exception e) {
            setError(e.getMessage(),false);
            e.printStackTrace();
        }
    }


}
