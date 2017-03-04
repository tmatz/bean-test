package io.github.tmatz.beantest;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.punchthrough.bean.sdk.Bean;
import com.punchthrough.bean.sdk.upload.SketchHex;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

public class DetailActivity extends AppCompatActivity {

    private static final String Tag = DetailActivity.class.getSimpleName();

    private Bean mBean;
    private CompositeSubscription mCompositeSubscription = new CompositeSubscription();
    private Subscription mConnectionSubscription;
    private Button mButtonUploadSketch;
    private TextView mTextViewBeanName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(Tag, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        if (intent != null) {
            mBean = intent.getParcelableExtra("Bean");
        }

        if (mBean == null) {
            Log.d(Tag, "Bean is not found.");
        }

        mTextViewBeanName = (TextView)findViewById(R.id.bean_name);
        mButtonUploadSketch = (Button)findViewById(R.id.action_upload_bean);
        mButtonUploadSketch.setEnabled(false);

        if (mBean != null) {
            Log.d(Tag, "Bean name: " + mBean.getDevice().getName());
            mTextViewBeanName.setText(mBean.getDevice().getName());

            mButtonUploadSketch.setOnClickListener(
                    view -> {
                        try {
                            InputStream inputStream = getResources().openRawResource(R.raw.sleep_ino_hex);
                            String hex = IOUtils.toString(inputStream, "ASCII");
                            SketchHex sketchHex = SketchHex.create("Sleep", hex);
                            mBean.programWithSketch(
                                    sketchHex,
                                    progress -> {
                                        Log.i(Tag, "Upload sketch progress. (" + progress.blocksSent() + "/" + progress.totalBlocks());
                                    },
                                    () -> {
                                        Log.i(Tag, "Upload sketch finished.");
                                    });
                        } catch (Throwable e) {
                            Log.e(Tag, e.getMessage());
                        }
                    });

            StartConnection(mBean);
        }
    }

    private void StartConnection(Bean bean)
    {
        if (mConnectionSubscription != null) {
            mConnectionSubscription.unsubscribe();
            mConnectionSubscription = null;
        }

        mConnectionSubscription = BeanConnect.ConnectToBean(getApplicationContext(), bean)
                // .timeout(10, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        event -> {
                            switch (event.getEventType()) {
                                case BeanConnect.BeanEvent.INFO_CONNECTED:
                                    mButtonUploadSketch.setEnabled(true);
                                    break;

                                case BeanConnect.BeanEvent.INFO_READ_REMOTE_RSSI:
                                    break;

                                case BeanConnect.BeanEvent.INFO_SCRATCH_VALUE_CHANGED:
                                    break;

                                case BeanConnect.BeanEvent.INFO_SERIAL_MESSAGE_RECEIVED:
                                    break;
                            }
                        },
                        error -> {
                            Log.d(Tag, "connection failed: " + error.toString());
                            mTextViewBeanName.setBackgroundColor(Color.RED);
                        },
                        () -> {

                        });
    }

    @Override
    protected void onStart() {
        Log.v(Tag, "onStart");
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCompositeSubscription.unsubscribe();
    }
}
