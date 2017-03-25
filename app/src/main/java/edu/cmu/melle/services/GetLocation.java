package edu.cmu.melle.services;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import edu.cmu.melle.models.Location;

import static android.icu.lang.UCharacter.GraphemeClusterBreak.L;

public class GetLocation extends AsyncTask<Void, Void, Location> {

    private Exception exception;

    private Handler mCallersHandler;

    // Return codes
    public static final int MSG_FINISHED = 1001;

    public GetLocation(Handler handler) {
        this.mCallersHandler = handler;
    }

    protected Location doInBackground(Void... params) {
        JSONObject jsonLocation = null;
        Location location;
        try {
            jsonLocation = getJSONObjectFromURL("https://obscure-spire-79030.herokuapp.com/api/heartbeat?robotID=1");
            location = new Location(jsonLocation.getDouble("currentLatitude"), jsonLocation.getDouble("currentLongitude"));
            mCallersHandler.sendMessage(Message.obtain( mCallersHandler, MSG_FINISHED, location));
            return location;
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    protected void onPostExecute(Location feed) {
        // TODO: check this.exception
        // TODO: do something with the feed
    }

    public static JSONObject getJSONObjectFromURL(String urlString) throws IOException, JSONException {

        HttpURLConnection urlConnection = null;

        URL url = new URL(urlString);

        urlConnection = (HttpURLConnection) url.openConnection();

        urlConnection.setRequestMethod("GET");
        urlConnection.setReadTimeout(10000 /* milliseconds */);
        urlConnection.setConnectTimeout(15000 /* milliseconds */);

        urlConnection.setDoOutput(true);

        urlConnection.connect();

        BufferedReader br=new BufferedReader(new InputStreamReader(url.openStream()));

        char[] buffer = new char[1024];

        String jsonString = new String();

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line+"\n");
        }
        br.close();

        jsonString = sb.toString();

        System.out.println("JSON: " + jsonString);

        return new JSONObject(jsonString);
    }
}