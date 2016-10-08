package io.github.tmatz.beantest;

import android.content.Intent;
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
import com.punchthrough.bean.sdk.BeanManager;
import com.punchthrough.bean.sdk.message.Callback;
import com.punchthrough.bean.sdk.message.UploadProgress;
import com.punchthrough.bean.sdk.upload.SketchHex;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.util.Collection;

public class DetailActivity extends AppCompatActivity {

    private static final String Tag = DetailActivity.class.getSimpleName();

    private Bean mBean;

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
            String beanAddress = intent.getStringExtra(Intent.EXTRA_TEXT);
            Log.d(Tag, "bean address: " + beanAddress);
            Collection<Bean> beans = BeanManager.getInstance().getBeans();
            for (Bean bean : beans) {
                if (bean.getDevice().getAddress().equals(beanAddress)) {
                    mBean = bean;
                    break;
                }
            }
        }

        if (mBean == null) {
            Log.d(Tag, "Bean is not found.");
        }

        if (mBean != null) {

            TextView textViewBeanName = (TextView)findViewById(R.id.bean_name);
            Log.d(Tag, "Bean name: " + mBean.getDevice().getName());
            textViewBeanName.setText(mBean.getDevice().getName());

            Button buttonUploadSketch = (Button)findViewById(R.id.action_upload_bean);
            buttonUploadSketch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        InputStream inputStream = getResources().openRawResource(R.raw.sleep_ino_hex);
                        String hex = IOUtils.toString(inputStream, "ASCII");
                        SketchHex sketchHex = SketchHex.create("Sleep", hex);
                        Callback<UploadProgress> progress = new Callback<UploadProgress>() {
                            @Override
                            public void onResult(UploadProgress result) {
                                Log.i(Tag, "Upload sketch progress. (" + result.blocksSent() + "/" + result.totalBlocks());
                            }
                        };
                        Runnable onComplete = new Runnable() {
                            @Override
                            public void run() {
                                Log.i(Tag, "Upload sketch finished.");
                            }
                        };
                        mBean.programWithSketch(sketchHex, progress, onComplete);
                    } catch (Throwable e) {
                        Log.e(Tag, e.getMessage());
                    }
                }
            });
        }
    }

    @Override
    protected void onStart() {
        Log.v(Tag, "onStart");
        super.onStart();
    }
}
