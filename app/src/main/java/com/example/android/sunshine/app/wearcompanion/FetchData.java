package com.example.android.sunshine.app.wearcompanion;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.example.android.sunshine.app.R;
import com.example.android.sunshine.app.Utility;
import com.example.android.sunshine.app.data.WeatherContract;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.concurrent.ExecutionException;

/**
 * Created by sahil on 10-Oct-16.
 */

public class FetchData implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    public FetchData(Context context){
        this.context = context;
    }

    private Context context;

    public final String LOG_TAG = FetchData.class.getSimpleName();

    private static String tempHigh;

    private GoogleApiClient mGoogleApiClient;

    private static String tempLow;

    private static final String[] WEAR_WEATHER_PROJECTION = new String[] {
        WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
        WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
        WeatherContract.WeatherEntry.COLUMN_MIN_TEMP
    };

    private static final int INDEX_WEATHER_ID = 0;
    private static final int INDEX_MAX_TEMP = 1;
    private static final int INDEX_MIN_TEMP = 2;

    public void fetchDataForWearable(){

        String locationQuery = Utility.getPreferredLocation(context);

        Uri weatherUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(locationQuery, System.currentTimeMillis());

        // we'll query our contentProvider, as always
        Cursor cursor = context.getContentResolver().query(weatherUri, WEAR_WEATHER_PROJECTION, null, null, null);

        if (cursor.moveToFirst()) {
            int weatherId = cursor.getInt(INDEX_WEATHER_ID);
            double high = cursor.getDouble(INDEX_MAX_TEMP);
            double low = cursor.getDouble(INDEX_MIN_TEMP);

            int iconId = Utility.getIconResourceForWeatherCondition(weatherId);
            Resources resources = context.getResources();
            int artResourceId = Utility.getArtResourceForWeatherCondition(weatherId);
            String artUrl = Utility.getArtUrlForWeatherCondition(context, weatherId);

            // On Honeycomb and higher devices, we can retrieve the size of the large icon
            // Prior to that, we use a fixed size
            @SuppressLint("InlinedApi")
            int largeIconWidth = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB
                    ? resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width)
                    : resources.getDimensionPixelSize(R.dimen.notification_large_icon_default);
            @SuppressLint("InlinedApi")
            int largeIconHeight = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB
                    ? resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_height)
                    : resources.getDimensionPixelSize(R.dimen.notification_large_icon_default);

            // Retrieve the large icon
            Bitmap largeIcon;
            try {
                largeIcon = Glide.with(context)
                        .load(artUrl)
                        .asBitmap()
                        .error(artResourceId)
                        .fitCenter()
                        .into(largeIconWidth, largeIconHeight).get();
            } catch (InterruptedException | ExecutionException e) {
                Log.e(LOG_TAG, "Error retrieving large icon from " + artUrl, e);
                largeIcon = BitmapFactory.decodeResource(resources, artResourceId);
            }

            tempHigh = Utility.formatTemperature(context, high);

            tempLow  = Utility.formatTemperature(context, low);

            mGoogleApiClient = new GoogleApiClient.Builder(context)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();

            mGoogleApiClient.connect();

            Log.i("sahil", "high temp b4 send " + tempHigh);

            PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/wear_temp");
            putDataMapRequest.getDataMap().putString("HighTemp", tempHigh);
            putDataMapRequest.getDataMap().putString("LowTemp", tempLow);

            PutDataRequest request = putDataMapRequest.asPutDataRequest().setUrgent();

            Log.i("sahil", "Data items length b4 send " + request.toString());

            Wearable.DataApi.putDataItem(mGoogleApiClient, request)
                .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(@NonNull DataApi.DataItemResult dataItemResult) {
                        if(dataItemResult.getStatus().isSuccess()) {
                            Log.d("sahil ", "successfully sent data items");
                        }
                        else {
                            Log.d("sahil ", "failed to send data item");
                        }
                    }
                });
        }
        cursor.close();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}

