package com.app.test.mentions.mentions;

import android.app.Activity;
import android.os.StrictMode;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;

import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class SearchActivity extends Activity {

    private TextView mentionsDisplay;

    static String TWITTER_CONSUMER_KEY = "ifL1xEZfZSyOj0ADsR8JnJrQr";
    static String TWITTER_CONSUMER_SECRET = "h9rDY3bqsHdmWhUnIdzsWBT3W2ay49mN392DR6upgUu1JqdnKU";
    static String TWITTER_ACCESS_KEY = "2441115534-oVq2xHszFZlpWB95XKD1FWB6JrjOTmTlfHKMh4K";
    static String TWITTER_ACCESS_SECRET = "qDYBIYT5VfnkqHJXUZU8TARfTn7hGjMWKxmGHAPE5Q2J0";

    // Preference Constants
    static String PREFERENCE_NAME = "twitter_oauth";
    static final String PREF_KEY_OAUTH_TOKEN = "oauth_token";
    static final String PREF_KEY_OAUTH_SECRET = "oauth_token_secret";
    static final String PREF_KEY_TWITTER_LOGIN = "isTwitterLogedIn";

    static final String TWITTER_CALLBACK_URL = "oauth://t4jsample";

    // Twitter oauth urls
    static final String URL_TWITTER_AUTH = "auth_url";
    static final String URL_TWITTER_OAUTH_VERIFIER = "oauth_verifier";
    static final String URL_TWITTER_OAUTH_TOKEN = "oauth_token";

    // Login button
    ImageButton btnLoginTwitter;
    // Update status button
    // Logout button
    Button btnLogoutTwitter;
    // EditText for update
    TextView lblUserName;

    ImageButton btnSearch;
    TextView lblIntro;
    EditText inpSearch;
    ScrollView resultsScroll;

    // Progress dialog
    ProgressDialog pDialog;

    String username;

    // Twitter
    private static Twitter twitter;
    private static RequestToken requestToken;

    // Shared Preferences
    private static SharedPreferences mSharedPreferences;

    // Internet Connection detector
    private ConnectionDetector cd;

    // Alert Dialog Manager
    AlertDialogManager alert = new AlertDialogManager();

    static{
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
    }

    private static final int TWITTER_LOGIN_REQUEST_CODE = 1;

    private void setupUIElements() {
        mentionsDisplay = (TextView)findViewById(R.id.tweet_txt);
        btnSearch = (ImageButton) findViewById(R.id.search_btn) ;
        lblIntro = (TextView) findViewById(R.id.intro_txt);
        inpSearch = (EditText) findViewById(R.id.search_edit);
        resultsScroll = (ScrollView) findViewById(R.id.scroll_view);
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
            // stop executing code by return
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


                    // Shared Preferences
                    Editor e = mSharedPreferences.edit();

                    // After getting access token, access token secret
                    // store them in application preferences
                    e.putString(PREF_KEY_OAUTH_TOKEN, accessToken.getToken());
                    e.putString(PREF_KEY_OAUTH_SECRET,
                            accessToken.getTokenSecret());
                    // Store login status - true
                    e.putBoolean(PREF_KEY_TWITTER_LOGIN, true);
                    e.commit(); // save changes

                    Log.e("Twitter OAuth Token", "> " + accessToken.getToken());
                    long userID = accessToken.getUserId();
                    User user = twitter.showUser(userID);
                    username = user.getName();

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
        // Hide login button
        btnLoginTwitter.setVisibility(View.GONE);

        // Show Update Twitter
        mentionsDisplay.setVisibility(View.VISIBLE);
        btnLogoutTwitter.setVisibility(View.VISIBLE);
        btnSearch.setVisibility(View.VISIBLE);
        inpSearch.setVisibility(View.VISIBLE);
        lblIntro.setVisibility(View.VISIBLE);
        resultsScroll.setVisibility(View.VISIBLE);

        // Displaying in xml ui
        lblUserName.setText(Html.fromHtml("<b>Welcome " + username + "</b>"));
    }


    private void loginToTwitter() {
        // Check if already logged in
        if (!isTwitterLoggedInAlready()) {
            ConfigurationBuilder cb = new ConfigurationBuilder();
            cb.setDebugEnabled(true)
                    .setOAuthConsumerKey(TWITTER_CONSUMER_KEY)
                    .setOAuthConsumerSecret(TWITTER_CONSUMER_SECRET)
                    .setOAuthAccessToken(TWITTER_ACCESS_KEY)
                    .setOAuthAccessTokenSecret(TWITTER_ACCESS_SECRET);

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
                QueryResult result = t.search(query);
                StringBuilder searchResults = new StringBuilder();
                for (Status status : result.getTweets()) {
                    searchResults.append("@" + status.getUser().getScreenName() + ":" + status.getText()+"----------------------------------------\n\n");
                }
                if(searchResults.length()>0)
                    mentionsDisplay.setText(searchResults.toString());
                else
                    mentionsDisplay.setText("Sorry - no mentions found for your search!");
            }
            catch(Exception e){
                mentionsDisplay.setText("Whoops - something went wrong!");
                e.printStackTrace();
            }
        }
        else
            mentionsDisplay.setText("Enter a search query!");
    }

    private void logoutFromTwitter() {
        // Clear the shared preferences
        Editor e = mSharedPreferences.edit();
        e.remove(PREF_KEY_OAUTH_TOKEN);
        e.remove(PREF_KEY_OAUTH_SECRET);
        e.remove(PREF_KEY_TWITTER_LOGIN);
        e.commit();

        // After this take the appropriate action
        // I am showing the hiding/showing buttons again
        // You might not needed this code
        btnLogoutTwitter.setVisibility(View.GONE);
        lblUserName.setText("");
        lblUserName.setVisibility(View.GONE);

        btnLoginTwitter.setVisibility(View.VISIBLE);
    }

}
