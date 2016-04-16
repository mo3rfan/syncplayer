package io.github.powerinside.syncplay;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

public class hop_in extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hop_in);

        // Get deep link data and validate
        Intent intent = getIntent();
        String action = intent.getAction();
        Uri data = intent.getData();

        Log.d("SyncPlayer", action);
        Log.d("SyncPlayer", data.toString());

        AlertDialog.Builder pop = new AlertDialog.Builder(hop_in.this)
                .setView(R.layout.username_dialog).setPositiveButton("Next", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // put username in bundle from r.id.edittext
                        // intent for picker.
                    }
                });
        pop.setTitle("Set username").create().show();
    }
}
