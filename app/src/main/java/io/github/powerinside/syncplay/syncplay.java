package io.github.powerinside.syncplay;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.provider.OpenableColumns;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by irfan on 4/6/16.
 */
public class syncplay {
    public static final String VERSION = "1.3.4";
    public InetAddress address;
    public int port;
    public mediafile mfile; // TODO: Make private
    protected String username;
    protected String password;
    protected JSONObject reply;
    protected boolean connected = true;

    Boolean doSeek = false;
    CustomVideoView videoview;
    Integer client = 1;
    JSONObject ignoreOnTheFly;
    Handler OSDHandler;
    private String room;
    //ping
    private Long latencyCalculation = 0L;
    //playstate
    private Boolean paused = true;
    private double position = 0.0;
    private Activity activity; // umm
    private Integer ourLatency = 0; // ...
    private boolean ignoreOnFly = false;

    public syncplay(String address, String username, String password, String room,
                    Activity a, Context c, Uri uri, CustomVideoView vv, Handler OSDHandler) throws UnknownHostException {
        if (!password.isEmpty()) {
            this.password = utils.md5(password);
        } else {
            this.password = "";
        }
        this.activity = a;
        this.videoview = vv;
        this.OSDHandler = OSDHandler;
        this.username = username;
        this.address = InetAddress.getByName(address.split(":")[0]); // might block?
        this.port = Integer.parseInt(address.split(":")[1]);
        this.room = room;
        mfile = new mediafile(c, uri, a);

        videoview.setVideoViewListener(new CustomVideoView.VideoViewListener() {
            @Override
            public void onPlay() {
                doSeek = false; //ignoreOnFly = true;
                setPaused(false);
                setPosition((double) videoview.getCurrentPosition());
            }

            @Override
            public void onPause() {
                doSeek = false; //ignoreOnFly = true;
                setPaused(true);
                setPosition((double) videoview.getCurrentPosition());
            }

            @Override
            public void onTimeBarSeekChanged(int currentTime) {
                //videoview.pause();
                doSeek = true;
                ignoreOnFly = true;
                client = 1;
                setPosition((double) currentTime);
            }
        });
    }

    public static void send_frame(PrintWriter out, String frame) {
        Log.d("Syncplay", "Client >> " + frame);
        out.println(frame + "\r\n");
        out.flush();
    }

    private void setPaused(Boolean b) {
        this.paused = b;
    }

    private void setPosition(Double d) {
        this.position = d / 1000; // To seconds
    }

    public Boolean isConnected() {
        return this.connected;
    }

    public String parse(String response) throws syncplayerror {
        try {
            this.reply = new JSONObject(response);
            try {
                JSONObject error = this.reply.getJSONObject("Error");
                throw new syncplayerror(error.getString("message"));
            } catch (JSONException e) {
                //e.printStackTrace();
            }
            try {
                this.reply.getString("Hello");
                // TODO: Do response hello verification.
                this.connected = true;

            } catch (JSONException e) {
                //e.printStackTrace();
            }
            try {
                JSONObject state = this.reply.getJSONObject("State");
                JSONObject ping = state.getJSONObject("ping");
                latencyCalculation = ping.getLong("latencyCalculation");
                JSONObject playstate = state.getJSONObject("playstate");

                try {
                    // Set videoview paused and position only if made by others and per settings.
                    String setBy = playstate.getString("setBy");
                    try {
                        // TODO: ignoringOnTheFly strict validation?
                        JSONObject ignore = state.getJSONObject("ignoringOnTheFly");
                        ignoreOnFly = true;
                        ignoreOnTheFly = ignore;
                        //if (setBy.equals(username)) {
                        if (ignoreOnFly == true && ignoreOnTheFly.length() == 2
                                && ignoreOnTheFly.getInt("server") == client) {
                            // server sends response matching client's integer.
                            // we now have to send a final frame.
                        } else if (ignoreOnFly == true && ignoreOnTheFly.length() == 1 &&
                                ignoreOnTheFly.keys().next().equals("server")) {
                            // Recieve server int, process locally and prepare to send ACK
                            client = ignore.getInt("server");
                            if (!setBy.equals(username)) {
                                boolean seek = false;
                                try {
                                    seek = playstate.getBoolean("doSeek");
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                paused = playstate.getBoolean("paused");
                                setPaused(paused);
                                if (seek) { //
                                    position = playstate.getDouble("position"); // ...
                                    showOSD("User '" + setBy + "' seeked to " + (int) position + " seconds."); // TODO: paused state and add in strings.
                                    videoview.seekTo((int) position * 1000);
                                }
                                if (paused) {
                                    videoview.pause();
                                    if (!seek)
                                        showOSD("User '" + setBy + "' paused.");
                                } else {
                                    videoview.start();
                                    showOSD("User '" + setBy + "' resumed.");
                                }
                            }
                        } else if (ignoreOnFly == true && ignoreOnTheFly.length() == 1 &&
                                ignoreOnTheFly.keys().next().equals("client")) {
                            // an ack that can be ignored.
                            ignoreOnTheFly = null;
                            ignoreOnFly = false;
                        }
                        //}

                    } catch (JSONException e) {
                        //e.printStackTrace();
                    }
                    // ws
                } catch (JSONException e) {
                    //e.printStackTrace();
                }

                // Send
                return "State"; // socket should prepare to send state frame.
            } catch (JSONException e) {
                //e.printStackTrace();
            }
            try {
                JSONObject set = this.reply.getJSONObject("Set");
                try {
                    JSONObject ready = set.getJSONObject("ready");
                    String username = ready.getString("username");
                    Boolean isReady = ready.getBoolean("isReady");
                    // manually initiated;
                    String s_ready = "ready";
                    if (!isReady) {
                        s_ready = "not ready";
                    }
                    showOSD("User '" + username + "' is " + s_ready);
                    return ""; // send nothing.
                } catch (JSONException e) {

                }
                JSONObject user = set.getJSONObject("user");
                String lusername = user.keys().next();

                JSONObject room = user.getJSONObject(lusername);
                JSONObject roomjson = room.getJSONObject("room");
                String osdmessage = "User '" + lusername + "'";
                String roomname = roomjson.getString("name");
                try {
                    JSONObject event = room.getJSONObject("event");
                    try {
                        Boolean joined = event.getBoolean("joined");
                        osdmessage += " joined"; // if key exists, it's always joined.
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    try {
                        Boolean left = event.getBoolean("left");
                        if (left && lusername.equals(username)) {
                            return "KILL";
                        }
                        osdmessage += " left";
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } catch (JSONException e) {
                    //e.printStackTrace();
                }
                if (!osdmessage.equals("User '" + username + "'")) {
                    showOSD(osdmessage);
                }
                return "Set";
            } catch (JSONException e) {

            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return ""; // unparsable response.
    }

    public String prepare_frame(String type) throws JSONException {
        // For sending
        JSONObject jobj = null;
        if (type.equals("Hello")) {
            JSONObject jroom = new JSONObject().put("name", this.room);

            JSONObject contents = new JSONObject()
                    .put("username", this.username)
                    .put("password", this.password)
                    .put("version", VERSION)
                    .put("room", jroom);

            jobj = new JSONObject().put("Hello", contents);
        }
        if (type.equals("State")) {
            setPosition((double) videoview.getCurrentPosition());
            JSONObject playstate = new JSONObject().put("paused", paused)
                    .put("position", position);

            JSONObject ping = new JSONObject().put("clientRtt", ourLatency)
                    .put("clientLatencyCalculation", (float) System.currentTimeMillis())
                    .put("latencyCalculation", latencyCalculation.longValue());

            JSONObject contents = new JSONObject().put("ping", ping)
                    .put("playstate", playstate);

            JSONObject ignore = null;
            if (ignoreOnFly == true && client == 1 && ignoreOnTheFly == null) {
                // Send a client ignoreOnTheFly
                ignore = new JSONObject().put("client", client);
                contents.put("ignoringOnTheFly", ignore);
                playstate.put("doSeek", doSeek);
            } else if (ignoreOnFly == true && ignoreOnTheFly.length() == 2
                    && ignoreOnTheFly.getInt("server") == client) {
                // NO DOSEEK HERE
                doSeek = false; // but no need to send this here.
                // Client sends final ACK and nullify variables and turn off ignore on fly
                ignore = new JSONObject().put("server", client);
                contents.put("ignoringOnTheFly", ignore);
                ignoreOnFly = false;
                ignoreOnTheFly = null;
                //client = 0;
            } else if (ignoreOnFly == true && ignoreOnTheFly.length() == 1
                    && ignoreOnTheFly.getInt(ignoreOnTheFly.keys().next()) == client) {
                // Respond to server's ignoreOnFly
                playstate.put("doSeek", doSeek);
                ignore = new JSONObject().put("client", client)
                        .put("server", client);
                contents.put("ignoringOnTheFly", ignore);
            } else {
                playstate.put("doSeek", doSeek); // is this required?
            }
            jobj = new JSONObject().put("State", contents);
        }
        if (type.equals("Set")) {
            // filename/filesize sent raw
            JSONObject details = new JSONObject().put("duration", mfile.getDuration())
                    .put("name", mfile.getFilename()).put("size", mfile.getFilesize());
            JSONObject contents = new JSONObject().put("file", details);
            jobj = new JSONObject().put("Set", contents);
            ignoreOnFly = true;
            //paused = false; // redundant?
            //videoview.start(); //cm ths
        }
        return jobj.toString();
    }

    public void showOSD(String m) {
        Message msg = new Message();
        msg.obj = m;
        OSDHandler.sendMessage(msg);
    }

    public class mediafile {
        String filename = "";
        Float duration;
        Long filesize = 0L;

        @TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
        public mediafile(Context c, Uri uri, Activity a) {
            // TODO: Get a better way to get filename and size
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(c, uri);
            String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            long timeInMillisec = Long.parseLong(time);
            this.duration = (float) timeInMillisec / 1000; // we need seconds.
            Cursor returnCursor = a.getContentResolver().query(uri, null, null, null, null);
            int nameIndex = -1, sizeIndex = -1;
            try {
                nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
            } catch (NullPointerException e) {
                e.printStackTrace();
            }

            returnCursor.moveToFirst();
            if (nameIndex != -1) {
                this.filename = returnCursor.getString(nameIndex);
                this.filesize = returnCursor.getLong(sizeIndex);
            }
        }

        public String getFilename() {
            return filename;
        }

        public Float getDuration() {
            return duration;
        }

        public Long getFilesize() {
            return filesize;
        }
    }

    protected class syncplayerror extends Exception {
        public syncplayerror(String msg) {
            super(msg);
        }
    }

}
