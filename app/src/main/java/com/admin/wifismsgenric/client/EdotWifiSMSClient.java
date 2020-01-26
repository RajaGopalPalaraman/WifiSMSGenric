package com.admin.wifismsgenric.client;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;

import java.util.List;

public class EdotWifiSMSClient implements RemoteClient<String,String> {

    public static final int CONNECTING = 0;
    public static final int CONNECTED = 1;
    public static final int DIS_CONNECTED = 2;

    private static final String LOG_TAG = "WorkerThreadLogTag";

    private int id = -1;

    private int status;

    protected String ipAddress;
    protected int port;

    private WorkerThread receiverThread;
    protected Handler handler;

    public EdotWifiSMSClient(@NonNull String ipAddress, int port, @NonNull Handler handler)
    {
        if (Thread.currentThread() != handler.getLooper().getThread()) {
            throw new IllegalArgumentException("Handler is not associated with current thread");
        }
        this.ipAddress = ipAddress;
        this.port = port;
        this.handler = handler;
    }

    private synchronized int generateId() {
        id++;
        id = id % 100;
        return id;
    }

    @Override
    public void connect(@NonNull final CallBack<String> callBack) {
        if (status == CONNECTED) {
            callBack.onConnection(true);
            return;
        }
        if (receiverThread == null) {
            status = CONNECTING;
            receiverThread = new WorkerThread(ipAddress,port);
            receiverThread.start();
        }
        if (!receiverThread.connect(new CallBack<String>() {
            @Override
            public void onConnection(final boolean success) {
                if (status != DIS_CONNECTED) {
                    status = success ? CONNECTED : DIS_CONNECTED;
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callBack.onConnection(success);
                    }
                });
            }

            @Override
            public void onData(int id, boolean success, @Nullable String data) {

            }

            @Override
            public void onSent(int id, boolean success) {

            }
        })) {
            throw new IllegalStateException("Closed Object");
        }
        status = CONNECTING;
    }

    @Override
    public int receive(@NonNull final CallBack<String> callBack) {
        if (status == DIS_CONNECTED) {
            throw new IllegalStateException("Connection not established");
        }
        int id = generateId();
        if (!receiverThread.receive(id, new CallBack<String>() {
            @Override
            public void onConnection(boolean success) {

            }

            @Override
            public void onData(final int id, final boolean success, @Nullable final String data) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callBack.onData(id,success,data);
                    }
                });
            }

            @Override
            public void onSent(int id, boolean success) {

            }
        })) {
            throw new IllegalStateException("Closed Object");
        }
        return id;
    }

    @Override
    public int send(@NonNull final CallBack<String> callBack, String data) {
        if (status == DIS_CONNECTED) {
            throw new IllegalStateException("Connection not established");
        }
        int id = generateId();
        if (!receiverThread.send(id, data, new CallBack<String>() {
            @Override
            public void onConnection(boolean success) {

            }

            @Override
            public void onData(int id, boolean success, @Nullable String data) {

            }

            @Override
            public void onSent(final int id, final boolean success) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callBack.onSent(id,success);
                    }
                });
            }
        })) {
            throw new IllegalStateException("Closed Object");
        }
        return id;
    }

    @Override
    public void disConnect() {
        if (status != DIS_CONNECTED) {
            status = DIS_CONNECTED;
            receiverThread.disConnect();
        }
    }

    @Override
    public int getConnectionState() {
        return status;
    }

    protected static class WorkerThread extends Thread {
        //Operation Constants
        private static final int OPERATION_CONNECT = 0;
        private static final int OPERATION_DISCONNECT = 1;
        private static final int OPERATION_SEND = 2;
        private static final int OPERATION_RECEIVE = 3;
        private static final int OPERATION_DEQUEUE_PRE_FETCHED_REQUEST = 4;

        private static final int TIME_OUT = 3000;

        private static final String DELIMITER = "\\$";

        private Handler internalHandler;

        private String ip;
        private int port;

        private Socket socket;
        private BufferedInputStream bufferedInputStream;
        private EdotWifiStreamReader wifiStreamReader;
        private OutputStream outputStream;

        private boolean handlerReady = false;
        private boolean connected = false;

        private final List<RequestWrapper> requestLists = new ArrayList<>();
        private static final RequestWrapper DISCONNECT_REQUEST_WRAPPER = new RequestWrapper(OPERATION_DISCONNECT,0,null);

        private static class RequestWrapper {
            private int operationType;
            private String data;
            private int id;
            private CallBack<String> callBack;

            private RequestWrapper(int operationType,int id, CallBack<String> callBack) {
                this.operationType = operationType;
                this.id = id;
                this.callBack = callBack;
            }
        }

        private WorkerThread(@NonNull String ip, int port) {
            this.ip = ip;
            this.port = port;
        }

        private void initialize() throws IOException {
            socket = new Socket();
            socket.connect(new InetSocketAddress(ip,port),TIME_OUT);

            bufferedInputStream = new BufferedInputStream(socket.getInputStream());
            outputStream = socket.getOutputStream();

            wifiStreamReader = new EdotWifiStreamReader(bufferedInputStream);
            connected = true;
        }

        private void loop() {
            Looper.prepare();
            internalHandler = new Handler(Looper.myLooper()){
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    if (msg.what == OPERATION_DEQUEUE_PRE_FETCHED_REQUEST) {
                        for (RequestWrapper requestWrapper : requestLists)
                        {
                            processRequest(requestWrapper);
                        }
                    } else {
                        processRequest((RequestWrapper) msg.obj);
                    }
                }
            };

            internalHandler.sendEmptyMessage(OPERATION_DEQUEUE_PRE_FETCHED_REQUEST);
            handlerReady = true;
            Looper.loop();
        }

        private synchronized boolean isConnected() {
            return connected;
        }

        protected boolean connect(@NonNull CallBack<String> callback) {
            if (handlerReady) {
                Message message = new Message();
                message.what = OPERATION_CONNECT;
                message.obj = new RequestWrapper(OPERATION_CONNECT,0,callback);
                return internalHandler.sendMessage(message);
            } else {
                requestLists.add(new RequestWrapper(OPERATION_CONNECT, 0, callback));
            }
            return true;
        }

        protected void disConnect() {
            if (handlerReady) {
                Message message = new Message();
                message.what = OPERATION_DISCONNECT;
                message.obj = DISCONNECT_REQUEST_WRAPPER;
            } else {
                requestLists.add(DISCONNECT_REQUEST_WRAPPER);
            }
        }

        protected boolean receive(int id, @NonNull CallBack<String> callback) {
            if (handlerReady) {
                Message message = new Message();
                message.what = OPERATION_RECEIVE;
                message.obj = new RequestWrapper(OPERATION_RECEIVE,id,callback);
                return internalHandler.sendMessage(message);
            } else {
                requestLists.add(new RequestWrapper(OPERATION_RECEIVE,id,callback));
            }
            return true;
        }

        protected boolean send(int id, String data,@NonNull CallBack<String> callback) {
            final RequestWrapper requestWrapper = new RequestWrapper(OPERATION_SEND,id,callback);
            requestWrapper.data = data;
            if (isConnected()) {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... voids) {
                        processRequest(requestWrapper);
                        return null;
                    }
                }.execute();
                return true;
            }
            return false;
        }

        private void processRequest(@NonNull RequestWrapper requestWrapper) {
            switch (requestWrapper.operationType)
            {
                case OPERATION_CONNECT:
                    try {
                        if (!isConnected()) {
                            initialize();
                            requestWrapper.callBack.onConnection(true);
                        }
                    } catch (IOException e) {
                        Log.d(LOG_TAG,"Connection cannot be established!!",e);
                        try {
                            dicConnectInternal();
                        } catch (IOException e1) {
                            Log.d(LOG_TAG,"Exception while disconnecting from server",e1);
                        }
                        requestWrapper.callBack.onConnection(false);
                    }
                    break;
                case OPERATION_SEND:
                    if (isConnected()) {
                        if (requestWrapper.data != null) {
                            Log.d(LOG_TAG, "Data Sent: " + requestWrapper.data);
                            try {
                                outputStream.write(requestWrapper.data.getBytes());
                                requestWrapper.callBack.onSent(requestWrapper.id,true);
                            } catch (IOException e) {
                                Log.d(LOG_TAG, "Unable to send data");
                                requestWrapper.callBack.onSent(requestWrapper.id,false);
                                try {
                                    dicConnectInternal();
                                } catch (IOException ex) {
                                    Log.d(LOG_TAG,"Exception while disconnecting from server",e);
                                }
                            }
                        }
                    } else {
                        Log.d(LOG_TAG,"At no connection");
                        requestWrapper.callBack.onSent(requestWrapper.id, false);
                    }
                    break;
                case OPERATION_RECEIVE:
                    if (isConnected()) {
                        String data = wifiStreamReader.getNext();
                        if (data != null ) {
                            Log.d(LOG_TAG, "Received Data: " + data);
                            requestWrapper.callBack.onData(requestWrapper.id,true,data);
                        } else {
                            Log.d(LOG_TAG,"At no data");
                            try {
                                dicConnectInternal();
                            } catch (IOException e) {
                                Log.d(LOG_TAG,"Exception while disconnecting from server",e);
                            }
                            requestWrapper.callBack.onData(requestWrapper.id,false,null);
                        }
                    } else {
                        Log.d(LOG_TAG,"At no connection");
                        requestWrapper.callBack.onData(requestWrapper.id, false, null);
                    }
                    break;
                case OPERATION_DISCONNECT:
                    if (isConnected()) {
                        try {
                            dicConnectInternal();
                        } catch (IOException e) {
                            Log.d(LOG_TAG, "Exception while disconnecting from server", e);
                        }
                    }
            }
        }

        private void dicConnectInternal() throws IOException {

            connected = false;

            if (wifiStreamReader != null)
            {
                wifiStreamReader.close();
                wifiStreamReader = null;
                bufferedInputStream = null;
            }
            if (outputStream != null)
            {
                outputStream.close();
                outputStream = null;
            }
            if (socket != null) {
                socket.close();
                socket = null;
            }

            //Stopping Thread
            Looper looper = Looper.myLooper();
            if (looper != null) {
                looper.quitSafely();
            }

        }

        @Override
        public void run() {
            super.run();
            loop();
        }
    }

}
