package cpe365.mynotes;

import android.app.ActionBar;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
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

/**
 * Class to represent the page viewed when notes are searched
 */
public class SearchNotes extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_notes);

        String search = getIntent().getStringExtra("search");

        setTitle("Search: \"" + search + "\"");

        new SearchNotesTask(search).execute();

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(false); // disable the button
            actionBar.setDisplayHomeAsUpEnabled(false); // remove the left caret
            actionBar.setDisplayShowHomeEnabled(false); // remove the icon
        }

    }

    // interrupt back button pressed to direct application where needed
    public void onBackPressed() {
        finish();
        this.startActivity(new Intent(SearchNotes.this,NotesList.class));
    }

    /**
     * Represents an asynchronous note search task used to search database for notes
     */
    public class SearchNotesTask extends AsyncTask<Void, Void, Boolean> {
        private final String mSearch;
        private JSONArray notesList = null;

        public SearchNotesTask(String search) {
            mSearch = search;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            HttpURLConnection urlConnection = null;
            try {
                URL url = new URL(getString(R.string.server));
                urlConnection = (HttpURLConnection) url.openConnection();

                String username = PreferenceManager.getDefaultSharedPreferences(SearchNotes.this).getString("username", "xxxx");
                String hash = PreferenceManager.getDefaultSharedPreferences(SearchNotes.this).getString("passHash", "xxxx");

                Map<String,Object> postParams = new LinkedHashMap<>();
                postParams.put("method","search");
                postParams.put("username", username);
                postParams.put("passHash", hash);
                postParams.put("string", mSearch);

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

                urlConnection.disconnect();
                return true;
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }

            if (urlConnection != null) urlConnection.disconnect();

            return false;
        }

        protected void onPostExecute(final Boolean success) {
            if (!success ){
                Toast.makeText(SearchNotes.this, R.string.fail, Toast.LENGTH_SHORT).show();
                return;
            }

            ListView notes = (ListView) findViewById(R.id.searchList);

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

            final ArrayAdapter<String> listAdapter = new ArrayAdapter<String>(SearchNotes.this, android.R.layout.simple_list_item_2, android.R.id.text1, list){
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
            assert(notes != null);
            notes.setClickable(true);
            notes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView parentView, View childView, int position, long id) {
                    Intent viewNote = new Intent(SearchNotes.this, NoteView.class);
                    viewNote.putExtra("noteId", Integer.toString(finalIds[position]));
                    viewNote.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    finish();
                    startActivity(viewNote);
                }
            });

            notes.setAdapter(listAdapter);

            if (notesList.length() == 0) {
                RelativeLayout mainLayout = (RelativeLayout) findViewById(R.id.searchLay);
                assert(mainLayout != null);
                TextView textView = new TextView(SearchNotes.this);
                textView.setTextSize(15);

                textView.setText(R.string.empty_search);
                RelativeLayout.LayoutParams params =
                        new RelativeLayout.LayoutParams(
                                LinearLayoutCompat.LayoutParams.WRAP_CONTENT,
                                LinearLayoutCompat.LayoutParams.WRAP_CONTENT);
                textView.setLayoutParams(params);
                mainLayout.addView(textView);
            }
        }
    }
}
