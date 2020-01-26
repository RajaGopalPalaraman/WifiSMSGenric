package com.admin.wifismsgenric;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

public class SmsReceiverBroadcast extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (messages != null && messages.length > 0) {
            context.startService(
                    new Intent(context, MyService.class)
                            .putExtra(MyService.SMS_RECEIVED, messages[0].getMessageBody())
                            .putExtra(MyService.MOBILE_NUMBERS, messages[0].getDisplayOriginatingAddress())
            );
        }

    }
}
