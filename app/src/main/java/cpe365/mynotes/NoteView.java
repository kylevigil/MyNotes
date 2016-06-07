package cpe365.mynotes;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

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
    private DeleteNote mDelete = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_view);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setTitle(R.string.app_name);
        final String noteId = getIntent().getStringExtra("noteId");

        final RetrieveNoteTask getNote = new RetrieveNoteTask(noteId);
        getNote.execute((Void) null);

        FloatingActionButton delete = (FloatingActionButton) findViewById(R.id.delete);
        assert delete != null;
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mDelete == null) {
                    mDelete = new DeleteNote(noteId);
                    mDelete.execute((Void) null);
                    finish();
                    startActivity(new Intent(NoteView.this, NotesList.class));
                }
            }
        });
        FloatingActionButton edit = (FloatingActionButton) findViewById(R.id.edit);
        assert edit != null;
        edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getNote.getStatus() == AsyncTask.Status.FINISHED) {
                    Intent editNote = new Intent(NoteView.this, CreateNote.class);
                    editNote.putExtra("existing", "1");
                    editNote.putExtra("noteId", noteId);
                    editNote.putExtra("title", getNote.getTitle());
                    editNote.putExtra("noteText", getNote.getNoteText());
                    startActivity(editNote);
                }
            }
        });
    }

    public void onBackPressed() {
        finish();
        this.startActivity(new Intent(NoteView.this,NotesList.class));
    }

    public class RetrieveNoteTask extends AsyncTask<Void, Void, Boolean> {
        private final String mNoteId;
        private JSONObject note;
        private String mNoteText;
        private String mNoteTitle;

        public RetrieveNoteTask(String noteId) {
            mNoteId = noteId;
        }

        public String getTitle() { return mNoteTitle; }
        public String getNoteText() { return mNoteText; }

        @Override
        protected Boolean doInBackground(Void... params) {
            HttpURLConnection urlConnection = null;
            try {
                URL url = new URL(getString(R.string.server));
                urlConnection = (HttpURLConnection) url.openConnection();

                String username = PreferenceManager.getDefaultSharedPreferences(NoteView.this).getString("username", "xxxx");
                String hash = PreferenceManager.getDefaultSharedPreferences(NoteView.this).getString("passHash", "xxxx");

                Map<String, Object> postParams = new LinkedHashMap<>();
                postParams.put("method", "getNote");
                postParams.put("username", username);
                postParams.put("passHash", hash);
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

                urlConnection.disconnect();
                return true;

            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }

            if (urlConnection != null) urlConnection.disconnect();

            return false;
        }

        protected void onPostExecute(final Boolean success) {
            if (!success) {
                Toast toast = Toast.makeText(NoteView.this, R.string.fail, Toast.LENGTH_LONG);
                toast.show();
                finish();
                NoteView.this.startActivity(new Intent(NoteView.this,NotesList.class));
                return;
            }
            TextView title = (TextView) findViewById(R.id.title);
            TextView noteText = (TextView) findViewById(R.id.noteText);

            try {
                mNoteTitle = note.getString("title");
                mNoteText = note.getString("noteText");

                assert noteText != null;
                assert title != null;
                title.setText(mNoteTitle);
                noteText.setText(mNoteText);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public class DeleteNote extends AsyncTask<Void, Void, Boolean> {
        private final String mId;

        public DeleteNote(String id) {
            mId = id;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            HttpURLConnection urlConnection = null;
            try {
                URL url = new URL(getString(R.string.server));
                urlConnection = (HttpURLConnection) url.openConnection();

                Map<String,Object> postParams = new LinkedHashMap<>();

                String username = PreferenceManager.getDefaultSharedPreferences(NoteView.this).getString("username", "xxxx");
                String passHash = PreferenceManager.getDefaultSharedPreferences(NoteView.this).getString("passHash", "xxxx");
                postParams.put("method","deleteNote");
                postParams.put("username", username);
                postParams.put("passHash", passHash);
                postParams.put("id", mId);

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
            mDelete = null;
            if (!success) {
                Toast toast = Toast.makeText(NoteView.this, R.string.fail, Toast.LENGTH_LONG);
                toast.show();
            } else {
                Toast toast = Toast.makeText(NoteView.this, R.string.deleted, Toast.LENGTH_LONG);
                toast.show();
            }
        }

        @Override
        protected void onCancelled() { mDelete = null; }
    }
}
