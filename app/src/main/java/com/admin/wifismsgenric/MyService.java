package com.admin.wifismsgenric;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.admin.wifismsgenric.client.EdotWifiSMSClient;
import com.admin.wifismsgenric.client.RemoteClient;

import java.util.ArrayList;

public class MyService extends Service {

    public static final String IP_ADDRESS = "ipAddress";
    public static final String PORT_NUMBER = "portNumber";
    public static final String MOBILE_NUMBERS = "mobileNumbers";
    public static final String SMS_RECEIVED = "smsReceived";

    private static final String NOTIFICATION_CHANNEL_ID = "channelID";
    private static final int NOTIFICATION_ID = 78;

    public static final int SMS_SENT_PENDING_INTENT_REQUEST_CODE = 10;

    private static final String TAG = "MyServiceClassLogTag";

    private Handler handler;
    private SmsManager smsManager;
    private NotificationCompat.Builder notificationBuilder;
    private SmsReceiverBroadcast smsReceiverBroadcast;

    private ArrayList<Integer> arrayList = new ArrayList<>();

    private RemoteClient<String,String> remoteClient;
    private RemoteClient.CallBack<String> remoteCallBack = new RemoteClient.CallBack<String>() {
        @Override
        public void onConnection(boolean success) {
            if (success) {
                Log.d(TAG,"Connected");
                startForeground(NOTIFICATION_ID,notificationBuilder.build());
                arrayList.add(remoteClient.receive(this));
            }
            else {
                Log.d(TAG,"DisConnected");
                stopSelf();
            }
        }

        @Override
        public void onData(int id, boolean success, @Nullable String data) {
            if (arrayList.contains(id))
            {
                arrayList.remove((Integer) id);
                if (success && data != null) {
                    Log.d(TAG,"Data Received: "+data);
                    sendSMS(data);
                    arrayList.add(remoteClient.receive(this));
                }
                else {
                    Log.d(TAG,"Error while receiving data");
                    stopSelf();
                }
            }
        }

        @Override
        public void onSent(int id, boolean success) {
            if (success) {
                Log.d(TAG, "Data Sent Successfully");
                Toast.makeText(MyService.this, R.string.commandForwardSuccess,
                        Toast.LENGTH_SHORT).show();
            } else {
                Log.d(TAG,"Error while sending data");
                stopSelf();
            }
        }
    };

    private void sendSMS(@NonNull String data) {
        if (!data.equalsIgnoreCase(previousMessage)) {
            previousMessage = data;
            if (smsManager != null) {
                for (String mobileNumber : mobileNumbers) {
                    smsManager.sendTextMessage(mobileNumber, null, data,
                            PendingIntent.getBroadcast(this,SMS_SENT_PENDING_INTENT_REQUEST_CODE,
                                    new Intent(this,SMSBroadCast.class),0), null);
                }
            }
        }
    }

    private void createNotification()
    {
        if (notificationBuilder == null) {
            String id = NOTIFICATION_CHANNEL_ID;
            String name = "Connectivity Notification Channel";
            String description = "Shows current status of Connectivity with kit";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                int importance = NotificationManager.IMPORTANCE_DEFAULT;
                NotificationChannel notificationChannel = new NotificationChannel(id, name, importance);
                notificationChannel.setDescription(description);

                NotificationManager notificationManager = getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(notificationChannel);
            }
            notificationBuilder = new NotificationCompat.Builder(this,id)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Kit Status")
                    .setContentText("Connected")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(false);
        }
    }

    private String[] mobileNumbers;

    private String previousMessage = "";
    private boolean serviceStarted = false;

    @Override
    public void onCreate() {
        super.onCreate();
        smsManager = SmsManager.getDefault();
        handler = new Handler(Looper.getMainLooper());
        createNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!serviceStarted)
        {
            if (intent.getStringExtra(IP_ADDRESS) != null)
            {
                String ip = intent.getStringExtra(IP_ADDRESS);

                int port = intent.getIntExtra(PORT_NUMBER,80);
                mobileNumbers = intent.getStringArrayExtra(MOBILE_NUMBERS);

                remoteClient = new EdotWifiSMSClient(ip,port,handler);
                remoteClient.connect(remoteCallBack);

                registerSMSReceiveBroadcast();
                serviceStarted = true;
            }
            else {
                stopSelf();
            }
            Toast.makeText(this,R.string.serviceStarted,Toast.LENGTH_SHORT).show();
        } else if (intent.getStringExtra(SMS_RECEIVED) != null) {
            String number = intent.getStringExtra(MOBILE_NUMBERS);
            for (String mobile : mobileNumbers) {
                if (mobile.equals(number)) {
                    Log.d(TAG, "Message received: " + intent.getStringExtra(SMS_RECEIVED));
                    remoteClient.send(remoteCallBack, intent.getStringExtra(SMS_RECEIVED));
                    break;
                }
            }
        }
        return START_NOT_STICKY;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    public void changeSim(int subscriptionID) {
        smsManager = SmsManager.getSmsManagerForSubscriptionId(subscriptionID);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (remoteClient != null) {
            remoteClient.disConnect();
        }
        Notification notification = notificationBuilder
                .setAutoCancel(true)
                .setContentText("DisConnected")
                .build();
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID,notification);
        notificationBuilder = null;
        Toast.makeText(this,R.string.serviceStopped,Toast.LENGTH_SHORT).show();
        unregisterSMSReceiverBroadcast();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void registerSMSReceiveBroadcast() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
        smsReceiverBroadcast = new SmsReceiverBroadcast();
        registerReceiver(smsReceiverBroadcast, intentFilter);
    }

    private void unregisterSMSReceiverBroadcast() {
        if (smsReceiverBroadcast != null) {
            unregisterReceiver(smsReceiverBroadcast);
            smsReceiverBroadcast = null;
        }
    }

}
