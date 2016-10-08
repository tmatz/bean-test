package io.github.tmatz.beantest;

import android.content.Context;
import android.util.Log;

import com.punchthrough.bean.sdk.Bean;
import com.punchthrough.bean.sdk.BeanDiscoveryListener;
import com.punchthrough.bean.sdk.BeanListener;
import com.punchthrough.bean.sdk.BeanManager;
import com.punchthrough.bean.sdk.message.BeanError;
import com.punchthrough.bean.sdk.message.ScratchBank;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;

public class BeanConnect {
    private static final String Tag = BeanConnect.class.getSimpleName();

    private Subscription mDiscoverySubscription;

    private static class MyBeanDiscoveryListener implements BeanDiscoveryListener
    {
        private Subscriber<? super Bean> observer;

        public void setObserver(Subscriber<? super Bean> subscriber) {
            observer = subscriber;
        }

        @Override
        public void onBeanDiscovered(Bean bean, int rssi) {
            Log.v(Tag, "BeanDiscoveryListener.onBeanDiscovered()");
            observer.onNext(bean);
        }

        @Override
        public void onDiscoveryComplete() {
            Log.v(Tag, "BeanDiscoveryListener.onDiscoveryComplete()");
            observer.onCompleted();
        }
    }

    public static Observable<Bean> DiscoverBeans() {
        return Observable.using(
                () -> new MyBeanDiscoveryListener(),
                (listener) -> Observable.create((subscriber) -> {
                    listener.setObserver(subscriber);
                    BeanManager.getInstance().startDiscovery(listener);
                }),
                (listener) -> BeanManager.getInstance().startDiscovery(listener));
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
            mBean = bean;
       }

        public Bean getBean() {
            return mBean;
        }
    }

    public static class BeanErrorException extends Throwable {

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

    private static class MyBeanListener implements BeanListener
    {
        private final Bean mBean;
        private Subscriber<? super BeanEvent> observer;

        public MyBeanListener(Bean bean) {
            mBean = bean;
        }

        public void start(Context context, Subscriber<? super BeanEvent> subscriber) {
            this.observer = subscriber;
            mBean.connect(context, this);
        }

        public void cancel() {
            mBean.disconnect();
        }

        @Override
        public void onConnected() {
            Log.i(Tag, "BeanListener.onConnected()");
            observer.onNext(BeanEvent.OnConnected(mBean));
        }

        @Override
        public void onConnectionFailed() {
            Log.i(Tag, "BeanListener.onConnectionFailed()");
            observer.onError(new BeanConnectionFailedException(mBean));
        }

        @Override
        public void onDisconnected() {
            Log.i(Tag, "BeanListener.onDiscconnected()");
            observer.onCompleted();
        }

        @Override
        public void onSerialMessageReceived(byte[] data) {
            Log.i(Tag, "BeanListener.onSerialMessageReceived()");
            observer.onNext(BeanEvent.OnSerialMessageReceived(mBean, data));
        }

        @Override
        public void onScratchValueChanged(ScratchBank bank, byte[] value) {
            Log.i(Tag, "BeanListener.onScratchValueChanged()");
            observer.onNext(BeanEvent.OnScratchValueChanged(mBean, bank, value));
        }

        @Override
        public void onError(BeanError error) {
            Log.i(Tag, "BeanListener.onError()");
            observer.onError(new BeanErrorException(mBean, error));
        }

        @Override
        public void onReadRemoteRssi(int rssi) {
            Log.i(Tag, "BeanListener.onReadRemoteRssi()");
            observer.onNext(BeanEvent.OnReadRemoteRssi(mBean, rssi));
        }
    }

    public static Observable<BeanEvent> ConnectToBean(Context context, Bean bean)
    {
        return Observable.using(
                () -> new MyBeanListener(bean),
                (listener) ->
                    Observable.create(subscriber -> {
                        listener.start(context, subscriber);
                    }),
                (listener) -> listener.cancel());
    }
}
