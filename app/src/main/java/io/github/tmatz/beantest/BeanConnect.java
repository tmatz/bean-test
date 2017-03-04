package io.github.tmatz.beantest;

import android.content.Context;
import android.util.Log;

import com.punchthrough.bean.sdk.Bean;
import com.punchthrough.bean.sdk.BeanDiscoveryListener;
import com.punchthrough.bean.sdk.BeanListener;
import com.punchthrough.bean.sdk.BeanManager;
import com.punchthrough.bean.sdk.message.BeanError;
import com.punchthrough.bean.sdk.message.ScratchBank;

import rx.AsyncEmitter;
import rx.Observable;

public class BeanConnect {
    private static final String Tag = BeanConnect.class.getSimpleName();

    private static class MyBeanDiscoveryListener implements BeanDiscoveryListener {
        private final AsyncEmitter<Bean> mEmitter;
        private boolean mIsCompleted = false;

        public MyBeanDiscoveryListener(AsyncEmitter<Bean> emitter) {
            mEmitter = emitter;
        }

        @Override
        public void onBeanDiscovered(Bean bean, int rssi) {
            Log.v(Tag, "BeanDiscoveryListener.onBeanDiscovered()");
            mEmitter.onNext(bean);
        }

        @Override
        public void onDiscoveryComplete() {
            Log.v(Tag, "BeanDiscoveryListener.onDiscoveryComplete()");
            mIsCompleted = true;
            mEmitter.onCompleted();
        }

        public void cancel() {
            if (!mIsCompleted) {
                BeanManager.getInstance().cancelDiscovery();
            }
        }
    }

    public static Observable<Bean> DiscoverBeans() {
        return Observable.fromEmitter(
                emitter -> {
                    MyBeanDiscoveryListener listener = new MyBeanDiscoveryListener(emitter);
                    BeanManager.getInstance().startDiscovery(listener);
                    emitter.setCancellation(() -> listener.cancel());
                }, AsyncEmitter.BackpressureMode.BUFFER);
    }

    public static class BeanEvent {
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
    
    public static class BeanConnectionFailedException extends Throwable {
        private final Bean mBean;

        public BeanConnectionFailedException(Bean bean) {
            super("Connection Failed.");
            mBean = bean;
       }

        public Bean getBean() {
            return mBean;
        }
    }

    public static class BeanErrorException extends Throwable {

        private final Bean mBean;
        private final BeanError mError;

        public BeanErrorException(Bean bean, BeanError error)
        {
            super(error.toString());
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

    public static Observable<BeanEvent> ConnectToBean(Context context, Bean bean)
    {
        return Observable.fromEmitter(
                emitter -> {
                    BeanListener listener = new BeanListener() {
                        @Override
                        public void onConnected() {
                            Log.v(Tag, "BeanListener.onConnected()");
                            emitter.onNext(BeanEvent.OnConnected(bean));
                        }

                        @Override
                        public void onConnectionFailed() {
                            Log.v(Tag, "BeanListener.onConnectionFailed()");
                            emitter.onError(new BeanConnectionFailedException(bean));
                        }

                        @Override
                        public void onDisconnected() {
                            Log.v(Tag, "BeanListener.onDiscconnected()");
                            emitter.onCompleted();
                        }

                        @Override
                        public void onSerialMessageReceived(byte[] data) {
                            Log.v(Tag, "BeanListener.onSerialMessageReceived()");
                            emitter.onNext(BeanEvent.OnSerialMessageReceived(bean, data));
                        }

                        @Override
                        public void onScratchValueChanged(ScratchBank bank, byte[] value) {
                            Log.v(Tag, "BeanListener.onScratchValueChanged()");
                            emitter.onNext(BeanEvent.OnScratchValueChanged(bean, bank, value));
                        }

                        @Override
                        public void onError(BeanError error) {
                            Log.v(Tag, "BeanListener.onError()" + error.toString());
                            emitter.onError(new BeanErrorException(bean, error));
                        }

                        @Override
                        public void onReadRemoteRssi(int rssi) {
                            Log.v(Tag, "BeanListener.onReadRemoteRssi()");
                            emitter.onNext(BeanEvent.OnReadRemoteRssi(bean, rssi));
                        }
                    };

                    bean.connect(context, listener);

                    emitter.setCancellation(
                            () -> {
                                bean.disconnect();
                            });

                }, AsyncEmitter.BackpressureMode.BUFFER);
    }
}
