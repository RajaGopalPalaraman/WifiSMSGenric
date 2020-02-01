package com.admin.wifismsgenric.client;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.net.Socket;

public class WifiSmsClient implements RemoteClient<String, String> {

    private String ip;
    private int port;

    private int status = DIS_CONNECTED;
    private ReceiverThread receiverThread;
    private SenderThread senderThread;
    private Handler handler;
    private int id;

    public WifiSmsClient(String ip, int port, Handler handler) {
        this.ip = ip;
        this.port = port;
        this.handler = handler;
    }

    @Override
    public void connect(@NonNull final CallBack<String> callBack) {
        if (getConnectionState() == DIS_CONNECTED) {
            status = CONNECTING;
        }
        receiverThread = new ReceiverThread(ip, port, new CallBack<String>() {
            @Override
            public void onConnection(boolean success) {
                if (success) {
                    senderThread = new SenderThread(receiverThread.socket);
                    senderThread.start();
                } else {
                    disConnect();
                }
                handler.post(() -> callBack.onConnection(success));
                status = success ? CONNECTED : DIS_CONNECTED;
            }

            @Override
            public void onData(int id, boolean success, @Nullable String data) {

            }

            @Override
            public void onSent(int id, boolean success) {

            }
        });
        receiverThread.start();
    }

    @Override
    public int receive(@NonNull final CallBack<String> callBack) {
        return receiverThread == null? 0 : receiverThread.receive(new CallBack<String>() {
            @Override
            public void onConnection(boolean success) {

            }

            @Override
            public void onData(int id, boolean success, @Nullable String data) {
                handler.post(() -> callBack.onData(id, success, data));
                if (!success) {
                    disConnect();
                }
            }

            @Override
            public void onSent(int id, boolean success) {

            }
        });
    }

    @Override
    public int send(@NonNull final CallBack<String> callBack, String data) {
        return senderThread == null? 0 : senderThread.send(data, new CallBack<String>() {
            @Override
            public void onConnection(boolean success) {

            }

            @Override
            public void onData(int id, boolean success, @Nullable String data) {

            }

            @Override
            public void onSent(int id, boolean success) {
                handler.post(() -> callBack.onSent(id, success));
                if (!success) {
                    disConnect();
                }
            }
        });
    }

    @Override
    public void disConnect() {
        if (senderThread != null) {
            senderThread.disConnectSafely();
            status = DIS_CONNECTED;
        }
        receiverThread = null;
        senderThread = null;
    }

    @Override
    public int getConnectionState() {
        return status;
    }

    private static class RequestWrapper {
        private String data;
        private int id;

        private CallBack<String> callBack;
    }

    private static class ReceiverThread extends Thread {
        private static final int RECEIVE_WHAT = 1;

        private final String ip;
        private final int port;

        private Socket socket;
        private EdotWifiStreamReader wifiStreamReader;
        private CallBack<String> callBack;
        private Handler internalHandler;
        private int id;

        private ReceiverThread(String ip, int port, CallBack<String> callBack) {
            this.ip = ip;
            this.port = port;
            this.callBack = callBack;
        }

        @Override
        public void run() {
            super.run();
            try {
                socket = new Socket(ip, port);
                wifiStreamReader = new EdotWifiStreamReader(socket.getInputStream());
                Looper.prepare();
                internalHandler = new Handler(Looper.myLooper()) {
                    @Override
                    public void handleMessage(@NonNull Message msg) {
                        super.handleMessage(msg);
                        processMessage(msg);
                    }
                };
                if (callBack != null) {
                    callBack.onConnection(true);
                    callBack = null;
                }
                Looper.loop();
            } catch (IOException ignored) {
                cleanUp();
                if (callBack != null) {
                    callBack.onConnection(false);
                    callBack = null;
                }
            }
        }

        private void processMessage(Message msg) {
            RequestWrapper requestWrapper = (RequestWrapper) msg.obj;

            if (wifiStreamReader == null) {
                if (requestWrapper.callBack != null) {
                    requestWrapper.callBack.onData(requestWrapper.id, false, null);
                }
            } else {
                String data = wifiStreamReader.getNext();
                if (data == null) {
                    cleanUp();
                    msg.getTarget().getLooper().quitSafely();
                    if (requestWrapper.callBack != null) {
                        requestWrapper.callBack.onData(requestWrapper.id, false, null);
                    }
                } else if (!data.isEmpty()) {
                    if (requestWrapper.callBack != null) {
                        requestWrapper.callBack.onData(requestWrapper.id, true, data);
                    }
                }
            }
        }

        private int receive(CallBack<String> callBack) {
            if (internalHandler != null) {
                RequestWrapper requestWrapper = new RequestWrapper();
                requestWrapper.id = generateId();
                requestWrapper.callBack = callBack;

                Message message = new Message();
                message.what = RECEIVE_WHAT;
                message.obj = requestWrapper;

                internalHandler.sendMessage(message);
                return requestWrapper.id;
            }
            return -1;
        }

        private synchronized int generateId() {
            id = id + 1;
            id = id % 100;
            return id;
        }

        private void cleanUp() {
            socket = null;
            internalHandler = null;
            wifiStreamReader = null;
        }

    }

    private static class SenderThread extends Thread {

        private static final int SEND_WHAT = 0;
        private static final int DISCONNECT_WHAT = 1;

        private Socket socket;
        private Handler internalHandler;
        private int id;

        private SenderThread(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            super.run();
            Looper.prepare();
            internalHandler = new Handler(Looper.myLooper()) {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    super.handleMessage(msg);
                    processMessage(msg);
                }
            };
            Looper.loop();
        }

        private int send(String data, CallBack<String> callBack) {
            Log.d("LogTag", "At send:" + data);
            if (internalHandler != null) {
                RequestWrapper requestWrapper = new RequestWrapper();
                requestWrapper.data = data;
                requestWrapper.id = generateId();
                requestWrapper.callBack = callBack;

                Message message = new Message();
                message.what = SEND_WHAT;
                message.obj = requestWrapper;

                internalHandler.sendMessage(message);
                return requestWrapper.id;
            }
            return -1;
        }

        private void disConnect() {
            if (internalHandler != null) {
                internalHandler.removeMessages(SEND_WHAT);
            }
            disConnectSafely();
        }

        private void disConnectSafely() {
            if (internalHandler != null) {
                internalHandler.sendEmptyMessage(DISCONNECT_WHAT);
            }
        }

        private void processMessage(@NonNull Message message) {
            switch (message.what) {
                case SEND_WHAT:
                    RequestWrapper requestWrapper = (RequestWrapper) message.obj;
                    try {
                        if (socket != null) {
                            socket.getOutputStream().write(requestWrapper.data.getBytes());
                            if (requestWrapper.callBack != null) {
                                requestWrapper.callBack.onSent(requestWrapper.id, true);
                            }
                        } else if (requestWrapper.callBack != null) {
                            requestWrapper.callBack.onSent(requestWrapper.id, false);
                        }
                    } catch (IOException ignored) {
                        if (requestWrapper.callBack != null) {
                            requestWrapper.callBack.onSent(requestWrapper.id, false);
                        }
                        disConnectSafely();
                    }
                    break;
                case DISCONNECT_WHAT:
                    try {
                        if (socket != null) {
                            socket.close();
                        }
                    } catch (IOException ignored) { }
                    if (internalHandler != null) {
                        internalHandler.getLooper().quitSafely();
                        internalHandler = null;
                    }
                    socket = null;
                    break;
            }
        }

        private synchronized int generateId() {
            id = id + 1;
            id = id % 100;
            return id;
        }

    }

}
