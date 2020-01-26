package com.admin.wifismsgenric;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class SMSBroadCast extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null)
        {
            if (getResultCode() == Activity.RESULT_OK) {
                Toast.makeText(context,R.string.smsSent,Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(context,R.string.smsFailed,Toast.LENGTH_SHORT).show();
            }
        }
    }
}
