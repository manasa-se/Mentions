package com.app.test.mentions.mentions;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.ConfigurationBuilder;

public class SearchActivity extends Activity {

    static String TWITTER_CONSUMER_KEY = "ifL1xEZfZSyOj0ADsR8JnJrQr";
    static String TWITTER_CONSUMER_SECRET = "h9rDY3bqsHdmWhUnIdzsWBT3W2ay49mN392DR6upgUu1JqdnKU";
    static String TWITTER_ACCESS_KEY = "2441115534-oVq2xHszFZlpWB95XKD1FWB6JrjOTmTlfHKMh4K";
    static String TWITTER_ACCESS_SECRET = "qDYBIYT5VfnkqHJXUZU8TARfTn7hGjMWKxmGHAPE5Q2J0";

    // Preference Constants
    static String PREFERENCE_NAME = "twitter_oauth";
    static final String PREF_KEY_OAUTH_TOKEN = "oauth_token";
    static final String PREF_KEY_OAUTH_SECRET = "oauth_token_secret";
    static final String PREF_KEY_TWITTER_LOGIN = "isTwitterLogedIn";
    static final String USERNAME = "USERNAME";
    static final String TWITTER_CALLBACK_URL = "oauth://t4jsample";

    // Twitter oauth urls
    static final String URL_TWITTER_AUTH = "auth_url";
    static final String URL_TWITTER_OAUTH_VERIFIER = "oauth_verifier";
    static final String URL_TWITTER_OAUTH_TOKEN = "oauth_token";

    ImageButton btnLoginTwitter;
    Button btnLogoutTwitter;
    TextView lblUserName;
    ImageButton btnSearch;
    TextView lblIntro;
    EditText inpSearch;
    ListView resultsList;

    // Progress dialog
    ProgressDialog pDialog;

     // Twitter
    private static Twitter twitter;
    private static RequestToken requestToken;

    // Shared Preferences
    private static SharedPreferences mSharedPreferences;

    // Internet Connection detector
    private ConnectionDetector cd;

    // Alert Dialog Manager
    AlertDialogManager alert = new AlertDialogManager();

    static final String KEY_TWEET = "tweet";
    static final String KEY_THUMB_URL = "thumb";
    LazyAdapter adapter;

    //TODO change to Async thread
    static{
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
    }

    private static final int TWITTER_LOGIN_REQUEST_CODE = 1;

    private void setupUIElements() {
        btnSearch = (ImageButton) findViewById(R.id.search_btn) ;
        lblIntro = (TextView) findViewById(R.id.intro_txt);
        inpSearch = (EditText) findViewById(R.id.search_edit);
        resultsList = (ListView) findViewById(R.id.listview);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        setupUIElements();
        cd = new ConnectionDetector(getApplicationContext());

        // Check if Internet present
        if (!cd.isConnectingToInternet()) {
            // Internet Connection is not present
            alert.showAlertDialog(SearchActivity.this, "Internet Connection Error",
                    "Please connect to working Internet connection", false);
             return;
        }

        // Check if twitter keys are set
        if(TWITTER_CONSUMER_KEY.trim().length() == 0 || TWITTER_CONSUMER_SECRET.trim().length() == 0){
            // Internet Connection is not present
            alert.showAlertDialog(SearchActivity.this, "Twitter oAuth tokens", "Please set your twitter oauth tokens first!", false);
            // stop executing code by return
            return;
        }

        // All UI elements
        btnLoginTwitter = (ImageButton) findViewById(R.id.btnLoginTwitter);
        btnLogoutTwitter = (Button) findViewById(R.id.btnLogoutTwitter);
        lblUserName = (TextView) findViewById(R.id.lblUserName);

        // Shared Preferences
        mSharedPreferences = getApplicationContext().getSharedPreferences(
                "MyPref", 0);

        btnLoginTwitter.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // Call login twitter function
               loginToTwitter();

            }
        });

        /** This if conditions is tested once is
         * redirected from twitter page. Parse the uri to get oAuth
         * Verifier
         * */
        if (!isTwitterLoggedInAlready()) {
            Uri uri = getIntent().getData();
            if (uri != null && uri.toString().startsWith(TWITTER_CALLBACK_URL)) {
                // oAuth verifier
                String verifier = uri
                        .getQueryParameter(URL_TWITTER_OAUTH_VERIFIER);

                try {
                    // Get the access token
                    AccessToken accessToken = twitter.getOAuthAccessToken(
                            requestToken, verifier);

                    Log.e("Twitter OAuth Token", "> " + accessToken.getToken());
                    long userID = accessToken.getUserId();
                    User user = twitter.showUser(userID);

                    // Shared Preferences
                    Editor e = mSharedPreferences.edit();

                    // After getting access token, access token secret
                    // store them in application preferences
                    e.putString(PREF_KEY_OAUTH_TOKEN, accessToken.getToken());
                    e.putString(PREF_KEY_OAUTH_SECRET,
                            accessToken.getTokenSecret());
                    // Store login status - true
                    e.putBoolean(PREF_KEY_TWITTER_LOGIN, true);
                    e.putString(USERNAME,user.getName());
                    e.commit(); // save changes

                      setupSearchDisplay();
                } catch (Exception e) {
                    // Check log for login errors
                    Log.e("Twitter Login Error", "> " + e.getMessage());
                }
            }
        }
        btnLogoutTwitter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                // Call logout twitter function
                logoutFromTwitter();
            }
        });
    }

    private void setupSearchDisplay() {
        btnLoginTwitter.setVisibility(View.GONE);
        btnLogoutTwitter.setVisibility(View.VISIBLE);
        btnSearch.setVisibility(View.VISIBLE);
        inpSearch.setVisibility(View.VISIBLE);
        lblIntro.setVisibility(View.VISIBLE);
        lblUserName.setText(Html.fromHtml("<b>Welcome " + mSharedPreferences.getString(USERNAME, "User") + "</b>"));
    }


    private void loginToTwitter() {
        // Check if already logged in
        if (!isTwitterLoggedInAlready()) {
            ConfigurationBuilder cb = new ConfigurationBuilder();
            cb.setDebugEnabled(true)
                    .setOAuthConsumerKey(TWITTER_CONSUMER_KEY)
                    .setOAuthConsumerSecret(TWITTER_CONSUMER_SECRET);

            TwitterFactory factory = new TwitterFactory(cb.build());
            twitter = factory.getInstance();

            try {
                requestToken = twitter
                        .getOAuthRequestToken(TWITTER_CALLBACK_URL);
                this.startActivity(new Intent(Intent.ACTION_VIEW, Uri
                        .parse(requestToken.getAuthenticationURL())));
            } catch (TwitterException e) {
                e.printStackTrace();
            }
        } else {
            // user already logged into twitter

            Toast.makeText(getApplicationContext(),
                    "Already Logged into twitter", Toast.LENGTH_LONG).show();
            setupSearchDisplay();
        }
    }

    /**
     * Check user already logged in your application using twitter Login flag is
     * fetched from Shared Preferences
     * */
    private boolean isTwitterLoggedInAlready() {
        // return twitter login status from Shared Preferences
        return mSharedPreferences.getBoolean(PREF_KEY_TWITTER_LOGIN, false);
    }

    public void searchTwitter(View view){
        EditText searchTxt = (EditText)findViewById(R.id.search_edit);
        String searchTerm = searchTxt.getText().toString();
        if(searchTerm.length()>0){
            try{
                ConfigurationBuilder cb = new ConfigurationBuilder();
                cb.setDebugEnabled(true)
                        .setOAuthConsumerKey(TWITTER_CONSUMER_KEY)
                        .setOAuthConsumerSecret(TWITTER_CONSUMER_SECRET)
                        .setOAuthAccessToken(TWITTER_ACCESS_KEY)
                        .setOAuthAccessTokenSecret(TWITTER_ACCESS_SECRET);

                TwitterFactory factory = new TwitterFactory(cb.build());
                Twitter t = factory.getInstance();
                if(!searchTerm.startsWith("@")) {
                    searchTerm = "@" + searchTerm;
                }
                Query query = new Query(searchTerm);
                final QueryResult result = t.search(query);
               // final ArrayList results = new ArrayList();
                final ArrayList<HashMap<String, String>> results = new ArrayList<HashMap<String, String>>();

                for (Status status : result.getTweets()) {
                    HashMap<String, String> map = new HashMap<String, String>();
                    map.put(KEY_TWEET, "@" + status.getUser().getScreenName() + ":" + status.getText());
                    if (status.getMediaEntities().length > 0)
                         map.put(KEY_THUMB_URL, status.getMediaEntities()[0].getMediaURL());
                    else
                        map.put(KEY_THUMB_URL,"");
                    results.add(map);
                   // results.add("@" + status.getUser().getScreenName() + ":" + status.getText()+"\n");
                }
                resultsList.setVisibility(View.VISIBLE);
                if(results.size()>0) {
                    adapter=new LazyAdapter(this, results);
                    resultsList.setAdapter(adapter);


                    // Click event for single list row
                    resultsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                        @Override
                        public void onItemClick(AdapterView<?> parent,final View view,
                                                final int position, long id) {


                            AlertDialog.Builder builder = new AlertDialog.Builder(SearchActivity.this);
                            builder.setMessage("Would you like to save the image to your SD card ?");
                            // Add the buttons
                            builder.setPositiveButton("SD Card", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // User clicked OK button
                                    final String tweet = ((TextView) view.findViewById(R.id.title)).getText().toString();
                                    URL url;
                                    try {
                                        url = new URL(results.get(position).get(KEY_THUMB_URL));
                                        final Bitmap bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());

                                        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                                        bmp.compress(Bitmap.CompressFormat.JPEG, 100, bytes);

                                        File f = new File(Environment.getExternalStorageDirectory() + File.separator + "mention_" + System.currentTimeMillis() + ".jpg");
                                        try {
                                            f.createNewFile();
                                            FileOutputStream fo = new FileOutputStream(f);
                                            fo.write(bytes.toByteArray());
                                            fo.close();
                                            Toast.makeText(getBaseContext(), "Image saved",
                                                    Toast.LENGTH_SHORT).show();
                                        } catch (Exception e) {
                                            Toast.makeText(getBaseContext(), "Image save failed",
                                                    Toast.LENGTH_SHORT).show();

                                            e.printStackTrace();
                                        }
                                    } catch (Exception e) {
                                        Toast.makeText(getBaseContext(), "Image save failed",
                                                Toast.LENGTH_SHORT).show();

                                        e.printStackTrace();
                                    }
                                    ;
                                }
                            });
                            builder.setNeutralButton("Internal", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    final String tweet = ((TextView) view.findViewById(R.id.title)).getText().toString();
                                    URL url;
                                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                                    try {
                                        url = new URL(results.get(position).get(KEY_THUMB_URL));
                                        final Bitmap bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                                        bmp.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    ContextWrapper cw = new ContextWrapper(getApplicationContext());
                                    // path to /data/data/yourapp/app_data/imageDir
                                    File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
                                    // Create imageDir
                                    File mypath = new File(directory, "mention_" + System.currentTimeMillis() + ".jpg");

                                    FileOutputStream fos = null;
                                    try {

                                        fos = new FileOutputStream(mypath);
                                        fos.write(bytes.toByteArray());
                                        fos.close();
                                        Toast.makeText(getBaseContext(), "Image saved",
                                                Toast.LENGTH_SHORT).show();
                                    } catch (Exception e) {
                                        Toast.makeText(getBaseContext(), "Image save failed",
                                                Toast.LENGTH_SHORT).show();
                                        e.printStackTrace();
                                    }
                                 }
                            });
                            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // User cancelled the dialog
                                    dialog.cancel();
                                }
                            });

                            AlertDialog dialog = builder.create();
                            dialog.show();
                        }
                    });
                }
                else {
                    Toast.makeText(getApplicationContext(),
                            "No mentions found for this username", Toast.LENGTH_LONG).show();
                }
            }
            catch(Exception e){
                Toast.makeText(getApplicationContext(),
                        "Whoops - something went wrong!", Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        }
        else
            Toast.makeText(getApplicationContext(),
                    "Enter a search query!", Toast.LENGTH_LONG).show();
    }

    private void logoutFromTwitter() {
        // Clear the shared preferences
        Editor e = mSharedPreferences.edit();
        e.remove(PREF_KEY_OAUTH_TOKEN);
        e.remove(PREF_KEY_OAUTH_SECRET);
        e.remove(PREF_KEY_TWITTER_LOGIN);
        e.remove(USERNAME);
        e.commit();

        // After this take the appropriate action
        // I am showing the hiding/showing buttons again

        btnLogoutTwitter.setVisibility(View.GONE);
        lblUserName.setText("");
        lblUserName.setVisibility(View.GONE);
        btnSearch.setVisibility(View.GONE);
        inpSearch.setVisibility(View.GONE);
        lblIntro.setVisibility(View.GONE);
        resultsList.setVisibility(View.GONE);
        btnLoginTwitter.setVisibility(View.VISIBLE);


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.search, menu);
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

    private class StableArrayAdapter extends ArrayAdapter<String> {

        HashMap<String, Integer> mIdMap = new HashMap<String, Integer>();

        public StableArrayAdapter(Context context, int textViewResourceId,
                                  List<String> objects) {
            super(context, textViewResourceId, objects);
            for (int i = 0; i < objects.size(); ++i) {
                mIdMap.put(objects.get(i), i);
            }
        }

        @Override
        public long getItemId(int position) {
            String item = getItem(position);
            return mIdMap.get(item);
        }
        @Override
        public boolean hasStableIds() {
            return true;
        }



    }



}
