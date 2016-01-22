package org.sdf.becquerel.bloc.pages;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.sdf.becquerel.bloc.BlocFragment;
import org.sdf.becquerel.bloc.HTTP;
import org.sdf.becquerel.bloc.R;

import java.util.HashMap;

/**
 * Created by stephen on 22/01/16.
 */
public class LoginFragment extends BlocFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.login, container, false);

        http.get(getActivity().getResources().getString(R.string.url_nation), new HTTP.DataDownloadListener() {
            public void dataDownloadedSuccessfully(Object data) {
                if (data.toString().contains("Login")) {
                    Button b = (Button) rootView.findViewById(R.id.login_button);
                    b.setOnClickListener(LoginFragment.this);
                } else {
                    getActivity().findViewById(R.id.user).setVisibility(View.INVISIBLE);
                    getActivity().findViewById(R.id.pwd).setVisibility(View.INVISIBLE);
                    getActivity().findViewById(R.id.login_button).setVisibility(View.INVISIBLE);
                    ((TextView) getActivity().findViewById(R.id.textView3)).setText("You are already logged in!");
                }
            }
            @Override
            public void dataDownloadFailed() {
                // handler failure (e.g network not available etc.)
            }
        });
        return rootView;
    }
    @Override
    public void onClick(View v) {
        Integer id = v.getId();
        if (id == R.id.login_button) {
            EditText edit = (EditText) rootView.findViewById(R.id.user);
            String user = edit.getText().toString();
            edit = (EditText) rootView.findViewById(R.id.pwd);
            String pass = edit.getText().toString();
            doLogin(user, pass);
            Log.d("dummy", "login sent");
        }
    }

    private void doLogin(String user, String pass) {
        //The HTTP method calls are a mess because they must run on a background thread
        //so everything is done in callbacks

        //GET the login page first to get a PHPSESSID cookie
        http.get(getString(R.string.url_login), new HTTP.DataDownloadListener() {
            public void dataDownloadedSuccessfully(Object data) {
            }

            public void dataDownloadFailed() {
                //TODO: handle connection failure
            }
        });
        //Build the request and POST it to login.php
        HashMap<String, String> params = new HashMap<String, String>();
        params.put(getString(R.string.login_userfield), user);
        params.put(getString(R.string.login_passfield), pass);
        params.put(getString(R.string.login_buttonname), "");
        http.post(getString(R.string.url_login), params, new HTTP.DataDownloadListener() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public void dataDownloadedSuccessfully(Object data) {
                        //login successful, send user to nation page
                        Fragment fragment = BlocFragment.fragmentChooser(R.layout.nation, 0);
                        getFragmentManager().beginTransaction().replace(R.id.frame_container, fragment).commit();
                        getActivity().setTitle("Your nation");
                    }

                    @Override
                    public void dataDownloadFailed() {
                        // handler failure (e.g network not available etc.)
                    }
                }
        );
    }
}
