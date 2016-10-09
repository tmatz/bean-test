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
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.punchthrough.bean.sdk.Bean;
import com.punchthrough.bean.sdk.BeanManager;

import java.util.ArrayList;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

public class MainActivity extends AppCompatActivity {
    private static final String Tag = MainActivity.class.getSimpleName();

    private static final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;

    private CompositeSubscription mCompositeSubscription = new CompositeSubscription();
    private ListView mListView;
    private ArrayList<Bean> mBeans;
    private ArrayAdapter<Bean> mAdapter;
    private Subscription mDiscoverySubscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(Tag, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BeanManager.getInstance().setScanTimeout(10);
        setupListView();
    }

    private void setupListView()
    {
        mListView = (ListView)findViewById(R.id.list);
        mBeans = new ArrayList<>();
        mAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mBeans);
        mListView.setAdapter(mAdapter);

        mListView.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(MainActivity.this, DetailActivity.class);
            intent.putExtra("Bean", mBeans.get(position));
            startActivity(intent);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.v(Tag, "onCreateOptionsMenu()");
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.v(Tag, "onOptionsItemSelected()");
        int itemId = item.getItemId();
        if (itemId == R.id.action_refresh) {
            StartDiscovery();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onStart() {
        Log.v(Tag, "onStart()");
        super.onStart();

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

    private void StartDiscovery()
    {
        Log.v(Tag, "StartDiscovery()");

        if (mDiscoverySubscription != null) {
            mDiscoverySubscription.unsubscribe();
            mCompositeSubscription.remove(mDiscoverySubscription);
        }

        mBeans.clear();
        mAdapter.notifyDataSetChanged();

        mDiscoverySubscription = BeanConnect.DiscoverBeans()
                .distinct(bean -> bean.getDevice().getAddress())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(bean -> {
                    Log.v(Tag, "Bean found: " + bean.describe());
                    mBeans.add(bean);
                    mAdapter.notifyDataSetChanged();
                    mDiscoverySubscription.unsubscribe();
                });

        mCompositeSubscription.add(mDiscoverySubscription);
    }
    
    @Override
    protected void onPause() {
        Log.v(Tag, "onPause()");
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.v(Tag, "onStop()");
        mCompositeSubscription.unsubscribe();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.v(Tag, "onDestroy()");
        super.onDestroy();
    }
}
