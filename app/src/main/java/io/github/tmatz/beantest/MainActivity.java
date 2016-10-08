package io.github.tmatz.beantest;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.punchthrough.bean.sdk.Bean;
import com.punchthrough.bean.sdk.BeanDiscoveryListener;
import com.punchthrough.bean.sdk.BeanListener;
import com.punchthrough.bean.sdk.BeanManager;
import com.punchthrough.bean.sdk.message.BeanError;
import com.punchthrough.bean.sdk.message.ScratchBank;

import java.util.ArrayList;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class MainActivity extends AppCompatActivity {
    private static final String Tag = MainActivity.class.getSimpleName();

    private static final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;

    private CompositeSubscription mCompositeSubscription = new CompositeSubscription();
    private boolean mDiscoveryCompleted = true;
    private ListView mListView;
    private ArrayList<Bean> mBeans;
    private ArrayAdapter<Bean> mAdapter;

    private Observable<Bean> BeanDiscoveryListenerWrapper() {
        return Observable.create(observer ->
        {
            mDiscoveryCompleted = false;
            BeanDiscoveryListener listener = new BeanDiscoveryListener() {
                @Override
                public void onBeanDiscovered(Bean bean, int rssi) {
                    Log.v(Tag, "BeanDiscoveryListener.onBeanDiscovered()");
                    observer.onNext(bean);
                }

                @Override
                public void onDiscoveryComplete() {
                    Log.v(Tag, "BeanDiscoveryListener.onDiscoveryComplete()");
                    mDiscoveryCompleted = true;
                    observer.onCompleted();
                }
            };
            BeanManager.getInstance().startDiscovery(listener);
        });
    }

    @Override
    protected void onStart() {
        Log.v(Tag, "onStart()");
        super.onStart();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(Tag, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BeanManager.getInstance().setScanTimeout(10);

        setupListView();

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.v(Tag, "ACCESS_COARSE_LOCATION is not granted.");

            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                //  show explanation.
                Log.v(Tag, "need explanation.");
                // TODO: do async
                ActivityCompat.requestPermissions(
                        this,
                        new String[] { Manifest.permission.ACCESS_COARSE_LOCATION },
                        MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
            } else {
                Log.v(Tag, "request permission.");
                ActivityCompat.requestPermissions(
                        this,
                        new String[] { Manifest.permission.ACCESS_COARSE_LOCATION },
                        MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
            }
        } else {
            StartDiscovery();
        }
    }

    private void setupListView()
    {
        mListView = (ListView)findViewById(R.id.list);
        mBeans = new ArrayList<>();
        mAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mBeans);
        mListView.setAdapter(mAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MainActivity.this, DetailActivity.class);
                intent.putExtra(Intent.EXTRA_TEXT, mBeans.get(position).getDevice().getAddress());
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        Log.v(Tag, "onResume()");
        super.onResume();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        Log.v(Tag, "onRequestPermissionsResult()");
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
        Log.v(Tag, "StartDiscovery()");
        mDiscoveryCompleted = false;
        mDiscoverySubscription = BeanDiscoveryListenerWrapper()
                .distinct(bean -> bean.getDevice().getAddress())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(bean -> {
                    Log.v(Tag, "Bean found: " + bean.describe());
                    mBeans.add(bean);
                    mAdapter.notifyDataSetChanged();
                    // ShowBeanInfo(bean);
                });
        mCompositeSubscription.add(mDiscoverySubscription);
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
        Log.v(Tag, "onPause()");
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.v(Tag, "onStop()");
        StopDiscovery();
        mCompositeSubscription.unsubscribe();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.v(Tag, "onDestroy()");
        super.onDestroy();
    }

    private static class BeanEvent {
        public static final int INFO_CONNECTED = 1;
        public static final int INFO_SERIAL_MESSAGE_RECEIVED = 2;
        public static final int INFO_SCRATCH_VALUE_CHANGED = 3;
        public static final int INFO_READ_REMOTE_RSSI = 4;

        private int mEventType;
        private Bean mBean;
        private byte[] mData;
        private ScratchBank mBank;

        private int mRssi;

        private BeanEvent() {
        }
        
        public int getEventType() {
            return mEventType;
        }

        public Bean getBean() {
            return mBean;
        }

        public byte[] getData() {
            return mData;
        }

        public ScratchBank getBank() {
            return mBank;
        }

        public byte[] getVaue() {
            return mData;
        }

        public int getRssi() {
            return mRssi;
        }
        
        public static BeanEvent OnConnected(Bean bean) {
            BeanEvent event = new BeanEvent();
            event.mEventType = INFO_CONNECTED;
            event.mBean = bean;
            return event;
        }

        public static BeanEvent OnSerialMessageReceived(Bean bean, byte[] data) {
            BeanEvent event = new BeanEvent();
            event.mEventType = INFO_SERIAL_MESSAGE_RECEIVED;
            event.mBean = bean;
            event.mData = data;
            return event;
        }
        
        public static BeanEvent OnScratchValueChanged(Bean bean, ScratchBank bank, byte[] value) {
            BeanEvent event = new BeanEvent();
            event.mEventType = INFO_SCRATCH_VALUE_CHANGED;
            event.mBean = bean;
            event.mBank = bank;
            event.mData = value;
            return event;
        }
        
        public static BeanEvent OnReadRemoteRssi(Bean bean, int rssi) {
            BeanEvent event = new BeanEvent();
            event.mEventType = INFO_READ_REMOTE_RSSI;
            event.mBean = bean;
            event.mRssi = rssi;
            return event;
        }
    }
    
    private class BeanConnectionFailedException extends Throwable {
        private final Bean mBean;

        public BeanConnectionFailedException(Bean bean) {
            mBean = bean;
       }

        public Bean getBean() {
            return mBean;
        }
    }

    private class BeanErrorException extends Throwable {

        private final Bean mBean;
        private final BeanError mError;

        public BeanErrorException(Bean bean, BeanError error) {
            mBean = bean;
            mError = error;
        }

        public Bean getBean() {
            return mBean;
        }

        public BeanError getError() {
            return mError;
        }
    }

    private Observable<BeanEvent> ConnectToBean(final Bean bean)
    {
        Log.i(Tag, "MainActivity.ConnectToBean()");
        return Observable.create(observer ->
            {
                Log.i(Tag, "start connect to bean...");
                BeanListener listener = new BeanListener() {
                    @Override
                    public void onConnected() {
                        Log.i(Tag, "BeanListener.onConnected()");
                        observer.onNext(BeanEvent.OnConnected(bean));
                    }

                    @Override
                    public void onConnectionFailed() {
                        Log.i(Tag, "BeanListener.onConnectionFailed()");
                        observer.onError(new BeanConnectionFailedException(bean));
                    }

                    @Override
                    public void onDisconnected() {
                        Log.i(Tag, "BeanListener.onDiscconnected()");
                        observer.onCompleted();
                    }

                    @Override
                    public void onSerialMessageReceived(byte[] data) {
                        Log.i(Tag, "BeanListener.onSerialMessageReceived()");
                        observer.onNext(BeanEvent.OnSerialMessageReceived(bean, data));
                    }

                    @Override
                    public void onScratchValueChanged(ScratchBank bank, byte[] value) {
                        Log.i(Tag, "BeanListener.onScratchValueChanged()");
                        observer.onNext(BeanEvent.OnScratchValueChanged(bean, bank, value));
                    }

                    @Override
                    public void onError(BeanError error) {
                        Log.i(Tag, "BeanListener.onError()");
                        observer.onError(new BeanErrorException(bean, error));
                    }

                    @Override
                    public void onReadRemoteRssi(int rssi) {
                        Log.i(Tag, "BeanListener.onReadRemoteRssi()");
                        observer.onNext(BeanEvent.OnReadRemoteRssi(bean, rssi));
                    }
                };

                bean.connect(this, listener);
            });
    }

    private void ShowBeanInfo(Bean bean)
    {
        Observable<BeanEvent> eventObservable = ConnectToBean(bean);
        Subscription subscription = eventObservable
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(event ->
                        {
                            Log.i(Tag, "get BeanEvent");
                            switch (event.getEventType())
                            {
                                case BeanEvent.INFO_CONNECTED:
                                    Log.i(Tag, "BeanListener.onConnected()");
                                    break;

                                case BeanEvent.INFO_SERIAL_MESSAGE_RECEIVED:
                                    Log.i(Tag, "BeanListener.onSerialMessageReceived()");
                                    Log.i(Tag, new String(event.getData()));
                                    break;

                                case BeanEvent.INFO_SCRATCH_VALUE_CHANGED:
                                    Log.i(Tag, "BeanListener.onScratchValueChanged()");
                                    Log.i(Tag, event.getBank().name());
                                    break;

                                case BeanEvent.INFO_READ_REMOTE_RSSI:
                                    Log.i(Tag, "BeanListener.onReadRemoteRssi()");
                                    Log.i(Tag, String.valueOf(event.getRssi()));
                                    break;
                            }
                        },
                        ex ->
                        {
                            if (ex instanceof BeanConnectionFailedException) {
                                Log.i(Tag, "BeanListener.onConnectionFailed()");
                            } else if (ex instanceof BeanErrorException) {
                                Log.i(Tag, "BeanListener.onError()");
                                Log.i(Tag, ((BeanErrorException) ex).getError().name());
                            }
                        },
                        () ->
                        {
                            Log.i(Tag, "BeanListener.onCompleted()");
                        });

        mCompositeSubscription.add(subscription);
    }
}
