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


public class MainActivity extends Activity
{
    final String applicationId = "demo";

    protected Integer userId;
    protected JSONObject ratings;
    protected JSONObject stats;
    protected JSONObject encyclopedia;

    protected String apiListUrl = "http://api.worldoftanks.ru/wot/account/list/?application_id=" + applicationId;
    protected String apiRatingsUrl = "http://api.worldoftanks.ru/wot/ratings/accounts/?application_id=" + applicationId;
    protected String apiStatsUrl = "http://api.worldoftanks.ru/wot/tanks/stats/?application_id=" + applicationId;
    protected String apiEncyclopediaUrl = "http://api.worldoftanks.ru/wot/encyclopedia/tanks/?application_id=" + applicationId;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
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
    public static class PlaceholderFragment extends Fragment
    {

        public PlaceholderFragment()
        {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_main, container, false);
        }
    }


    public Integer getUserId()
    {
        return this.userId;
    }

    public JSONObject getRatings() throws Exception
    {
        if (this.ratings == null) {
            // типы:
            // 1, 7, 28, all
            JSONObject ratings = (new HttpRequest()).execute(this.apiRatingsUrl + "&type=7&account_id=" + getUserId()).get();
            handleRequest(ratings);

            this.ratings = ratings.getJSONObject("data").getJSONObject(String.valueOf(getUserId()));
        }

        return this.ratings;
    }


    public JSONObject getEncyclopedia() throws Exception
    {
        if (this.encyclopedia == null) {
            JSONObject encyclopedia = (new HttpRequest()).execute(this.apiEncyclopediaUrl).get();
            handleRequest(encyclopedia, false);

            this.encyclopedia = encyclopedia.getJSONObject("data");
        }

        return this.encyclopedia;
    }

    public JSONObject getStats() throws Exception
    {
        if (this.stats == null) {
            JSONObject stats = (new HttpRequest()).execute(this.apiStatsUrl + "&account_id=" + getUserId()).get();
            handleRequest(stats);

            this.stats = stats.getJSONObject("data").getJSONArray(String.valueOf(getUserId())).getJSONObject(0);
        }

        return this.stats;
    }

    /**
     * @param username Username
     * @throws Exception
     */
    private void setUserIdFromUsername(String username) throws Exception
    {
        JSONObject list = (new HttpRequest()).execute(this.apiListUrl + "&search=" + username).get();
        handleRequest(list);

        this.userId = list.getJSONArray("data").getJSONObject(0).getInt("account_id");
    }

    /**
     * @param username Username
     * @throws Exception
     */
    protected void load(String username) throws Exception
    {
        this.userId = null;
        this.stats = null;
        this.ratings = null;

        setUserIdFromUsername(username);
    }

    /**
     * @param v Button view
     */
    public void buttonSearchClick(View v)
    {
        try {
            String username = ((EditText) findViewById(R.id.editText)).getText().toString();
            load(username);

            TextView resultTextViewWin = (TextView) findViewById(R.id.resultTextViewWin);
            resultTextViewWin.setText(calculateWinRating());

            TextView resultTextViewWN6 = (TextView) findViewById(R.id.resultTextViewWN6);
            resultTextViewWN6.setText(calculateWn6Rating());

            TextView resultTextViewWN7 = (TextView) findViewById(R.id.resultTextViewWN7);
            resultTextViewWN7.setText(calculateWn7Rating());

            TextView resultTextViewWN8 = (TextView) findViewById(R.id.resultTextViewWN8);
            resultTextViewWN8.setText(calculateWn8Rating());

            TextView resultTextViewPf = (TextView) findViewById(R.id.resultTextViewPf);
            resultTextViewPf.setText(calculatePfRating());

            TextView resultTextViewArmorSite = (TextView) findViewById(R.id.resultTextViewArmorSite);
            resultTextViewArmorSite.setText(calculateArmorSiteRating());

            TextView resultTextViewRbr = (TextView) findViewById(R.id.resultTextViewRbr);
            resultTextViewRbr.setText(calculateRbrRating());
        } catch (Exception e) {
            createErrorMessage(e.getMessage());
        }
    }


    protected String calculateWn8Rating()
    {
        /*
        Формула WN8:
        Шаг 1.

        взвешенное соотношение = суммарный показатель на аккаунте / ожидаемый показатель
        rDAMAGE = avgDmg / expDmg
        rSPOT = avgSpot / expSpot
        rFRAG = avgFrag / expFrag
        rDEF = avgDef / expDef
        rWIN = avgWinRate / expWinRate

        Шаг 2.

        Нормализованное значение =  max(нижняя граница, (взвешенное соотношение – константа ) / (1 – константа))
        rWINc = max(0, (rWIN - 0.71) / (1- 0.71))
        rDAMAGEc= max(0, (rDAMAGE-0.22) / (1-0.22))
        Нормализованное значение = min(верхняя граница, max(нижняя граница, (взвешенное соотношение – константа ) / (1 – константа)))
        rFRAGc = min(rDAMAGEc+0.2 , max(0, (rFRAG-0.12) / (1-0.12)))
        rSPOTc = min (rDAMAGEc+0.1 ,  max(0, (rSPOT-0.38) / (1-0.38)))
        rDEFc =min (rDAMAGEc+0.1 , max(0, (rDEF-0.10) / (1-0.10)))

        Шаг3

        WN8 = 980*rDAMAGEc + 210*rDAMAGEc*rFRAGc + 155*rFRAGc*rSPOTc + 75*rDEFc*rFRAGc + 145*MIN(1.8,rWINc)

        Параметры, участвующие в расчете шага 1:
        avgDmg - суммарный нанесенный урон игрока,
        avgSpot - суммарное количество обнаруженных,
        avgFrag - суммарное количество уничтоженных,
        avgDef - суммарное количество очков защиты,
        avgWinRate - суммарное количество побед,
        expDmg - ожидаемый нанесенный урон,
        expSpot - ожидаемое количество обнаруженных,
        expFrag - ожидаемое количество уничтоженных,
        expDef - ожидаемое количество очков защиты,
        expWinRate - ожидаемое количество побед.
         */

        return "123";
    }

    protected String calculateWn7Rating()
    {
        /*
        Формула WN7:

        (1240-1040/(min(TIER,6)^0.164))*FRAGS
        + DAMAGE * 530/(184*exp(0.24*TIER)+130)
        + SPOT * 125*min(TIER,3)/3
        + min(DEF,2.2)*100
        + ((185/(0.17+exp(((WINRATE)-35)*-0.134)))-500)*0.45
        + (-1*(((5 - min(TIER,5))*125)/(1 + exp((TIER-(TOTAL/220^(3/TIER)))*1.5))))


        Параметры, участвующие в расчете:
        DAMAGE – средний урон за бой,
        TIER – средний уровень танков игрока,
        FRAGS – среднее количество фрагов за бой,
        SPOT – среднее количество обнаруженных врагов,
        DEF - среднее количество очков защиты базы за бой,
        WINRATE – процент побед, умноженный на 100,
        TOTAL – общее кол-во боёв.
         */

        return "123";
    }

    protected String calculateWn6Rating()
    {
        /*
        Формула WN6:

        (1240-1040/(MIN(TIER,6))^0.164)*FRAGS
        +DAMAGE*530/(184*exp(0.24*TIER)+130)
        +SPOT*125
        +MIN(DEF,2.2)*100
        +((185/(0.17+exp((WINRATE-35)*-0.134)))-500)*0.45
        +(6-MIN(TIER,6))*-60


        Параметры, участвующие в расчете:
        DAMAGE – средний урон за бой,
        TIER – средний уровень танков игрока,
        FRAGS – среднее количество фрагов за бой,
        SPOT – среднее количество обнаруженных врагов,
        DEF - среднее количество очков защиты базы за бой,
        WINRATE – процент побед, умноженный на 100,
        TOTAL – общее кол-во боёв.
         */

        return "123";
    }


    protected String calculateEffRating()
    {
        /*
        Формула:

        DAMAGE * (10 / (TIER + 2)) * (0.23 + 2*TIER / 100) +
        FRAGS * 250 +
        SPOT * 150 +
        log(CAP + 1,1.732) * 150 +
        DEF * 150;

        Параметры, участвующие в расчете:
        DAMAGE – средний урон за бой,
        TIER – средний уровень танков игрока,
        FRAGS – среднее количество фрагов за бой,
        SPOT – среднее количество обнаруженных врагов,
        CAP – среднее количество очков захвата,
        DEF - среднее количество очков защиты базы за бой.
         */

        return "123";
    }


    protected String calculatePfRating()
    {
        /*
        Формула:

        DAMAGE * (10 / (TIER + 2)) * (0.23 + 2*TIER / 100) +
        FRAGS * 250 +
        SPOT * 150 +
        log(CAP + 1,1.732) * 150 +
        DEF * 150;

        Параметры, участвующие в расчете:
        DAMAGE – средний урон за бой,
        TIER – средний уровень танков игрока,
        FRAGS – среднее количество фрагов за бой,
        SPOT – среднее количество обнаруженных врагов,
        CAP – среднее количество очков захвата,
        DEF - среднее количество очков защиты базы за бой.
         */

        try {
            JSONObject ratings = getRatings();
            JSONObject stats = getStats();
            JSONObject encyclopedia = getEncyclopedia();

            int damage = ratings.getJSONObject("damage_avg").getInt("value");

            //return ratings.getJSONObject("data").getJSONObject(String.valueOf(getUserId())).getJSONObject("wins_ratio").getString("value") + "%";
        } catch (Exception e) {
            return e.getMessage();
        }

        return "123";
    }


    protected String calculateArmorSiteRating()
    {
        /*
        Формула:

        (ln B)/10* Hp* 1.0+
        (ln B)/10*D*WINRATE*2+
        (ln B)/10*D*FRAGS*0.9+
        (ln B)/10*D*SPOT*0.5+
        (ln B)/10*D*CAP*0.5+
        (ln B)/10*D*DEF*0.5

        Параметры, участвующие в расчете:
        D – средний урон за бой,
        В- общее количество боёв,
        Нр- средний опыт за бой,
        WINRATE - процент побед игрока,
        FRAGS – среднее количество фрагов за бой,
        SPOT - среднее количество обнаруженных противников за бой,
        CAP – среднее количество очков захвата за бой,
        DEF - среднее количество очков защиты базы за бой.
         */




        return "123";
    }


    protected String calculateRbrRating()
    {
        try {
            JSONObject ratings = getRatings();
            return ratings.getJSONObject("global_rating").getString("value");
        } catch (Exception e) {
            return "";
        }
    }


    protected String calculateWinRating()
    {
        try {
            JSONObject ratings = getRatings();
            return ratings.getJSONObject("wins_ratio").getString("value") + "%";
        } catch (Exception e) {
            return "";
        }
    }


    /**
     * @param message Error message
     */
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

    /**
     * @param data JSON object
     * @param handleCount Кидает исключение если результатов больше нуля. По умолчанию true.
     * @throws Exception
     */
    protected void handleRequest(JSONObject data, boolean handleCount) throws Exception
    {
        handleRequest(data);

        if (handleCount != false && data.getInt("count") != 1) {
            throw new Exception("Найдено " + data.getString("count") + " результатов.");
        }
    }

    /**
     * @param data JSON object
     * @throws Exception
     */
    protected void handleRequest(JSONObject data) throws Exception
    {
        if (null == data) {
            throw new Exception("Неизвестная ошибка.");
        }

        if (!data.getString("status").equals("ok")) {
            throw new Exception(data.getJSONObject("error").getString("message"));
        }
    }

    /**
     * Async http request
     */
    private class HttpRequest extends AsyncTask<String, Void, JSONObject>
    {
        private ProgressDialog spinner;

        @Override
        protected void onPreExecute()
        {
            spinner = new ProgressDialog(MainActivity.this);
            spinner.setMessage("Load...");
            spinner.setIndeterminate(false);
            spinner.setCancelable(true);
            spinner.show();
        }

        @Override
        protected void onPostExecute(JSONObject json)
        {
            spinner.dismiss();
        }

        @Override
        protected JSONObject doInBackground(String... links)
        {
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
                e.printStackTrace();
            }

            return null;
        }
    }
}
