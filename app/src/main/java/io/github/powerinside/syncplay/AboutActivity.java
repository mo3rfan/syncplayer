package io.github.powerinside.syncplay;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import org.sufficientlysecure.htmltextview.HtmlTextView;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        setContentView(R.layout.activity_about);

        HtmlTextView htmlTextView = (HtmlTextView) findViewById(R.id.html_text);
        htmlTextView.setHtml(R.raw.about);

    }

}
