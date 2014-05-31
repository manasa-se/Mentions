package com.app.test.mentions.mentions;

import android.app.Activity;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;


public class SearchActivity extends Activity {

    private TextView mentionsDisplay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        mentionsDisplay = (TextView)findViewById(R.id.tweet_txt);
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

            }
            catch(Exception e){
                mentionsDisplay.setText("Whoops - something went wrong!");
                e.printStackTrace();
            }
        }
        else
            mentionsDisplay.setText("Enter a search query!");
    }

}
