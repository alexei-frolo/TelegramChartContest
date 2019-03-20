package com.froloapp.telegramchart;

import android.content.res.AssetManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.TintableCompoundButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.AppCompatCheckBox;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Toast;

import com.froloapp.telegramchart.widget.Utils;
import com.froloapp.telegramchart.widget.linechartview.Line;
import com.froloapp.telegramchart.widget.linechartview.LineChartAdapter;

import java.io.InputStream;


public class MainActivity extends AppCompatActivity {

    private ListView listCharts;

    // hold task to cancel if needed
    private JsonParserTask jsonParserTask;

    private void log(String msg) {
        if (BuildConfig.DEBUG) {
            Log.d("ChartViewMainActivity", msg);
        }
    }

    private void log(Throwable e) {
        if (BuildConfig.DEBUG) {
            Log.e("ChartViewMainActivity", "", e);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        listCharts = findViewById(R.id.listCharts);

        loadCharts();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AsyncTask task = jsonParserTask;
        if (task != null) task.cancel(true);
    }

    private void loadCharts() {
        AsyncTask task = jsonParserTask;
        if (task != null) task.cancel(true);

        JsonParserTask newTask = new JsonParserTask(new JsonParserTask.Callback() {
            @Override public void onStart() {
                // show progress here
            }
            @Override public void onError(Throwable error) {
                log(error);
                Toast.makeText(MainActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
            }
            @Override public void onResult(LineChartAdapter[] adapters) {
                ChartListAdapter chartListAdapter = new ChartListAdapter(adapters);
                listCharts.setAdapter(chartListAdapter);
            }
            @Override
            public void onCancelled() {
            }
        });
        jsonParserTask = newTask;
        AssetManager assets = getAssets();
        try {
            InputStream is = assets.open("chart_data.json");
            newTask.execute(is);
        } catch (Throwable e) {
            log(e);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.actionSwitchDatNightMode: {
                switchDayNightMode();
                return true;
            }
            default: return super.onOptionsItemSelected(item);
        }
    }

    private boolean isNightModeEnabled() {
        int nightModeFlags = getResources().getConfiguration().uiMode &
                        Configuration.UI_MODE_NIGHT_MASK;
        switch (nightModeFlags) {
            case Configuration.UI_MODE_NIGHT_YES: {
                return true;
            }
            default: {
                return false;
            }
        }
    }

    private void switchDayNightMode() {
        log("Switching DayNight mode");
        boolean nightModeEnabled = isNightModeEnabled();
        applyDayNightMode(!nightModeEnabled);
    }

    private void applyDayNightMode(boolean nightModeEnabled) {
        log("Applying DayNight mode: [night=" + nightModeEnabled + "]");
        @AppCompatDelegate.NightMode int mode = nightModeEnabled ?
                AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
        AppCompatDelegate delegate = getDelegate();
        delegate.setLocalNightMode(mode);
        delegate.applyDayNight();
    }
}
