package barqsoft.footballscores;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import barqsoft.footballscores.service.myFetchService;

/**
 * Created by bendix on 10/09/15.
 */
public class WidgetProvider extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        /*Log.d("", "Received broadcast! "+intent.getAction());

        if(intent.getAction().equals("barqsoft.footballscores.DATA")) {
            Log.d("", "Data: " + intent.getExtras().getString("DATA"));
        } else if(intent.getAction().equals("barqsoft.footballscores.REFRESH")) {
            Log.d("", "Starting service!");
            Intent service_start = new Intent(context, myFetchService.class);
            context.startService(service_start);
        } else if(intent.getAction().equals("barqsoft.footballscores.SERVICE_STARTED") ||
                intent.getAction().equals("android.appwidget.action.APPWIDGET_UPDATE")) {
            Intent i = new Intent();
            i.setAction("barqsoft.footballscores.FETCH_DATA");
            context.sendBroadcast(i);
            Log.d("", "Sent FETCH_DATA broadcast!");
        }*/

        if(intent.getAction().equals("barqsoft.footballscores.REFRESH") ||
                intent.getAction().equals("android.appwidget.action.APPWIDGET_UPDATE")) {

            getJSONData(context, "n1");

        } else if(intent.getAction().equals("barqsoft.footballscores.DATA")) {

            Log.d("", "Data: " + intent.getExtras().getString("DATA"));

            try {

                JSONArray root = new JSONObject(intent.getExtras().getString("DATA")).getJSONArray("fixtures");

                if(root.length() > 0) {

                    JSONObject match = root.getJSONObject(0);
                    JSONObject result = match.getJSONObject("result");

                    SimpleDateFormat match_date = new SimpleDateFormat("HH:mm:ss");
                    match_date.setTimeZone(TimeZone.getTimeZone("UTC"));

                    String home_name = match.getString("homeTeamName");
                    String away_name = match.getString("awayTeamName");
                    int home_score = result.getInt("goalsHomeTeam");
                    int away_score = result.getInt("goalsAwayTeam");
                    String score = "-";
                    if(home_score > -1 && away_score > -1)
                        score = home_score+" - "+away_score;
                    String date = match.getString("date");
                    String time = date.substring(date.indexOf("T") + 1, date.indexOf("Z"));
                    Date d = match_date.parse(time);
                    Calendar c = Calendar.getInstance();
                    c.setTimeInMillis(d.getTime());
                    time = c.get(Calendar.HOUR_OF_DAY)+":"+c.get(Calendar.MINUTE);

                    RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget);

                    remoteViews.setTextViewText(R.id.home_name, home_name);
                    remoteViews.setTextViewText(R.id.away_name, away_name);
                    remoteViews.setTextViewText(R.id.score_textview, score);
                    remoteViews.setTextViewText(R.id.data_textview, time);

                    ComponentName widget = new ComponentName(context, WidgetProvider.class);
                    AppWidgetManager manager = AppWidgetManager.getInstance(context);
                    manager.updateAppWidget(widget, remoteViews);

                }


            } catch (Exception e) {
                e.printStackTrace();
            }

            AlarmManager alman = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent alarm = new Intent();
            alarm.setAction("barqsoft.footballscores.REFRESH");
            alman.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 60 * 1000, PendingIntent.getBroadcast(context, 0, alarm, 0));

        }

    }

    private void getJSONData(final Context context, String timeFrame) {
        AsyncTask<String, Integer, String> task = new AsyncTask<String, Integer, String>() {

            @Override
            protected void onPostExecute(String s) {
                Intent i = new Intent();
                i.setAction("barqsoft.footballscores.DATA");
                i.putExtra("DATA", s);
                context.sendBroadcast(i);
            }

            @Override
            protected String doInBackground(String... params) {

                //Creating fetch URL
                final String BASE_URL = "http://api.football-data.org/alpha/fixtures"; //Base URL
                final String QUERY_TIME_FRAME = "timeFrame"; //Time Frame parameter to determine days
                //final String QUERY_MATCH_DAY = "matchday";

                Uri fetch_build = Uri.parse(BASE_URL).buildUpon().
                        appendQueryParameter(QUERY_TIME_FRAME, params[0]).build();
                //Log.v(LOG_TAG, "The url we are looking at is: "+fetch_build.toString()); //log spam
                HttpURLConnection m_connection = null;
                BufferedReader reader = null;
                String JSON_data = null;
                //Opening Connection
                try {
                    URL fetch = new URL(fetch_build.toString());
                    m_connection = (HttpURLConnection) fetch.openConnection();
                    m_connection.setRequestMethod("GET");
                    m_connection.addRequestProperty("X-Auth-Token",context.getResources().getString(R.string.api_key));
                    m_connection.connect();

                    // Read the input stream into a String
                    InputStream inputStream = m_connection.getInputStream();
                    StringBuffer buffer = new StringBuffer();
                    if (inputStream == null) {
                        // Nothing to do.
                        return null;
                    }
                    reader = new BufferedReader(new InputStreamReader(inputStream));

                    String line;
                    while ((line = reader.readLine()) != null) {
                        // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                        // But it does make debugging a *lot* easier if you print out the completed
                        // buffer for debugging.
                        buffer.append(line + "\n");
                    }
                    if (buffer.length() == 0) {
                        // Stream was empty.  No point in parsing.
                        return null;
                    }
                    return buffer.toString();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    return null;
                }
            }
        };

        task.execute(timeFrame);
    }



}
