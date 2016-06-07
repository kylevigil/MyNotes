package cpe365.mynotes;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
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
    private boolean modify = false;
    private String noteId = "";

    private SaveNoteTask mSaveNote = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_note);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mTitle = (EditText) findViewById(R.id.titleInput);
        mNote = (EditText) findViewById(R.id.contentInput);


        if (getIntent().hasExtra("existing")) {
            modify = true;
            noteId = getIntent().getStringExtra("noteId");
            mTitle.append(getIntent().getStringExtra("title"));
            mNote.append(getIntent().getStringExtra("noteText"));
        }

        FloatingActionButton save = (FloatingActionButton) findViewById(R.id.save);
        if (save != null) {
            save.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if (mTitle.getText().toString().length() == 0) {
                        mTitle.setError(getString(R.string.no_title));
                        mTitle.requestFocus();
                    } else if (mTitle.getText().toString().length() > 64) {
                        mTitle.setError(getString(R.string.long_title));
                        mTitle.requestFocus();
                    } else if (!isGoodTag(mNote.getText().toString())) {
                        mNote.setError(getString(R.string.tag_too_long));
                        mNote.requestFocus();
                    }
                    else {
                        if (mSaveNote == null) {
                            mSaveNote = new SaveNoteTask(mTitle.getText().toString(), mNote.getText().toString(), modify, noteId);
                            mSaveNote.execute((Void) null);
                        }
                    }
                }
            });
        }
    }


    public static boolean isGoodTag(String noteText) {
        if (noteText.isEmpty()) return true;
        String[] words = noteText.split(" ");

        for (String word : words) {
            if (word.length() > 1 && word.charAt(0) == '#' && word.length() > 64) {
                return false;
            }
        }
        return true;
    }


    public void onBackPressed() {
        AlertDialog.Builder addUserDialog = new AlertDialog.Builder(CreateNote.this);
        addUserDialog.setMessage(R.string.confirm_return)
                .setPositiveButton("Discard", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (!modify) {
                            finish();
                            Intent r = new Intent(CreateNote.this, NotesList.class);
                            r.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            CreateNote.this.startActivity(r);
                        } else {
                            finish();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });
        addUserDialog.show();
    }

    public class SaveNoteTask extends AsyncTask<Void, Void, Boolean> {
        private final String mTitle;
        private final String mNote;
        private final boolean mModify;
        private final String mNoteId;

        public SaveNoteTask(String title, String note, boolean modify, String noteId) {
            mTitle = title;
            mNote = note;
            mModify = modify;
            mNoteId = noteId;
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
                if (mModify) {
                    postParams.put("method", "modifyNote");
                    postParams.put("id", mNoteId);
                } else {
                    postParams.put("method", "addNote");
                }
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

        @Override
        protected void onPostExecute(final Boolean success) {
            mSaveNote = null;
            if (!success) {
                Toast toast = Toast.makeText(CreateNote.this, R.string.fail, Toast.LENGTH_LONG);
                toast.show();
            } else {
                Toast toast = Toast.makeText(CreateNote.this, R.string.saved, Toast.LENGTH_LONG);
                toast.show();

                CreateNote.this.finish();
                Intent notes = new Intent(CreateNote.this, NotesList.class);
                CreateNote.this.startActivity(notes);
            }
        }

        @Override
        protected void onCancelled() {
            mSaveNote = null;
        }
    }
}
