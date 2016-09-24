package io.github.tmatz.beantest;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
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

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;

public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;

    private boolean mDiscoveryCompleted = true;

    private Observable<Bean> BeanDiscoveryListenerWrapper() {
        return Observable.create(new Observable.OnSubscribe<Bean>() {
            @Override
            public void call(final Subscriber<? super Bean> subscriber) {
                mDiscoveryCompleted = false;
                BeanDiscoveryListener listener = new BeanDiscoveryListener() {
                    @Override
                    public void onBeanDiscovered(Bean bean, int rssi) {
                        System.out.println("BeanDiscoveryListener.onBeanDiscovered()");
                        subscriber.onNext(bean);
                    }

                    @Override
                    public void onDiscoveryComplete() {
                        System.out.println("BeanDiscoveryListener.onDiscoveryComplete()");
                        mDiscoveryCompleted = true;
                        subscriber.onCompleted();
                    }
                };
                BeanManager.getInstance().startDiscovery(listener);
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("MainActivity.onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BeanManager.getInstance().setScanTimeout(10);

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            System.out.println("ACCESS_COARSE_LOCATION is not granted.");

            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                //  show explanation.
                System.out.println("need explanation.");
                // TODO: do async
                ActivityCompat.requestPermissions(
                        this,
                        new String[] { Manifest.permission.ACCESS_COARSE_LOCATION },
                        MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
            } else {
                System.out.println("request permission.");
                ActivityCompat.requestPermissions(
                        this,
                        new String[] { Manifest.permission.ACCESS_COARSE_LOCATION },
                        MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
            }
        } else {
            StartDiscovery();
        }
    }

    @Override
    protected void onResume() {
        System.out.println("MainActivity.onResume()");
        super.onResume();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        System.out.println("MainActivity.onRequestPermissionsResult()");
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    StartDiscovery();
                }
            }
        }
    }

    private Subscription mDiscoverySubscription;

    private void StartDiscovery()
    {
        System.out.println("MainActivity.StartDiscovery()");
        mDiscoveryCompleted = false;
        mDiscoverySubscription = BeanDiscoveryListenerWrapper()
                .distinct(bean -> bean.getDevice().getAddress())
                .subscribe(bean -> {
                    System.out.println("onNext()");
                    System.out.println(bean.getDevice().getName());
                    System.out.println(bean.getDevice().getAddress());
                });
    }

    private void StopDiscovery()
    {
        if (!mDiscoveryCompleted) {
            BeanManager.getInstance().cancelDiscovery();
        }

        if (mDiscoverySubscription != null)
        {
            mDiscoverySubscription.unsubscribe();
            mDiscoverySubscription = null;
        }
    }

    @Override
    protected void onPause() {
        System.out.println("MainActivity.onPause()");
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        System.out.println("MainActivity.onDestroy()");
        super.onDestroy();
        StopDiscovery();
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
