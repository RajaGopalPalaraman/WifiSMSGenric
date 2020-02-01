package com.admin.wifismsgenric;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import static android.content.Context.WIFI_SERVICE;

public class DashboardFragment extends Fragment {

    public static final String NUMBERS_ARRAY = "numbersArray";

    private static final String IP_HTML = "<b>IP</b>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;:&nbsp;&nbsp;&nbsp;";
    private static final String PORT_HTML = "<b>Port</b>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;:&nbsp;&nbsp;&nbsp;";
    private static final String STATUS_HTML = "<b>Status</b>&nbsp;&nbsp;&nbsp;:&nbsp;&nbsp;&nbsp;";

    private static final String NOT_CONNECTED_STRING = IP_HTML +
            "--<br/><br/>" + PORT_HTML + "--<br/><br/>";// + STATUS_HTML + "--<br/><br/>";

    private static final String CONNECTED = "Connected";
    private static final String DIS_CONNECTED = "Disconnected";
    private static final String CONNECTING = "Connecting";

    private static final int PORT = 80;

    private TextView errorMessageView;
    private TextView serverInfoView;
    private String ip;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_main,container,false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getContext().stopService(new Intent(getContext(),MyService.class));
        //Initialization
        errorMessageView = getActivity().findViewById(R.id.messageView);
        serverInfoView = getActivity().findViewById(R.id.serverInfoView);

        WifiManager wifiManager = (WifiManager) getContext().getApplicationContext().getSystemService(WIFI_SERVICE);

        if (!wifiManager.isWifiEnabled()) {
            errorMessageView.setVisibility(View.VISIBLE);
            errorMessageView.setText(R.string.wifiNotActive);

            setServerInfoView(NOT_CONNECTED_STRING);
            return;
        }

        ip = HelperUtil.getWifiServerIP(getActivity().getApplication());
        if (ip != null){
            if (isPermissionGranted()) {
                connectToServer();
            } else {
                errorMessageView.setVisibility(View.VISIBLE);
                errorMessageView.setText(R.string.permissionNotProvided);

                String html = IP_HTML + ip + "<br/><br/>" + PORT_HTML + PORT + "<br/><br/>";
                setServerInfoView(html);
            }
        } else {
            errorMessageView.setVisibility(View.VISIBLE);
            errorMessageView.setText(R.string.wifiNotConnected);

            setServerInfoView(NOT_CONNECTED_STRING);
        }
    }

    private void connectToServer()
    {
        Log.d("WifiLogTag",ip);

        Intent intent = new Intent(getContext(),MyService.class);
        intent.putExtra(MyService.IP_ADDRESS,ip);
        intent.putExtra(MyService.PORT_NUMBER,PORT);

        Bundle bundle = getArguments();
        intent.putExtra(MyService.MOBILE_NUMBERS,bundle.getStringArray(NUMBERS_ARRAY));

        getContext().startService(intent);

        errorMessageView.setVisibility(View.GONE);
        String html = IP_HTML + ip + "<br/><br/>" + PORT_HTML + PORT + "<br/><br/>";
        setServerInfoView(html);
    }

    private void setServerInfoView(@NonNull String html) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            serverInfoView.setText(Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY));
        } else {
            serverInfoView.setText(Html.fromHtml(html));
        }
    }

    private boolean isPermissionGranted() {
        return ContextCompat.checkSelfPermission(getContext(), Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        errorMessageView.setVisibility(View.GONE);

        errorMessageView = null;
        serverInfoView = null;
        ip = null;
    }
}
