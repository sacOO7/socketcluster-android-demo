package git.cluster.io.socketclusterandroid;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFrame;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

import hani.momanii.supernova_emoji_library.Actions.EmojIconActions;
import hani.momanii.supernova_emoji_library.Helper.EmojiconEditText;
import hani.momanii.supernova_emoji_library.Helper.EmojiconTextView;
import io.github.sac.Ack;
import io.github.sac.BasicListener;
import io.github.sac.Emitter;
import io.github.sac.EventThread;
import io.github.sac.ReconnectStrategy;
import io.github.sac.Socket;

public class MainActivity extends AppCompatActivity {

    String url="ws://192.168.0.4:8000/socketcluster/";
    Socket socket;
    EmojiconEditText emojiconEditText;
    ImageView emojiButton;
    ScrollView scrollView;
    ImageView submitButton;
    View rootView;
    LinearLayout container;
    EmojIconActions emojIcon;
    EditText username;
    String Username="Demo";
    Handler Typinghandler=new Handler();
    Boolean typing=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        socket = new Socket(url);
        socket.setListener(new BasicListener() {

            public void onConnected(Socket socket, Map<String, List<String>> headers) {
                socket.createChannel("MyClassroom").subscribe(new Ack() {
                    @Override
                    public void call(String name, Object error, Object data) {
                        if (error==null){
                            Log.i ("Success","subscribed to channel "+name);
                        }
                    }
                });
                Log.i("Success ","Connected to endpoint");
            }

            public void onDisconnected(Socket socket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) {
                Log.i("Success ","Disconnected from end-point");
            }

            public void onConnectError(Socket socket,WebSocketException exception) {
                Log.i("Success ","Got connect error "+ exception);
            }

            public void onSetAuthToken(String token, Socket socket) {
                socket.setAuthToken(token);
            }

            public void onAuthentication(Socket socket,Boolean status) {
                if (status) {
                    Log.i("Success ","socket is authenticated");
                } else {
                    Log.i("Success ","Authentication is required (optional)");
                }
            }

        });

        socket.setReconnection(new ReconnectStrategy().setMaxAttempts(10).setDelay(3000));

        socket.connectAsync();

        rootView = findViewById(R.id.root_view);
        emojiButton = (ImageView) findViewById(R.id.emoji_btn);
        submitButton = (ImageView) findViewById(R.id.submit_btn);
        container= (LinearLayout) findViewById(R.id.container);
        scrollView= (ScrollView) findViewById(R.id.scroll);
        emojiconEditText = (EmojiconEditText) findViewById(R.id.emojicon_edit_text);
        emojIcon=new EmojIconActions(this,rootView,emojiconEditText,emojiButton,"#495C66","#DCE1E2","#E6EBEF");
        emojIcon.ShowEmojIcon();
        emojIcon.setKeyboardListener(new EmojIconActions.KeyboardListener() {
            @Override
            public void onKeyboardOpen() {
                Log.e("Keyboard","open");
            }

            @Override
            public void onKeyboardClose() {
                Log.e("Keyboard","close");
            }
        });

        View v=getLayoutInflater().inflate(R.layout.dialogue_layout, null);
        username= (EditText) v.findViewById(R.id.username_input);
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setView(v)
                .setCancelable(false)
                .setPositiveButton(R.string.login, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // sign in the user ...
                        Username=username.getText().toString();
                        JSONObject object=new JSONObject();
                        try {
                            object.put("ismessage",false);
                            object.put("data","<b><gray>"+Username+"</b></gray> joined the group");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        socket.getChannelByName("MyClassroom").publish(object);

                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .create()
                .show();

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                JSONObject object=new JSONObject();
                try {
                    object.put("user",Username);
                    object.put("ismessage",true);
                    object.put("data",emojiconEditText.getText().toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                socket.getChannelByName("MyClassroom").publish(object, new Ack() {
                    @Override
                    public void call(String name, Object error, Object data) {
                        if (error==null){
                            Log.i ("Success","Publish sent successfully");
                        }
                    }
                });
                emojiconEditText.setText("");
            }
        });

        emojiconEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (Username==null) return;

                if (!typing){
                    typing=true;
                    if (!s.equals("")) {
                        JSONObject object = new JSONObject();
                        try {
                            object.put("user", Username);
                            object.put("istyping", true);
                            object.put("data", "<b>" + Username + "</b> is typing...");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        socket.getChannelByName("MyClassroom").publish(object);
                    }
                }

                Typinghandler.removeCallbacks(onTypingTimeout);
                Typinghandler.postDelayed(onTypingTimeout,600);

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        socket.onSubscribe("MyClassroom",new Emitter.Listener() {
            @Override
            public void call(String name, final Object data) {

                    try {
                        JSONObject object= (JSONObject) data;
                        if (object.opt("istyping")!=null && !object.getString("user").equals(Username)){
                            if (object.getBoolean("istyping")){
                                final TextView textView=getTextView();
                                textView.setText(Html.fromHtml(object.getString("data")));
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        container.addView(textView);
                                        scrollView.scrollTo(0,scrollView.getBottom());
                                    }
                                });
                            }else{
                                Log.i ("success","Stopped typing");
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        container.removeViewAt(container.getChildCount()-1);
                                    }
                                });
                            }

                        }else {
                            final TextView textView=getTextView();
                            if (!object.getBoolean("ismessage")) {
                                textView.setGravity(Gravity.CENTER_HORIZONTAL);
                                textView.setText(Html.fromHtml(object.getString("data")));
                            } else {
                                textView.setText(Html.fromHtml("<b>" + object.getString("user") + "</b> : " + object.getString("data")));
                            }
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    container.addView(textView);
                                    scrollView.scrollTo(0,scrollView.getBottom());
                                }
                            });

                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
            }
        });

    }

    private EmojiconTextView getTextView(){
        EmojiconTextView textView = new EmojiconTextView(MainActivity.this);
        textView.setUseSystemDefault(false);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        textView.setEmojiconSize(44);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 5, 0, 5);
        textView.setLayoutParams(params);
        return textView;
    }
    private Runnable onTypingTimeout = new Runnable() {
        @Override
        public void run() {
            if (!typing) return;

            typing = false;
//            mSocket.emit("stop typing");
            JSONObject object=new JSONObject();
            try {
                object.put("user",Username);
                object.put("istyping",false);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            socket.getChannelByName("MyClassroom").publish(object);
        }
    };

    @Override
    public void onBackPressed() {

        JSONObject object=new JSONObject();
        try {
            object.put("ismessage",false);
            object.put("data","<b><gray>"+Username+"</b></gray> leaved the group");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        socket.getChannelByName("MyClassroom").publish(object);

        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
    }
}
