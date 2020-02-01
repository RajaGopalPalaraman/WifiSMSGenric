package com.admin.wifismsgenric.client;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface RemoteClient<S,R> {

    int CONNECTING = 0;
    int CONNECTED = 1;
    int DIS_CONNECTED = 2;

    interface CallBack<V>
    {
        void onConnection(boolean success);
        void onData(int id, boolean success,@Nullable V data);
        void onSent(int id, boolean success);
    }

    void connect(@NonNull CallBack<R> callBack);
    int receive(@NonNull CallBack<R> callBack);
    int send(@NonNull CallBack<R> callBack,S data);
    void disConnect();

    int getConnectionState();

}
