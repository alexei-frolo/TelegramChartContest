package com.froloapp.telegramchart;


import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.froloapp.telegramchart.widget.linechartview.LineChartAdapter;

import java.io.InputStream;

abstract class AbsChartActivity extends AppCompatActivity {
    // hold task to cancel if needed
    private JsonParserTask jsonParserTask;

    private void log(String msg) {
        if (BuildConfig.DEBUG) {
            Log.d("AbsChartActivity", msg);
        }
    }

    private void log(Throwable e) {
        if (BuildConfig.DEBUG) {
            Log.e("AbsChartActivity", "", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AsyncTask task = jsonParserTask;
        if (task != null) task.cancel(true);
    }

    void load() {
        Object lastInstance = getLastCustomNonConfigurationInstance();
        if (lastInstance instanceof JsonParserTask) {
            JsonParserTask retainedTask = (JsonParserTask) lastInstance;
            LineChartAdapter[] result = retainedTask.getResult();
            if (result != null) {
                jsonParserTask = retainedTask;
                populateCharts(result);
            } else {
                loadCharts();
            }
        }  else {
            loadCharts();
        }
    }

    abstract void populateCharts(LineChartAdapter[] adapters);

    private void loadCharts() {
        AsyncTask task = jsonParserTask;
        if (task != null) task.cancel(true);

        JsonParserTask newTask = new JsonParserTask(new JsonParserTask.Callback() {
            @Override public void onStart() {
                // show progress here
            }
            @Override public void onError(Throwable error) {
                log(error);
                Toast.makeText(AbsChartActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
            }
            @Override public void onResult(LineChartAdapter[] adapters) {
                populateCharts(adapters);
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

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return jsonParserTask;
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
    }
}
