package org.sdf.becquerel.bloc;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import org.jsoup.nodes.Document;

import java.util.HashMap;

public class CommViewActivity extends AppCompatActivity {
    private String playerID;
    private String message;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comm_view);
        HashMap<String, String> map = (HashMap<String, String>) getIntent().getSerializableExtra("comm_map");
        playerID = map.get("playerID");
        message = map.get("message");
        ((TextView) findViewById(R.id.comm_text)).setText(message);
        ((TextView) findViewById(R.id.comm_footer)).setText("From " + map.get("player") + " at " + map.get("date"));
    }

    public void onClick(View v) {
        if(v.getId() == R.id.reply_button) {
            HTTP http = new HTTP();
            HashMap<String, String> params = new HashMap<String, String>();
            params.put("message", ((android.widget.EditText) findViewById(R.id.comm_reply)).getText().toString());
            params.put("id", playerID);
            http.post(getString(R.string.url_player) + playerID, params, new HTTP.DataDownloadListener() {
                        @SuppressWarnings("unchecked")
                        @Override
                        public void dataDownloadedSuccessfully(Object data) {
                            if (data.toString().contains("Login")) {
                                BlocFragment.alert("You are not logged in or have timed out. Go back and log in again.", CommViewActivity.this);
                            } else {
                                if(data.toString().contains(getString(R.string.comm_sent_success))) {
                                    BlocFragment.alert("Communique sent.", CommViewActivity.this);
                                    finish();
                                } else {
                                    BlocFragment.alert("Sending failed.", CommViewActivity.this);
                                }
                            }
                        }

                        @Override
                        public void dataDownloadFailed() {
                            // handler failure (e.g network not available etc.)
                        }
                    }
            );
        }
    }

}
