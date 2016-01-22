package org.sdf.becquerel.bloc.pages;
import android.app.Fragment;
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

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.sdf.becquerel.bloc.BlocFragment;
import org.sdf.becquerel.bloc.HTTP;
import org.sdf.becquerel.bloc.MainActivity;
import org.sdf.becquerel.bloc.R;
import org.sdf.becquerel.bloc.SettingsActivity;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by stephen on 22/01/16.
 */
public class PolicyFragment extends BlocFragment {
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
        Bundle bundle = getArguments();
        Integer fragment = bundle.getInt("ID");
        rootView = inflater.inflate(fragment, container, false);
        //Page fragments get created with an identifying ID
        switch (bundle.getInt("ID")) {
            case R.layout.domestic:
                //Convert int[] to Integer[]
                tmpArr = getActivity().getResources().obtainTypedArray(R.array.domestic_policy_id);
                newArr = new Integer[tmpArr.length()];
                for (int i = 0; i < tmpArr.length(); i++) {
                    newArr[i] = tmpArr.getResourceId(i, 0);
                }
                tmpArr.recycle();
                //Grab resource values here when we know the Activity will be initialized
                domesticPolicyIDs = Arrays.asList(newArr);
                domesticPolicyURLs = getActivity().getResources().getStringArray(R.array.domestic_policy_url);
                allDomesticPolicies = Arrays.asList(getActivity().getResources().getStringArray(R.array.all_domestic_policies));
                getPolicies(getString(R.string.url_domestic), R.layout.domestic, domesticPolicyIDs, allDomesticPolicies, R.id.domestic_table);
                break;
            case R.layout.military:
                tmpArr = getActivity().getResources().obtainTypedArray(R.array.military_policy_id);
                newArr = new Integer[tmpArr.length()];
                for (int i = 0; i < tmpArr.length(); i++) {
                    newArr[i] = tmpArr.getResourceId(i, 0);
                }
                tmpArr.recycle();
                militaryPolicyIDs = Arrays.asList(newArr);
                militaryPolicyURLs = getActivity().getResources().getStringArray(R.array.military_policy_url);
                allMilitaryPolicies = Arrays.asList(getActivity().getResources().getStringArray(R.array.all_military_policies));
                getPolicies(getString(R.string.url_military), R.layout.military, militaryPolicyIDs, allMilitaryPolicies, R.id.military_table);
                break;
        }
        return rootView;
    }

    @Override
    public void onClick(View v) {
        Integer id = v.getId();
        if ((domesticPolicyIDs != null) && domesticPolicyIDs.contains(id)) {
            String url = domesticPolicyURLs[domesticPolicyIDs.indexOf(id)];
            doPolicy(url);
        } else if ((militaryPolicyIDs != null) && militaryPolicyIDs.contains(id)) {
            String url = militaryPolicyURLs[militaryPolicyIDs.indexOf(id)];
            doPolicy(url);
        }
    }

    /** Policy Fragment code **/
    //Simple wrapper to GET a policy page and do an Alert box with the result
    private void doPolicy(String url) {
        http.get(url, new HTTP.DataDownloadListener() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public void dataDownloadedSuccessfully(Object data) {
                        if (data.toString().contains("Login")) {
                            alert("You are not logged in or have timed out. Go back and log in again.", getActivity());
                        } else {
                            //policy php pages consist of one line of text containing the result
                            String reply = parse(data.toString()).select("h4").first().text();
                            alert(reply, getActivity());
                        }
                    }

                    @Override
                    public void dataDownloadFailed() {
                        // handler failure (e.g network not available etc.)
                    }
                }
        );
    }

    //Builds a three-column table with policy title, cost, and button to do the policy
    private void buildPolicyTable(TableLayout table, List<Policy> data) {
        for (Policy p : data) {
            TableRow row = new TableRow(getActivity().getApplicationContext());
            TextView tl = new TextView(getActivity().getApplicationContext());
            tl.setText(p.text);
            tl.setTextColor(0xFF000000);
            TextView tm = new TextView(getActivity().getApplicationContext());
            tm.setText("(Cost: " + p.cost + ")");
            tm.setTextColor(0xFF000000);
            Button b = new Button(getActivity().getApplicationContext());
            b.setId(p.ID);
            b.setText("GO");
            b.setOnClickListener(this);
            row.addView(tl, new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));
            row.addView(tm, new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));
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
                            alert("You are not logged in or have timed out. Go back and log in again.", getActivity());
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
