package com.sakna63.swipelistview.sample;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import com.fortysevendeg.swipelistview.SwipeListView;
import com.fortysevendeg.swipelistview.SwipeListViewListener;

import java.util.List;


@SuppressWarnings("unchecked")
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
//                Log.d(TAG, "onMove");
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
            public void onDismiss(int[] reverseSortedPositions) {
                Log.d(TAG, "onDismiss");
            }

            @Override
            public int onChangeSwipeMode(int position) {
//                Log.d(TAG, "onChangeSwipeMode");
                return SwipeListView.SWIPE_MODE_BOTH;
            }

            @Override
            public void onFirstListItem() {  }
            @Override
            public void onLastListItem() {  }

            @Override
            public void onScrollBottom() {
                Log.d(TAG, "onScrollBottom");
            }
        });
        listview.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "onItemClick");
            }
        });
//        ListView listview = (ListView) findViewById(R.id.swipe_list);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.list_item, R.id.text) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
//                view.findViewById(R.id.icon).setOnClickListener(new OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        Log.d(TAG, "click icon");
//                    }
//                });
//                view.findViewById(R.id.back_view).setOnClickListener(new OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        Log.d(TAG, "click back");
//                    }
//                });
                return view;
            }
        };
//        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
//        listview.setOnItemLongClickListener(new OnItemLongClickListener() {
//            @Override
//            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
//                Log.d(TAG, "onItemLongClick");
//                return true;
//            }
//        });
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
