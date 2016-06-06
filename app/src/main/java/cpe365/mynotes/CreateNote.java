package cpe365.mynotes;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

public class CreateNote extends AppCompatActivity {
    private EditText mTitle;
    private EditText mNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_note);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mTitle = (EditText) findViewById(R.id.titleInput);
        mNote = (EditText) findViewById(R.id.contentInput);

        FloatingActionButton save = (FloatingActionButton) findViewById(R.id.save);
        if (save != null) {
            save.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    SaveNote save = new SaveNote(mTitle.getText().toString(), mNote.getText().toString());
                    save.execute((Void) null);
                    finish();
                    Intent notes = new Intent(CreateNote.this, NotesList.class);
                    CreateNote.this.startActivity(notes);
                }
            });
        }
    }

    public class SaveNote extends AsyncTask<Void, Void, Boolean> {
        private final String mTitle;
        private final String mNote;

        public SaveNote(String title, String note) {
            mTitle = title;
            mNote = note;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            HttpURLConnection urlConnection = null;
            try {
                URL url = new URL(getString(R.string.server));
                urlConnection = (HttpURLConnection) url.openConnection();

                Map<String,Object> postParams = new LinkedHashMap<>();

                String username = PreferenceManager.getDefaultSharedPreferences(CreateNote.this).getString("username", "xxxx");
                String passHash = PreferenceManager.getDefaultSharedPreferences(CreateNote.this).getString("passHash", "xxxx");
                postParams.put("method","addNote");
                postParams.put("username", username);
                postParams.put("passHash", passHash);
                postParams.put("title", mTitle);
                postParams.put("noteText", mNote);

                StringBuilder postData = new StringBuilder();

                for (Map.Entry<String,Object> param : postParams.entrySet()) {
                    if (postData.length() != 0) postData.append('&');
                    postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
                    postData.append('=');
                    postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
                }
                byte[] postDataBytes = postData.toString().getBytes("UTF-8");

                HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.getOutputStream().write(postDataBytes);
                conn.getInputStream();

                urlConnection.disconnect();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (urlConnection != null) urlConnection.disconnect();

            return false;
        }

        protected void onPostExecute(final Boolean success) {
            if (!success) {
                Toast toast = Toast.makeText(CreateNote.this, R.string.fail, Toast.LENGTH_LONG);
                toast.show();
            } else {
                Toast toast = Toast.makeText(CreateNote.this, R.string.saved, Toast.LENGTH_LONG);
                toast.show();
            }
        }
    }
}
