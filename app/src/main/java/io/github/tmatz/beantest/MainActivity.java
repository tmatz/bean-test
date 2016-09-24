package io.github.tmatz.beantest;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import com.punchthrough.bean.sdk.Bean;
import com.punchthrough.bean.sdk.BeanDiscoveryListener;
import com.punchthrough.bean.sdk.BeanListener;
import com.punchthrough.bean.sdk.BeanManager;
import com.punchthrough.bean.sdk.message.BeanError;
import com.punchthrough.bean.sdk.message.Callback;
import com.punchthrough.bean.sdk.message.DeviceInfo;
import com.punchthrough.bean.sdk.message.ScratchBank;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private final List<Bean> mBean = new ArrayList<>();
    private boolean mDiscoveryCompleted = true;
    private final BeanDiscoveryListener mDiscoveryListener = new BeanDiscoveryListener() {
        @Override
        public void onBeanDiscovered(Bean bean, int rssi) {
            System.out.println("BeanDiscoveryListener.onBeanDiscovered()");
            mBean.add(bean);
            ShowBeanInfo(bean);
        }

        @Override
        public void onDiscoveryComplete() {
            System.out.println("BeanDiscoveryListener.onDiscoveryComplete()");
            mDiscoveryCompleted = true;
            for (Bean bean: mBean)
            {
                System.out.println(bean.getDevice().getName());
                System.out.println(bean.getDevice().getAddress());
            }
            mBean.clear();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("MainActivity.onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BeanManager.getInstance().setScanTimeout(10);
    }

    @Override
    protected void onResume() {
        System.out.println("MainActivity.onResume()");
        super.onResume();

        int grant = ContextCompat.checkSelfPermission(
                getApplicationContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION);
        if (grant == PackageManager.PERMISSION_GRANTED) {
            mDiscoveryCompleted = false;
            BeanManager.getInstance().startDiscovery(mDiscoveryListener);
        } else {
        }

    }

    @Override
    protected void onPause() {
        System.out.println("MainActivity.onPause()");
        super.onPause();
        if (!mDiscoveryCompleted) {
            BeanManager.getInstance().cancelDiscovery();
        }
    }

    private void ShowBeanInfo(final Bean bean)
    {
        BeanListener beanListener = new BeanListener() {
            @Override
            public void onConnected() {
                System.out.println("BeanListener.onConnected()");
                bean.readDeviceInfo(new Callback<DeviceInfo>() {
                    @Override
                    public void onResult(DeviceInfo result) {
                        System.out.println(result.hardwareVersion());
                        System.out.println(result.firmwareVersion());
                        System.out.println(result.softwareVersion());
                    }
                });
            }

            @Override
            public void onConnectionFailed() {
                System.out.println("BeanListener.onConnectionFailed()");
            }

            @Override
            public void onDisconnected() {
                System.out.println("BeanListener.onDisconnected()");
            }

            @Override
            public void onSerialMessageReceived(byte[] data) {
                System.out.println("BeanListener.onSerialMessageReceived()");
            }

            @Override
            public void onScratchValueChanged(ScratchBank bank, byte[] value) {
                System.out.println("BeanListener.onScratchValueChanged()");
            }

            @Override
            public void onError(BeanError error) {
                System.out.println("BeanListener.onError()");
            }

            @Override
            public void onReadRemoteRssi(int rssi) {
                System.out.println("BeanListener.onReadRemoteRssi()");
            }
        };

        bean.connect(this, beanListener);
    }
}
