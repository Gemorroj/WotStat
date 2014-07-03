package com.wapinet.wotstat;

import android.app.Activity;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class MainActivity extends Activity {
    final String applicationId = "demo";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
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

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }
    }





    public void buttonSearchClick(View v){
        try {
            String username = ((EditText) findViewById(R.id.editText)).getText().toString();

            JSONObject json = (new HttpRequest()).execute("http://api.worldoftanks.ru/wot/account/list/?application_id=" + applicationId + "&search=" + username).get();


            TextView resultTextViewWN6 = (TextView) findViewById(R.id.resultTextViewWN6);
            resultTextViewWN6.setText(json.toString());

            TextView resultTextViewWN8 = (TextView) findViewById(R.id.resultTextViewWN8);
            resultTextViewWN8.setText("Hello Kitty!");


        } catch (Exception e) {
            createErrorMessage(e.toString());
        }
    }


    protected void createErrorMessage(String message)
    {
        (new AlertDialog.Builder(this)
                .setCancelable(true)
                .setTitle("Error")
                .setCancelable(true)
                .setMessage(message)
                .create()
        ).show();
    }


    private class HttpRequest extends AsyncTask<String, Void, JSONObject> {
        private ProgressDialog spinner;

        @Override
        protected void onPreExecute() {
            spinner = new ProgressDialog(MainActivity.this);
            spinner.setMessage("Load...");
            spinner.show();
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            spinner.dismiss();
        }

        @Override
        protected JSONObject doInBackground(String... links) {
            try {
                URL url = new URL(links[0]);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                try {
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());

                    BufferedReader br = new BufferedReader(new InputStreamReader(in));

                    String line;
                    String text = "";
                    while ((line = br.readLine()) != null) {
                        text += line;
                    }

                    br.close();

                    return new JSONObject(text);
                } finally {
                    urlConnection.disconnect();
                }
            } catch (Exception e) {
                createErrorMessage(e.toString());
            }

            return new JSONObject();
        }
    }
}
