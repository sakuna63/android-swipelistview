package com.sakna63.swipelistview.sample;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import com.fortysevendeg.swipelistview.SwipeListView;
import com.fortysevendeg.swipelistview.SwipeListViewListener;

import java.util.List;


public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SwipeListView listview = (SwipeListView) findViewById(R.id.swipe_list);
        listview.setSwipeListViewListener(new SwipeListViewListener() {
            @Override
            public void onOpened(int position, boolean toRight) {
                Log.d(TAG, "onOpened");
            }

            @Override
            public void onClosed(int position, boolean fromRight) {
                Log.d(TAG, "onClosed");
            }

            @Override
            public void onListChanged() {
                Log.d(TAG, "onListChanged");
            }

            @Override
            public void onMove(int position, float x) {
                Log.d(TAG, "onMove");
            }

            @Override
            public void onStartOpen(int position, boolean right) {
                Log.d(TAG, "onStartOpen");
            }

            @Override
            public void onStartClose(int position, boolean right) {
                Log.d(TAG, "onStartClose");

            }

            @Override
            public void onClickFrontView(int position) {
                Log.d(TAG, "onClickFrontView");
            }

            @Override
            public void onClickBackView(int position) {
                Log.d(TAG, "onClickBackView");
            }

            @Override
            public void onDismiss(int[] reverseSortedPositions) {
                Log.d(TAG, "onDismiss");
            }

            @Override
            public int onChangeSwipeMode(int position) {
                Log.d(TAG, "onChangeSwipeMode");
                return 0;
            }

            @Override
            public void onChoiceChanged(int position, boolean selected) { }
            @Override
            public void onChoiceStarted() { }
            @Override
            public void onChoiceEnded() { }
            @Override
            public void onFirstListItem() {  }
            @Override
            public void onLastListItem() {  }
        });
//        ListView listview = (ListView) findViewById(R.id.swipe_list);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.list_item, R.id.text);
//        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        listview.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "onItemLongClick");
                return true;
            }
        });
        listview.setAdapter(adapter);

        PackageManager pm = this.getPackageManager();
        List<ApplicationInfo> list = pm.getInstalledApplications(0);
        for (ApplicationInfo info : list) {
            Log.d(MainActivity.class.getSimpleName(), info.packageName);
            adapter.add(info.packageName);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
