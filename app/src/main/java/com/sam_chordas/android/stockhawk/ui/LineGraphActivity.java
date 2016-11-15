package com.sam_chordas.android.stockhawk.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import static android.media.tv.TvContract.Programs.Genres.encode;

/**
 * Created by mposadar on 13/11/16.
 */

public class LineGraphActivity extends AppCompatActivity {

    // UI
    private LineChart mChart;
    // LOGIC
    private String symbol;
    private List<Entry> entries;
    private static final String URL_BASE = "https://query.yahooapis.com/v1/public/yql?q=";
    private static final String URL_EXTRAS = "&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys&callback=";
    private List<String> quarters;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_line_graph);

        mChart = (LineChart) findViewById(R.id.chart);
        Description description = new Description();
        description.setText(getString(R.string.app_name));
        mChart.setDescription(description);
        entries = new ArrayList<>();
        quarters = new ArrayList<>();

        try {
            symbol = getIntent().getExtras().getString(QuoteColumns.SYMBOL);

            final String complete_url = URLEncoder.encode(
                    "select * from yahoo.finance.historicaldata where symbol = \""+symbol+"\" "+
                    "and startDate = \"" + Utils.getMinusSevenDays() + "\" and endDate = \""+ Utils.getCurrentDay() +"\""
            );

            final OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(URL_BASE+complete_url+URL_EXTRAS)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String json = response.body().string();
                        try {
                            JSONObject jsonObject = new JSONObject(json);
                            JSONObject queryObject = jsonObject.getJSONObject("query");
                            JSONObject resultObject = queryObject.getJSONObject("results");
                            JSONArray quotesArray = resultObject.getJSONArray("quote");
                            int size = quotesArray.length();
                            if (size > 0) {
                                for (int i = 0; i < size; i++) {
                                    JSONObject element = quotesArray.getJSONObject(i);
                                    String date = element.getString("Date");
                                    String dayName = Utils.getDayName(date);
                                    String closeStr = element.getString("Close");
                                    float close = Float.parseFloat(closeStr);

                                    quarters.add(dayName);
                                    entries.add(new Entry((float) i, close));
                                }

                                LineGraphActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        populateChar();
                                    }
                                });
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
        catch (NullPointerException e) {
            symbol = "";
            Toast.makeText(this, "Symbol is empty!", Toast.LENGTH_SHORT).show();
        }
    }

    private void populateChar() {

        LineDataSet dataSet = new LineDataSet(entries, symbol);
        dataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        LineData lineData = new LineData(dataSet);
        mChart.setData(lineData);
        mChart.invalidate();

        IAxisValueFormatter formatter = new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                return quarters.get((int) value);
            }

            // we don't draw numbers, so no decimal digits needed
            @Override
            public int getDecimalDigits() {
                return 0;
            }
        };

        XAxis xAxis = mChart.getXAxis();
        xAxis.setGranularity(1f); // minimum axis-step (interval) is 1
        xAxis.setValueFormatter(formatter);
    }
}
