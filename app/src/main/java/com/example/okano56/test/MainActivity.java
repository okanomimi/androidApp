package com.example.okano56.test;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import static com.example.okano56.test.R.id.buttonToDBSite;


//public class MainActivity extends ActionBarActivity{
    public class MainActivity extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onClick(View view){
        switch (view.getId()){
            case R.id.btnToSecond:
                //Intent intent = new Intent(this,ActivitySecond.class);
                Intent intent = new Intent(this,MapsActivity.class);
                int viewWidth = view.getWidth() ;
                intent.putExtra("viewWidth", viewWidth ) ;
                startActivity(intent);

                break;
            case buttonToDBSite:
                Intent intent2 = new Intent(this,DBSampleA.class);
                startActivity(intent2);
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * このメソッドが呼び出されると、buttonのサイズを取得可能
     * なので、ここでbuttonのサイズが必要なメソッドなどの実行
      * @param hasFocus
     */
    @Override
    public void onWindowFocusChanged(boolean hasFocus){

    }
}
