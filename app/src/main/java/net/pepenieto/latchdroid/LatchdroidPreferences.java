package net.pepenieto.latchdroid;

import org.androidannotations.annotations.sharedpreferences.DefaultBoolean;
import org.androidannotations.annotations.sharedpreferences.DefaultInt;
import org.androidannotations.annotations.sharedpreferences.DefaultString;
import org.androidannotations.annotations.sharedpreferences.SharedPref;

@SharedPref(value=SharedPref.Scope.UNIQUE)
public interface LatchdroidPreferences {

    String latchAccountId();

    @DefaultBoolean(false)
    boolean remoteLock();

    @DefaultInt(1)
    int whenNoConnection();

    @DefaultString("on")
    String lastStatus();
}
