package edu.cmu.melle.services;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import edu.cmu.melle.models.Location;

import static com.google.android.gms.internal.zznu.ib;
import static edu.cmu.melle.services.GetLocation.getJSONObjectFromURL;

/**
 * Created by root on 10/20/16.
 */

public class SendRunning extends AsyncTask<Void, Void, Integer> {

    private Exception exception;

    private Handler mCallersHandler;

    private boolean mIsRunning;

    // Return codes
    public static final int MSG_FINISHED = 1001;

    public SendRunning(Handler handler, boolean isRunning) {
        mCallersHandler = handler;
        mIsRunning = isRunning;
    }

    protected Integer doInBackground(Void... params) {

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("robotID", 1);
            jsonObject.put("isRunning", mIsRunning);

            int httpStatus = postJSONObjectToURL("https://obscure-spire-79030.herokuapp.com/api/running", jsonObject);
            mCallersHandler.sendMessage(Message.obtain( mCallersHandler, MSG_FINISHED, httpStatus));
            return httpStatus;
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    protected void onPostExecute(Location feed) {
        // TODO: check this.exception
        // TODO: do something with the feed
    }

    public static int postJSONObjectToURL(String urlString, JSONObject jsonObject) throws IOException, JSONException {

        HttpURLConnection urlConnection = null;

        URL url = new URL(urlString);

        urlConnection = (HttpURLConnection) url.openConnection();

        urlConnection.setDoOutput(true);
        urlConnection.setDoInput(true);
        urlConnection.setInstanceFollowRedirects(false);
        urlConnection.setRequestMethod("POST");
        urlConnection.setRequestProperty("Content-Type", "application/json");
        urlConnection.setRequestProperty("charset", "utf-8");
        urlConnection.setUseCaches (false);

        DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream());

        wr.writeBytes(jsonObject.toString());

        wr.flush();
        wr.close();

        urlConnection.connect();

        return urlConnection.getResponseCode();
    }

}
