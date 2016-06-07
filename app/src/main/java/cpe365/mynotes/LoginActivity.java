package cpe365.mynotes;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A login screen that offers login via username/password.
 */
public class LoginActivity extends AppCompatActivity {
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;
    private AddUserTask mAddUser = null;

    // UI references.
    private EditText mUsernameView;
    private EditText mPasswordView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // Set up the login form.
        mUsernameView = (EditText) findViewById(R.id.username);
        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mSignInButton = (Button) findViewById(R.id.sign_in_button);
        if (mSignInButton != null) {
            mSignInButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    attemptLogin();
                }
            });
        }
    }

    public void onBackPressed() {
        System.exit(1);
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid username, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mUsernameView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String username = mUsernameView.getText().toString();
        String password = mPasswordView.getText().toString();

        username = username.trim();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }
        if (username.length() > 32) {
            mUsernameView.setError(getString(R.string.short_user));
            focusView = mUsernameView;
            cancel = true;
        }
        if (username.contains(" ")) {
            mUsernameView.setError(getString(R.string.contains_spaces));
            focusView = mUsernameView;
            cancel = true;
        }
        // Check for a valid username.
        if (TextUtils.isEmpty(username)) {
            mUsernameView.setError(getString(R.string.error_field_required));
            focusView = mUsernameView;
            cancel = true;
        }
        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            try {
                mAuthTask = new UserLoginTask(username, password);
            } catch (Exception e) {
                e.printStackTrace();
            }
            mAuthTask.execute((Void) null);
        }
    }

    private boolean isPasswordValid(String password) {
        return password.length() > 4;
    }

    /**
     * Represents an asynchronous login task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {
        private final String mUsername;
        private final String mHash;
        private String response;

        UserLoginTask(String username, String password) throws Exception {
            mUsername = username;

            byte[] bytesOfMessage = (password + username.toLowerCase() ).getBytes("UTF-8");
            MessageDigest md = MessageDigest.getInstance("MD5");

            byte[] bytes = md.digest(bytesOfMessage);

            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for(byte b: bytes)
                sb.append(String.format("%02x", b & 0xff));
            mHash = sb.toString();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            HttpURLConnection urlConnection = null;
            try {
                URL url = new URL(getString(R.string.server));
                urlConnection = (HttpURLConnection) url.openConnection();

                Map<String,Object> postParams = new LinkedHashMap<>();
                postParams.put("method","login");
                postParams.put("username",mUsername);
                postParams.put("passHash",mHash);

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

                JSONObject jObj = new JSONObject(json);
                response = jObj.getString("status");

                urlConnection.disconnect();
                return true;
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }

            if (urlConnection != null) urlConnection.disconnect();

            return false;
        }

        /**
         * Represents an asynchronous registration task used to register
         * the user.
         */
        @Override
        protected void onPostExecute(final Boolean success) {
            if (!success){
                mAuthTask = null;
                Toast toast = Toast.makeText(LoginActivity.this, R.string.fail, Toast.LENGTH_LONG);
                toast.show();
                return;
            }

            if (response.equals("0")) {
                Intent notes = new Intent(LoginActivity.this, NotesList.class);
                PreferenceManager.getDefaultSharedPreferences(LoginActivity.this).edit().putString("username", mUsername).apply();
                PreferenceManager.getDefaultSharedPreferences(LoginActivity.this).edit().putString("passHash", mHash).apply();
                finish();
                LoginActivity.this.startActivity(notes);
            } else if (response.equals("2")) {
                mPasswordView.setError(getString(R.string.error_incorrect_password));
                mPasswordView.requestFocus();
            } else {
                AlertDialog.Builder addUserDialog = new AlertDialog.Builder(LoginActivity.this);
                addUserDialog.setMessage(R.string.dialog_add_user)
                        .setPositiveButton(R.string.add_user, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                try {
                                    if (mAddUser == null) {
                                        mAddUser = new AddUserTask(mUsername, mHash);
                                        mAddUser.execute((Void) null);
                                    }
                                } catch (Exception e) {
                                    Toast toast = Toast.makeText(LoginActivity.this, R.string.fail, Toast.LENGTH_LONG);
                                    toast.show();
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
            mAuthTask = null;
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
        }
    }

    public class AddUserTask extends AsyncTask<Void, Void, Boolean> {
        private final String mUsername;
        private final String mHash;

        AddUserTask(String username, String hash) throws Exception {
            mUsername = username;
            mHash = hash;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            HttpURLConnection urlConnection = null;
            try {
                URL url = new URL(getString(R.string.server));
                urlConnection = (HttpURLConnection) url.openConnection();

                Map<String,Object> postParams = new LinkedHashMap<>();
                postParams.put("method","addUser");
                postParams.put("username",mUsername);
                postParams.put("passHash",mHash);

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
            mAddUser = null;
            if (!success)
                Toast.makeText(LoginActivity.this, R.string.fail, Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(LoginActivity.this, R.string.success, Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onCancelled() {
            mAddUser = null;
        }
    }
}

