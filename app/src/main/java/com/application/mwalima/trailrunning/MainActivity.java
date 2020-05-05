package com.application.mwalima.trailrunning;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;;

public class MainActivity extends Activity implements View.OnClickListener {
    private ViewGroup mListView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        super.onCreate(savedInstanceState);

        mListView = (ViewGroup) findViewById(R.id.list);
        addPage("RunningMap", MapsActivity.class);
    }


    private void addPage(String Name, Class<? extends Activity> activityClass) {
        Button b = new Button(this);
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        b.setLayoutParams(layoutParams);
        b.setText(Name);
        b.setTag(activityClass);
        b.setOnClickListener(this);
        mListView.addView(b);
    }

    public void onClick(View view) {
        Class activityClass = (Class) view.getTag();
        startActivity(new Intent(this, activityClass));
    }
}
