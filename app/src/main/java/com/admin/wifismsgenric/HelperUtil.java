package com.admin.wifismsgenric;

import android.app.Application;
import android.content.Context;
import android.net.wifi.WifiManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

public final class HelperUtil {

    public static final int PORT_TO_CONNECT = 80;

    @Nullable
    public static String getWifiServerIP(@NonNull Application context)
    {
        if (((WifiManager) context.getSystemService(Context.WIFI_SERVICE)).getConnectionInfo().getIpAddress() != 0) {
            int address = ((WifiManager) context.getSystemService(Context.WIFI_SERVICE)).getDhcpInfo().serverAddress;
            return String.format(Locale.ENGLISH,
                    "%d.%d.%d.%d", address & 0xFF, address >> 8 & 0xFF, address >> 16 & 0xFF, address >> 24 & 0xFF);
        }
        return null;
    }

}
