package cpe365.mynotes;

import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

public class NoteView extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_view);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        String noteId = getIntent().getStringExtra("noteId");
        String username = PreferenceManager.getDefaultSharedPreferences(NoteView.this).getString("username", "xxxx");
        String passHash = PreferenceManager.getDefaultSharedPreferences(NoteView.this).getString("passHash", "xxxx");

        RetrieveNoteTask getNote = new RetrieveNoteTask(username, passHash, noteId);
        getNote.execute((Void) null);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.delete);
        assert fab != null;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public class RetrieveNoteTask extends AsyncTask<Void, Void, Boolean> {
        private final String mUsername;
        private final String mHash;
        private final String mNoteId;
        private JSONObject note;
        private String response;

        public RetrieveNoteTask(String username, String passHash, String noteId) {
            mUsername = username;
            mHash = passHash;
            mNoteId = noteId;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            HttpURLConnection urlConnection = null;
            try {
                URL url = new URL(getString(R.string.server));
                urlConnection = (HttpURLConnection) url.openConnection();

                Map<String, Object> postParams = new LinkedHashMap<>();
                postParams.put("method", "getNote");
                postParams.put("username", mUsername);
                postParams.put("passHash", mHash);
                postParams.put("id", mNoteId);

                StringBuilder postData = new StringBuilder();

                for (Map.Entry<String, Object> param : postParams.entrySet()) {
                    if (postData.length() != 0) postData.append('&');
                    postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
                    postData.append('=');
                    postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
                }
                byte[] postDataBytes = postData.toString().getBytes("UTF-8");

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.getOutputStream().write(postDataBytes);

                Reader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

                String json = "";
                for (int c; (c = in.read()) >= 0; )
                    json += (char) c;

                JSONArray j = new JSONArray(json);
                note = new JSONObject(j.getString(0));

                response = json;
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }

            if (urlConnection != null) urlConnection.disconnect();

            return true;
        }

        protected void onPostExecute(final Boolean success) {
            TextView title = (TextView) findViewById(R.id.title);
            TextView noteText = (TextView) findViewById(R.id.noteText);

            try {
                title.setText(note.getString("title"));
                noteText.setText(note.getString("noteText"));
            } catch (JSONException e) {
                e.printStackTrace();
            }

//            Toast.makeText(NoteView.this, response, Toast.LENGTH_LONG).show();

        }
    }
}
