package org.sdf.becquerel.bloc;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Created by stephen on 19/12/15.
 */

public class BlocFragment extends Fragment implements View.OnClickListener {
    public BlocFragment(){}
    private View rootView;
    private HTTP http = new HTTP();

    //See strings.xml
    private List<String> econLabels = null;
    private List<String> milLabels = null;
    //See arrays.xml
    private List<Integer> domesticPolicyIDs = null;
    private String[] domesticPolicyURLs = null;
    private List<String> allDomesticPolicies = null;
    private List<Integer> militaryPolicyIDs = null;
    private String[] militaryPolicyURLs = null;
    private List<String> allMilitaryPolicies = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        TypedArray tmpArr;
        Integer[] newArr;
        Bundle bundle=getArguments();
        Integer fragment = bundle.getInt("ID");
        rootView = inflater.inflate(fragment, container, false);
        //Page fragments get created with an identifying ID
        switch(bundle.getInt("ID")) {
            case R.layout.login:
                //Login page is mostly defined in XML. Just need to set click handler.
                Button b = (Button) rootView.findViewById(R.id.login_button);
                b.setOnClickListener(this);
                break;
            case R.layout.nation:
                //TODO: make listed stats user-configurable in preferences
                econLabels = Arrays.asList(getActivity().getResources().getStringArray(R.array.econ_labels));
                milLabels = Arrays.asList(getActivity().getResources().getStringArray(R.array.mil_labels));
                getNationStats();
                break;
            case R.layout.domestic:
                //Convert int[] to Integer[]
                tmpArr = getActivity().getResources().obtainTypedArray(R.array.domestic_policy_id);
                newArr = new Integer[tmpArr.length()];
                for(int i = 0; i < tmpArr.length(); i++) {
                    newArr[i] = tmpArr.getResourceId(i, 0);
                }
                //Grab resource values here when we know the Activity will be initialized
                domesticPolicyIDs = Arrays.asList(newArr);
                domesticPolicyURLs = getActivity().getResources().getStringArray(R.array.domestic_policy_url);
                allDomesticPolicies = Arrays.asList(getActivity().getResources().getStringArray(R.array.all_domestic_policies));
                getPolicies(getString(R.string.url_domestic), R.layout.domestic, domesticPolicyIDs, allDomesticPolicies, R.id.domestic_table);
                break;
            case R.layout.military:
                tmpArr = getActivity().getResources().obtainTypedArray(R.array.military_policy_id);
                newArr = new Integer[tmpArr.length()];
                for(int i = 0; i < tmpArr.length(); i++) {
                    newArr[i] = tmpArr.getResourceId(i, 0);
                }
                militaryPolicyIDs = Arrays.asList(newArr);
                militaryPolicyURLs = getActivity().getResources().getStringArray(R.array.military_policy_url);
                allMilitaryPolicies = Arrays.asList(getActivity().getResources().getStringArray(R.array.all_military_policies));
                getPolicies(getString(R.string.url_military), R.layout.military, militaryPolicyIDs, allMilitaryPolicies, R.id.military_table);
                break;

            default:
                //
                break;
        }

        return rootView;
    }

    //General-purpose click handler for any buttons in any instance of a BlocFragment
    //e.g. the "Login" button, buttons for various policies, in future communique delete/send etc.
    public void onClick(View v) {
        Integer id = v.getId();
        if(id == R.id.login_button) {
            EditText edit = (EditText)  rootView.findViewById(R.id.user);
            String user = edit.getText().toString();
            edit = (EditText) rootView.findViewById(R.id.pwd);
            String pass = edit.getText().toString();
            doLogin(user, pass);
            Log.d("dummy", "login sent");
        }
        else if(domesticPolicyIDs.contains(id)) {
            String url = domesticPolicyURLs[domesticPolicyIDs.indexOf(id)];
            doPolicy(url);
        }
        else if(militaryPolicyIDs.contains(id)) {
            String url = militaryPolicyURLs[militaryPolicyIDs.indexOf(id)];
            doPolicy(url);
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
                        Bundle args = new Bundle();
                        args.putInt("ID", R.layout.nation);
                        Fragment fragment = null;
                        fragment = new BlocFragment();
                        fragment.setArguments(args);
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

    //Parse home/nation page to get stats
    private void getNationStats() {
        http.get(getString(R.string.url_nation), new HTTP.DataDownloadListener() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public void dataDownloadedSuccessfully(Object data) {
                        if (data.toString().contains("Login")) {
                            alert("You are not logged in or have timed out. Go back and log in again.");
                        } else {
                            Document doc = parse(data.toString());
                            Map<String, String> stats = parseNation(doc);
                            buildTable((TableLayout) rootView.findViewById(R.id.nation_table), stats);
                            ((MainActivity) getActivity()).updateBottomBar(parseBottomBar(doc));
                        }
                    }

                    @Override
                    public void dataDownloadFailed() {
                        // handler failure (e.g network not available etc.)
                    }
                }
        );
    }

    private void doPolicy(String url) {
        http.get(url, new HTTP.DataDownloadListener() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public void dataDownloadedSuccessfully(Object data) {
                        if (data.toString().contains("Login")) {
                            alert("You are not logged in or have timed out. Go back and log in again.");
                        } else {
                            //policy php pages consist of one line of text containing the result
                            String reply = parse(data.toString()).select("h4").first().text();
                            alert(reply);
                        }
                    }

                    @Override
                    public void dataDownloadFailed() {
                        // handler failure (e.g network not available etc.)
                    }
                }
        );
    }
    //Builds a two-column table for the home/nation page
    private void buildTable(TableLayout table, Map<String, String> data) {
        for (Map.Entry<String, String> s : data.entrySet()) {
            TableRow row = new TableRow(getActivity().getApplicationContext());
            TextView tl = new TextView(getActivity().getApplicationContext());
            tl.setText(s.getKey());
            tl.setTextColor(0xFF000000);
            TextView tr = new TextView(getActivity().getApplicationContext());
            tr.setText(s.getValue());
            tr.setTextColor(0xFF000000);
            row.addView(tl);
            row.addView(tr);
            table.addView(row);
        }
    }
    //Builds a three-column table with policy title, cost, and button to do the policy
    private void buildPolicyTable(TableLayout table, List<Policy> data) {
        for (Policy p : data) {
            TableRow row = new TableRow(getActivity().getApplicationContext());
            TextView tl = new TextView(getActivity().getApplicationContext());
            tl.setText(p.text);
           // tl.setLayoutParams(columnParams);
            tl.setTextColor(0xFF000000);
            TextView tm = new TextView(getActivity().getApplicationContext());
            tm.setText("(Cost: " + p.cost + ")");
           // tm.setLayoutParams(columnParams);
            tm.setTextColor(0xFF000000);
            Button b = new Button(getActivity().getApplicationContext());
            b.setId(p.ID);
            b.setText("GO");
            b.setOnClickListener(this);
           // b.setLayoutParams(columnParams);
            row.addView(tl);
            row.addView(tm);
            row.addView(b);

            table.addView(row);
        }
    }
    //Depending on which page is selected (domestic, military, etc), will show the policies
    //the user has selected to show with their costs and buttons to execute them
    private void getPolicies(String policyURL, Integer policyID, List<Integer> mPolicyIDs, List<String> mAllPolicies, Integer mTableID) {
        final Integer pID = policyID;
        final List<Integer> policyIDs = mPolicyIDs;
        final List<String> allPolicies = mAllPolicies;
        final Integer tableID = mTableID;

        http.get(policyURL, new HTTP.DataDownloadListener() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public void dataDownloadedSuccessfully(Object data) {
                        if (data.toString().contains("Login")) {
                            alert("You are not logged in or have timed out. Go back and log in again.");
                        } else {
                            List<Policy> policyObjs = new java.util.ArrayList<Policy>();
                            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                            //Read the enabled policies from preferences, convert from Set<> to List<>
                            java.util.Set<String> policies = prefs.getStringSet(SettingsActivity.policyPrefIDs.get(pID), new java.util.HashSet<String>());
                            List<String> policyList = Arrays.asList(policies.toArray(new String[policies.size()]));
                            Document doc = parse(data.toString());
                            Elements rows = doc.select(getString(R.string.policy_table_breadcrumb)).select("tr");
                            Log.d("dummy", policyList.toString());
                            for (Element row : rows) {
                                //If the policy on the webpage is one that is enabled in prefs
                                if (policyList.contains(row.child(0).text())) {
                                    //get the ID for this policy (see arrays.xml)
                                    Integer mID = policyIDs.get(allPolicies.indexOf(row.child(0).text()));
                                    policyObjs.add(new Policy(row.child(0).text(), row.child(2).text(), mID));
                                }
                            }
                            buildPolicyTable((TableLayout) rootView.findViewById(tableID), policyObjs);
                            ((MainActivity) getActivity()).updateBottomBar(parseBottomBar(doc));
                        }
                    }

                    @Override
                    public void dataDownloadFailed() {
                        // handler failure (e.g network not available etc.)
                    }
                }
        );
    }

    //Shamelessly hard-coded. The bottom bar is a table, the relevant values are in columns 2-6
    private static String parseBottomBar(Document doc) {
        String bbBreadcrumb = "div#bottombar";
        String result = "| ";
        Element row = doc.select(bbBreadcrumb).select("tr").first();
        String[] labels = new String[]{"", "", "Oil: ", "RM: ", "MG: ", "Weps: ", "Food: "};
        for(int i = 1; i < 7; i++) {
            Element cell = row.child(i);
            result = result + labels[i] + cell.select("a").first().text() + " | ";
        }
        return result;

    }
    //get the BLOC main/home page, parse out some selected stats (selected in arrays.xml)
    //TODO: allow adjustment of which stats are pulled in preferences
    private Map<String, String> parseNation(Document doc) {
        Map<String, String> result = new HashMap<String, String>();

        //Economics
        result.putAll(readBlocTable(doc.body().select(getString(R.string.econ_breadcrumb)), econLabels));
        //Military
        result.putAll(readBlocTable(doc.body().select(getString(R.string.mil_breadcrumb)), milLabels));

        return result;
    }
    //Reads the two-column table on nation page to pull the stats
    private Map<String, String> readBlocTable(Elements tableContainer, List<String> labels) {
        //All of the tables on the nation page have the important info in italicised text so we can grab it by selecting the first <i>
        //If rummy ever updates this it would be extremely painful
        Element leftCell;
        Element rightCell;
        String rightText;

        Map<String, String> result = new HashMap<String, String>();

        Elements rows = tableContainer.select("tbody > tr");
        for(Element e : rows) {
            leftCell = e.child(0);
            if(labels.contains(leftCell.text())) {
                rightCell = e.children().select("i").first();
                rightText = rightCell.html();
                if(rightText.contains("<img")) {
                    //then the cell has a dropdown info
                    rightText = rightCell.text() + " (" + rightCell.parent().nextElementSibling().text() + ")";
                }
                //strip out any <font> tags, etc.
                rightText = rightText.replaceAll("<.+>([^<]*)</.+>", "$1");
                result.put(leftCell.text(), rightText);
            }
        }
        return result;
    }

    //Uses JSoup to parse HTML text into object
    private Document parse(String html) {
        Long mark1 = System.currentTimeMillis();
        Document doc = Jsoup.parse(html, "blocgame.com");
        Long mark2 = System.currentTimeMillis();
        Log.d("dummy", "Parse full page: " + (mark2 - mark1));
        return doc;
    }

    //Pops up a message to user with an OK to dismiss
    private void alert(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //do nothing
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert);
        builder.create();
        builder.show();
    }

    private class Policy {
        public String text;
        public String cost;
        public Integer ID;
        public Policy(String text, String cost, Integer ID) {
            this.text = text;
            this.cost = cost;
            this.ID = ID;
        }
    }
}
