package io.github.powerinside.syncplay;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.android.gms.analytics.GoogleAnalytics;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by irfan on 4/8/16.
 */
public class syncplaysocket extends syncplay {
    Boolean isfileSet = false;
    Activity activity;
    private Socket socket;
    private PrintWriter output;
    private BufferedReader bufferedreader;
    private Handler handler;
    private ProgressDialog loader;

    public syncplaysocket(String address, String username, String password,
                          String room, Handler handler, ProgressDialog p,
                          Activity a, Context c, Uri uri, CustomMediaPlayer vv,
                          Handler OSDHandler) throws JSONException, IOException {
        super(address, username, password, room, a, c, uri, vv, OSDHandler);
        this.handler = handler;
        this.loader = p;
        this.activity = a;
    }

    public void run() {
        try {
            socket = new Socket();
            try {
                socket.connect(new InetSocketAddress(this.address, this.port), 1000);
            } catch (IllegalArgumentException e) {
                Message msg = new Message();
                msg.obj = "Bad address or port!";
                handler.sendMessage(msg);
                this.close();
            }
            if (socket.isConnected()) {
                Log.d("Syncplay", "Connected!");
                loader.dismiss();
                GoogleAnalytics.getInstance(activity).dispatchLocalHits(); // or put this in another place.
            } else {

            }

            output = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream())));
            String helloframe = prepare_frame("Hello");
            send_frame(output, helloframe);

            bufferedreader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), "utf8"), 150);
            String response = "";

            response = bufferedreader.readLine();
            while (response != null && isConnected()) {
                //TODO: Use some optimizations
                Log.d("Syncplay", "Server << " + response);
                if (response == null)
                    break;
                try {
                    String nextup = parse(response);
                    if (nextup.equals(activity.getString(R.string.nextup_state))) {
                        send_frame(output, prepare_frame(activity.getString(R.string.nextup_state)));
                    }
                    if (nextup.equals(activity.getString(R.string.nextup_state)) && !isfileSet) {
                        send_frame(output, prepare_frame(activity.getString(R.string.nextup_set)));
                        isfileSet = true;
                    }
                    if (nextup.equals("KILL")) {
                        // TODO: Instantiate msg obj once.
                        Message msg = new Message();
                        msg.obj = "Disconnected from server!";
                        handler.sendMessage(msg);
                        this.close();
                    }
                    if (nextup.equals("ReadyState")) {
                        send_frame(output, prepare_frame(activity.getString(R.string.nextup_readystate),
                                isReady, is_manually_initiated));
                    }
                } catch (syncplay.syncplayerror syncplayerror) {
                    Message msg = new Message();
                    msg.obj = "Error! " + syncplayerror.getMessage();
                    handler.sendMessage(msg);
                    this.close();
                }
                response = bufferedreader.readLine();
            }

        } catch (IOException e) {
            e.printStackTrace();
            Message msg = new Message();
            msg.obj = "Failed to connect!";
            handler.sendMessage(msg);
            this.close();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            socket.shutdownInput();
            socket.shutdownOutput();
            socket.close();
            connected = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
        loader.dismiss();
        activity.finish();
    }
}
