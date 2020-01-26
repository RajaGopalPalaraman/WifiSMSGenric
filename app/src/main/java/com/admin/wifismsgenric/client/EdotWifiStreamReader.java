package com.admin.wifismsgenric.client;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

public final class EdotWifiStreamReader implements Closeable {

    private static final char DELIMITER = '$';

    private final InputStream inputStream;

    public EdotWifiStreamReader(@NonNull InputStream inputStream) {
        this.inputStream = inputStream;
    }

    @Nullable
    public String getNext()
    {
        StringBuilder s = new StringBuilder();
        int tempInt = 0;
        char tempChar;
        while (true)
        {
            try {
                Log.d("Debugger",""+tempInt);
                if (((tempInt = inputStream.read()) == -1)) {
                    break;
                }
                else if (tempInt == DELIMITER)
                {
                    return s.toString();
                }
                else
                {
                    tempChar = (char) tempInt;
                    s.append(tempChar);
                }
            } catch (IOException e) {
                Log.d("Debugger","error",e);
            }

        }
        return null;
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }
}
