package io.github.powerinside.syncplay;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import de.cketti.library.changelog.ChangeLog;
import io.github.powerinside.syncplay.database.ServerListContract;
import io.github.powerinside.syncplay.database.ServerListHelper;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "SyncPlay";
    private static final boolean DEVELOPER_MODE = false;
    private static final int REQUEST_INVITE = 1;
    private static final int READ_EXTERNAL_STORAGE_FOR_MEDIA = 2;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */

    ChangeLog cl;
    private int REQUEST_TAKE_GALLERY_VIDEO = 1;
    private Intent tovideoplayer;
    private Intent pickerprovider;

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (DEVELOPER_MODE) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()   // or .detectAll() for all detectable problems
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        cl = new ChangeLog(this);
        if (cl.isFirstRun()) {
            cl.getFullLogDialog().show();
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final ServerListHelper serverListHelper = new ServerListHelper(this);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        ListView listView = (ListView) findViewById(R.id.listView);

        final SQLiteDatabase db_read = serverListHelper.getReadableDatabase();

        final String[] projection = {
                ServerListContract.ServerEntry._ID,
                ServerListContract.ServerEntry.COLUMN_NAME_NAME
        };
        final Cursor cursor;

        cursor = db_read.query(ServerListContract.ServerEntry.TABLE_NAME,
                projection,
                null, null, null, null, null
        );
        String[] from = {
                ServerListContract.ServerEntry.COLUMN_NAME_NAME
        };
        int[] to = {
                R.id.textView
        };
        final SimpleCursorAdapter mAdapter = new SimpleCursorAdapter(this,
                R.layout.simple_list, cursor,
                from, to, 0);
        assert listView != null;

        listView.setAdapter(mAdapter);
        listView.setEmptyView(findViewById(R.id.emptytextview));

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                TextView tv = (TextView) view.findViewById(R.id.textView);
                final String[] label = {tv.getText().toString()};
                final SQLiteDatabase db_w = serverListHelper.getWritableDatabase();
                final Cursor cursor_mod = db_w.query(ServerListContract.ServerEntry.TABLE_NAME,
                        null, ServerListContract.ServerEntry.COLUMN_NAME_NAME + " = ?", label, null, null, null);
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                final String[] oldname = {null};
                final AlertDialog alertDialog = builder.setTitle(R.string.modify_entry_title).setPositiveButton(R.string.save_button, null).setNegativeButton(R.string.delete_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // delete
                        SQLiteDatabase db = serverListHelper.getWritableDatabase();
                        String[] whereargs = {oldname[0]};
                        try {
                            db.delete(ServerListContract.ServerEntry.TABLE_NAME,
                                    ServerListContract.ServerEntry.COLUMN_NAME_NAME + " = ?"
                                    , whereargs);
                            Cursor cursor;
                            cursor = db_read.query(ServerListContract.ServerEntry.TABLE_NAME,
                                    projection,
                                    null, null, null, null, null
                            );

                            mAdapter.changeCursor(cursor);
                            mAdapter.notifyDataSetChanged();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                }).setView(R.layout.fabdialog_add).create();

                alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(final DialogInterface dialog) {
                        final EditText namefield = (EditText) alertDialog.findViewById(R.id.name);
                        final EditText serverfield = (EditText) alertDialog.findViewById(R.id.server);
                        final EditText roomfield = (EditText) alertDialog.findViewById(R.id.defaultroom);
                        final EditText passwdfield = (EditText) alertDialog.findViewById(R.id.password);
                        final EditText userfield = (EditText) alertDialog.findViewById(R.id.username);

                        if (cursor_mod.moveToNext()) {
                            oldname[0] = cursor_mod.getString(1);
                            namefield.setText(cursor_mod.getString(1));
                            serverfield.setText(cursor_mod.getString(2));
                            passwdfield.setText(cursor_mod.getString(3));
                            roomfield.setText(cursor_mod.getString(4));
                            userfield.setText(cursor_mod.getString(5));
                        }

                        Button b = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                        final String finalOldname = oldname[0];
                        b.setOnClickListener(new View.OnClickListener() {
                            //target api kitkat?
                            @Override
                            public void onClick(View v) {
                                if (namefield.getText().length() > 0 && serverfield.getText().length() > 0
                                        && roomfield.getText().length() > 0 && userfield.getText().length() > 0) {
                                    ContentValues cv = new ContentValues();
                                    cv.put(ServerListContract.ServerEntry.COLUMN_NAME_NAME, namefield.getText().toString());
                                    cv.put(ServerListContract.ServerEntry.COLUMN_NAME_ADDR, serverfield.getText().toString());
                                    cv.put(ServerListContract.ServerEntry.COLUMN_NAME_DEFROOM, roomfield.getText().toString());
                                    cv.put(ServerListContract.ServerEntry.COLUMN_NAME_PASSWD, passwdfield.getText().toString());
                                    cv.put(ServerListContract.ServerEntry.COLUMN_NAME_USERNAME, userfield.getText().toString());
                                    try {
                                        db_w.update(ServerListContract.ServerEntry.TABLE_NAME, cv,
                                                ServerListContract.ServerEntry.COLUMN_NAME_NAME + " = ?",
                                                new String[]{finalOldname});
                                        Cursor cursorold;
                                        cursorold = db_read.query(ServerListContract.ServerEntry.TABLE_NAME,
                                                projection,
                                                null, null, null, null, null
                                        );
                                        mAdapter.changeCursor(cursorold);
                                        mAdapter.notifyDataSetChanged();
                                        alertDialog.dismiss();

                                    } catch (SQLiteConstraintException e) {
                                        // Not unique!
                                        Toast.makeText(getApplicationContext(), "Error! Label " + namefield.getText()
                                                + " already exists! Use a different name there.", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Toast.makeText(getApplicationContext(), R.string.fields_error,
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                });
                alertDialog.show();
                return true;
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView tv = (TextView) view.findViewById(R.id.textView);
                String label = tv.getText().toString();
                Cursor itemdetails = db_read.query(ServerListContract.ServerEntry.TABLE_NAME,
                        null, ServerListContract.ServerEntry.COLUMN_NAME_NAME + " = ?",
                        new String[]{label}, null, null, null);
                if (itemdetails.moveToNext()) {
                    String server = itemdetails.getString(2);
                    String passwd = itemdetails.getString(3);
                    String room = itemdetails.getString(4);
                    String username = itemdetails.getString(5);

                    pickerprovider = new Intent()
                            .setType("video/*, audio/*")
                            .setAction(Intent.ACTION_GET_CONTENT);
                    tovideoplayer = new Intent()
                            .putExtra("server", server)
                            .putExtra("passwd", passwd)
                            .putExtra("room", room)
                            .putExtra("username", username);
                    startActivity(new Intent(getApplicationContext(), videoPlayer.class)
                            .putExtras(tovideoplayer));
                }
            }
        });

        assert fab != null;

        // TODO: Suggest and autofill username
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this).setView(R.layout.fabdialog_add);

                final AlertDialog mdialog = builder.setTitle(R.string.add_server_dialog)
                        .setPositiveButton(R.string.save_button, null).setNegativeButton(R.string.cancel_button, null).create();

                mdialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(final DialogInterface dialog) {
                        Button b = mdialog.getButton(AlertDialog.BUTTON_POSITIVE);
                        b.setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                ContentValues values = new ContentValues();
                                SQLiteDatabase db = serverListHelper.getWritableDatabase();
                                EditText namefield = (EditText) mdialog.findViewById(R.id.name);
                                EditText serverfield = (EditText) mdialog.findViewById(R.id.server);
                                EditText roomfield = (EditText) mdialog.findViewById(R.id.defaultroom);
                                EditText passwdfield = (EditText) mdialog.findViewById(R.id.password);
                                EditText userfield = (EditText) mdialog.findViewById(R.id.username);

                                values.put(ServerListContract.ServerEntry.COLUMN_NAME_ADDR, serverfield.getText().toString());
                                values.put(ServerListContract.ServerEntry.COLUMN_NAME_NAME, namefield.getText().toString());
                                values.put(ServerListContract.ServerEntry.COLUMN_NAME_DEFROOM, roomfield.getText().toString());
                                values.put(ServerListContract.ServerEntry.COLUMN_NAME_PASSWD, passwdfield.getText().toString());
                                values.put(ServerListContract.ServerEntry.COLUMN_NAME_USERNAME, userfield.getText().toString());

                                if (namefield.getText().length() > 0 && serverfield.getText().length() > 0
                                        && roomfield.getText().length() > 0 && userfield.getText().length() > 0) {
                                    try {
                                        // TODO: Validate mandatory fields properly
                                        db.insertOrThrow(ServerListContract.ServerEntry.TABLE_NAME, null, values);

                                        Cursor cursor;
                                        cursor = db_read.query(ServerListContract.ServerEntry.TABLE_NAME,
                                                projection,
                                                null, null, null, null, null
                                        );

                                        mAdapter.changeCursor(cursor);
                                        mAdapter.notifyDataSetChanged();
                                        mdialog.dismiss();
                                    } catch (SQLException e) {
                                        e.printStackTrace();
                                        Toast.makeText(getApplicationContext(), R.string.insertOrThrow, Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Toast.makeText(getApplicationContext(), getString(R.string.fields_error),
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                });
                if (!mdialog.isShowing())
                    mdialog.show();

                Snackbar.make(view, "", Snackbar.LENGTH_LONG)
                        .setAction(R.string.add_server_dialog, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                            }
                        });
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode) {
            case READ_EXTERNAL_STORAGE_FOR_MEDIA: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startActivityForResult(Intent.createChooser(pickerprovider, "Pick a video"), REQUEST_TAKE_GALLERY_VIDEO);
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement

        if (id == R.id.changelog) {
            if (!cl.getFullLogDialog().isShowing())
                cl.getFullLogDialog().show();
        }

        if (id == R.id.about) {
            startActivity(new Intent(this, AboutActivity.class));
        }

        if (id == R.id.privacypolicy) {
            startActivity(new Intent(this, privacyPolicy.class));
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == 1) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                Uri selectedFile = data.getData();
                if (selectedFile != null) {
                    startActivity(new Intent(this, videoPlayer.class)
                            .setData(selectedFile)
                            .putExtras(tovideoplayer));
                } else if (resultCode != REQUEST_INVITE) {
                    Toast.makeText(this, "Error loading the file.", Toast.LENGTH_SHORT)
                            .show();
                }
            }
        }
    }
}
