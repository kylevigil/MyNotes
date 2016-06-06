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
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class NotesList extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        String username = PreferenceManager.getDefaultSharedPreferences(NotesList.this).getString("username", "xxxx");
        String passHash = PreferenceManager.getDefaultSharedPreferences(NotesList.this).getString("passHash", "xxxx");

        RetrieveNotesList getNotes = new RetrieveNotesList(username, passHash);
        getNotes.execute((Void) null);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.createNote);
        if (fab != null) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    finish();
                    Intent notes = new Intent(NotesList.this, CreateNote.class);
                    NotesList.this.startActivity(notes);
                }
            });
        }
    }


    public class RetrieveNotesList extends AsyncTask<Void, Void, Boolean> {
        private final String mUsername;
        private final String mHash;
        private JSONArray notesList;

        public RetrieveNotesList(String username, String passHash) {
            mUsername = username;
            mHash = passHash;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            HttpURLConnection urlConnection = null;
            try {
                URL url = new URL(getString(R.string.server));
                urlConnection = (HttpURLConnection) url.openConnection();

                Map<String,Object> postParams = new LinkedHashMap<>();
                postParams.put("method","getNotes");
                postParams.put("username", mUsername);
                postParams.put("passHash", mHash);

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

                Reader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

                String json = "";
                for (int c; (c = in.read()) >= 0;)
                    json += (char)c;

                notesList = new JSONArray(json);

            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }

            if (urlConnection != null) urlConnection.disconnect();

            return true;
        }

        protected void onPostExecute(final Boolean success) {
            if (notesList == null) {
                return;
            }

            ListView notes = (ListView) findViewById(R.id.notesList);

            String[] titles = new String[notesList.length()];
            String[] tags = new String[notesList.length()];
            int[] ids = new int[notesList.length()];

            for (int i = 0; i < notesList.length(); i++) {
                try {
                    JSONObject note = new JSONObject(notesList.getString(i));
                    titles[i] = note.getString("title");
                    ids[i] = note.getInt("id");

                    if (!note.getString("tags").equals("null")) {
                        tags[i] = note.getString("tags");
                    } else {
                        tags[i] = "";
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            final String[] finalTitle = titles;
            final String[] finalTag = tags;
            final int[] finalIds = ids;

            ArrayList<String> list = new ArrayList<>();
            list.addAll(Arrays.asList(titles));

            final ArrayAdapter<String> listAdapter = new ArrayAdapter<String>(NotesList.this, android.R.layout.simple_list_item_2, android.R.id.text1, list){
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View view = super.getView(position, convertView, parent);
                    TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                    TextView text2 = (TextView) view.findViewById(android.R.id.text2);

                    text1.setText(finalTitle[position]);
                    text2.setText(finalTag[position]);
                    return view;
                }
            };
            if(notes != null) {
                notes.setClickable(true);
                notes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    public void onItemClick(AdapterView parentView, View childView, int position, long id) {
                        Intent viewNote = new Intent(NotesList.this, NoteView.class);
                        viewNote.putExtra("noteId", Integer.toString(finalIds[position]));
                        startActivity(viewNote);
                    }
                });

                notes.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                    public boolean onItemLongClick(AdapterView parentView, View childView, int position, long id) {
                        final int fPosition = position;
                        AlertDialog.Builder deleteNote = new AlertDialog.Builder(NotesList.this);
                        deleteNote.setMessage(R.string.delete)
                                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        try {
                                            DeleteNote addUser = new DeleteNote(Integer.toString(finalIds[fPosition]));
                                            addUser.execute((Void) null);
                                            finish();
                                            startActivity(new Intent(NotesList.this, NotesList.class));
                                        } catch (Exception e) {
                                            Toast toast = Toast.makeText(NotesList.this, R.string.fail, Toast.LENGTH_LONG);
                                            toast.show();
                                        }
                                    }
                                })
                                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        // User cancelled the dialog
                                    }
                                });
                        deleteNote.show();

                        return true;
                    }
                });

                notes.setAdapter(listAdapter);
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

                String username = PreferenceManager.getDefaultSharedPreferences(NotesList.this).getString("username", "xxxx");
                String passHash = PreferenceManager.getDefaultSharedPreferences(NotesList.this).getString("passHash", "xxxx");
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
            if (!success) {
                Toast toast = Toast.makeText(NotesList.this, R.string.fail, Toast.LENGTH_LONG);
                toast.show();
            } else {
                Toast toast = Toast.makeText(NotesList.this, R.string.deleted, Toast.LENGTH_LONG);
                toast.show();
            }
        }
    }
}
