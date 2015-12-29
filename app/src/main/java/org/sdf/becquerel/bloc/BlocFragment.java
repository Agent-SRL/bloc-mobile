package org.sdf.becquerel.bloc;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Created by stephen on 19/12/15.
 */

public class BlocFragment extends Fragment implements View.OnClickListener, android.text.TextWatcher {
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

    private List<Communique> commList = null;
    CommAdapter adapter = null;

    private List<Integer> marketToggleIDs = null;
    private List<Integer> marketButtonIDs = null;
    private HashMap<String, Integer> marketPriceList = null;

/** Control logic **/
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
            case R.layout.comms:
                commList = new ArrayList<Communique>();
                adapter = new CommAdapter(getActivity().getApplicationContext(), R.layout.listview_items, commList);
                AdapterLinearLayout list = (AdapterLinearLayout) rootView.findViewById(R.id.comms_list);
                list.setAdapter(adapter);
                if(bundle.getInt("navID") == R.id.nav_commsout) {
                    //get sent comms
                    getComms(1, false);
                } else {
                    //get received comms
                    getComms(1, true);
                }
                break;
            case R.layout.market:
                Button b2 = null;
                tmpArr = getActivity().getResources().obtainTypedArray(R.array.market_button_ids);
                newArr = new Integer[tmpArr.length()];
                for(int i = 0; i < tmpArr.length(); i++) {
                    b2 = (Button) rootView.findViewById(tmpArr.getResourceId(i, 0));
                    b2.setOnClickListener(this);
                    newArr[i] = tmpArr.getResourceId(i, 0);
                }
                marketButtonIDs = Arrays.asList(newArr);
                tmpArr = getActivity().getResources().obtainTypedArray(R.array.market_toggle_ids);
                newArr = new Integer[tmpArr.length()];
                for(int i = 0; i < tmpArr.length(); i++) {
                    b2 = (Button) rootView.findViewById(tmpArr.getResourceId(i, 0));
                    b2.setOnClickListener(this);
                    newArr[i] = tmpArr.getResourceId(i, 0);
                }
                marketToggleIDs = Arrays.asList(newArr);
                EditText e = (EditText) rootView.findViewById(R.id.custom_amount);
                e.addTextChangedListener(this);
                marketPriceList = new HashMap<String, Integer>();
                //initialize with RM prices by default
                b2.callOnClick();
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
        else if( (domesticPolicyIDs != null) && domesticPolicyIDs.contains(id)) {
            String url = domesticPolicyURLs[domesticPolicyIDs.indexOf(id)];
            doPolicy(url);
        }
        else if( (militaryPolicyIDs != null) && militaryPolicyIDs.contains(id)) {
            String url = militaryPolicyURLs[militaryPolicyIDs.indexOf(id)];
            doPolicy(url);
        }
        else if((marketToggleIDs != null) && marketToggleIDs.contains(id)) {
            ToggleButton b;
            for(Integer i : marketToggleIDs) {
                b = (ToggleButton) rootView.findViewById(i);
                if(i.equals(id)) {
                    b.setChecked(true);
                } else {
                    b.setChecked(false);
                }
            }
            setPrices(id);
        }
        else if((marketButtonIDs != null) && marketButtonIDs.contains(id)) {
            Integer selectedID = null;
            for(Integer i : marketToggleIDs) {
                ToggleButton b = (ToggleButton) rootView.findViewById(i);
                if(b.isChecked()) {
                    selectedID = i;
                }
            }
            String resource = null;
            switch(selectedID) {
                case R.id.market_food:
                    resource = getString(R.string.market_food_name);
                    break;
                case R.id.market_mg:
                    resource = getString(R.string.market_mg_name);
                    break;
                case R.id.market_oil:
                    resource = getString(R.string.market_oil_name);
                    break;
                case R.id.market_rm:
                    resource = getString(R.string.market_rm_name);
                    break;
            }
            String action = null;
            Integer count = 0;
            switch(id) {
                case R.id.buy_1:
                    action = "buy";
                    count = 1;
                    break;
                case R.id.buy_5:
                    action = "buy";
                    count = 5;
                    break;
                case R.id.buy_custom:
                    action = "buy";
                    count = Integer.valueOf(((TextView) rootView.findViewById(R.id.custom_amount)).getText().toString());
                    break;
                case R.id.sell_1:
                    action = "sell";
                    count = 1;
                    break;
                case R.id.sell_5:
                    action = "sell";
                    count = 5;
                    break;
                case R.id.sell_custom:
                    action = "sell";
                    count = Integer.valueOf(((TextView) rootView.findViewById(R.id.custom_amount)).getText().toString());
                    break;
            }
            String baseURL = getString(R.string.url_policies_base) + action + resource;
            doTrade(baseURL, count);
            //now refresh prices, bottom bar, etc:
            for(Integer i : marketToggleIDs) {
                ToggleButton b = (ToggleButton) rootView.findViewById(i);
                if(b.isChecked()) {
                    b.callOnClick();
                }
            }
        }
    }

/** Market page Fragment code **/

    //Text change listener for "custom amount" box
    public void afterTextChanged(Editable s) {
        int eID = getActivity().getCurrentFocus().getId();
        if(eID > 0 && eID == R.id.custom_amount) {
            //exiting the box, update buttons
            ((Button) rootView.findViewById(R.id.buy_custom)).setText("Buy " + s.toString());
            ((Button) rootView.findViewById(R.id.sell_custom)).setText("Sell " + s.toString());
        }
    }
    public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
    public void onTextChanged(CharSequence s, int start, int before, int count) { }

    //Given an arbitrary number to buy, break it down into 20s, 5s, and 1s
    private void doTrade(String baseURL, Integer count) {
        double c = (double) count.intValue();
        int twenties, fives, ones;
        twenties = (int) Math.floor(c / 20.0);
        c = c % 20.0;
        fives = (int) Math.floor(c / 5.0);
        c = c % 5.0;
        ones = (int) c;
        List<String> trades = new ArrayList<String>();
        for(int i = 0; i < twenties; i++) {
            //e.g. baseURL = ./buyoil --> buyoil100.php
            //buy*100.php actually buys 20 units because Rumcode
            trades.add(baseURL + "100.php");
        }
        for(int i = 0; i < fives; i++) {
            trades.add(baseURL + "5.php");
        }
        for(int i = 0; i < ones; i++) {
            trades.add(baseURL + ".php");
        }
        for(String url : trades) {
            http.get(url, new HTTP.DataDownloadListener() {
                public void dataDownloadedSuccessfully(Object data) {
                    if (data.toString().contains("Login")) {
                        alert("You are not logged in or have timed out. Go back and log in again.", getActivity());
                    } else {
                        //Document doc = parse(data.toString());
                        //((MainActivity) getActivity()).updateBottomBar(parseBottomBar(doc));
                    }
                }
                @Override
                public void dataDownloadFailed() {
                    // handler failure (e.g network not available etc.)
                }
            });
        }
    }

    private void setPrices(Integer id) {
        final Integer resourceID = id;
        http.get(getString(R.string.url_market), new HTTP.DataDownloadListener() {
            public void dataDownloadedSuccessfully(Object data) {
                if (data.toString().contains("Login")) {
                    alert("You are not logged in or have timed out. Go back and log in again.", getActivity());
                } else {
                    Document doc = parse(data.toString());
                    Elements rows = doc.select(getString(R.string.market_table_breadcrumb)).select("tr");
                    Pattern p = Pattern.compile("\\$([0-9]+)k");
                    Matcher m;
                    Integer price;
                    for(Element t : rows) {
                        if(t.children().size() == 5) {
                            //menu bar
                            continue;
                        }
                        if(t.child(0).text().equals("Action")) {
                            //header row
                            continue;
                        }
                        m = p.matcher(t.child(2).text());
                        if(m.find()) {
                            marketPriceList.put(t.child(0).text(), Integer.valueOf(m.group(1)));
                        }
                    }
                    Integer selectedID = null;
                    for(Integer i : marketToggleIDs) {
                        ToggleButton b = (ToggleButton) rootView.findViewById(i);
                        if(b.isChecked()) {
                            selectedID = i;
                        }
                    }
                    String buyKey, sellKey;
                    switch(selectedID) {
                        case R.id.market_food:
                            buyKey = getString(R.string.market_buy_food);
                            sellKey = getString(R.string.market_sell_food);
                            break;
                        case R.id.market_mg:
                            buyKey = getString(R.string.market_buy_mg);
                            sellKey = getString(R.string.market_sell_mg);
                            break;
                        case R.id.market_oil:
                            buyKey = getString(R.string.market_buy_oil);
                            sellKey = getString(R.string.market_sell_oil);
                            break;
                        case R.id.market_rm:
                            buyKey = getString(R.string.market_buy_rm);
                            sellKey = getString(R.string.market_sell_rm);
                            break;
                        default:
                            //RM by default - see onCreateView()
                            buyKey = getString(R.string.market_buy_rm);
                            sellKey = getString(R.string.market_sell_rm);
                            break;
                    }
                    ( (TextView) rootView.findViewById(R.id.buy_price)).setText("Buy price: " + marketPriceList.get(buyKey).toString() + "k");
                    ( (TextView) rootView.findViewById(R.id.sell_price)).setText("Sell price: " + marketPriceList.get(sellKey).toString() + "k");
                    ((MainActivity) getActivity()).updateBottomBar(parseBottomBar(doc));
                }
            }
            @Override
            public void dataDownloadFailed() {
                // handler failure (e.g network not available etc.)
            }
        });
    }

/** Communiques Fragment code **/
//TODO: add a way to send comms to any user
    //Get sent or received comms
    private void getComms(Integer page, Boolean in) {
        final Integer pageNo = page;
        final Boolean inbound = in;
        String url;
        if(inbound) {
            url = getString(R.string.url_commsin);
        } else {
            url = getString(R.string.url_commsout);
        }
        http.get(url + page.toString(), new HTTP.DataDownloadListener() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public void dataDownloadedSuccessfully(Object data) {
                        String player;
                        String playerID;
                        Pattern p = Pattern.compile("stats\\.php\\?id=([0-9]+)");
                        Matcher m;
                        String msg;
                        String dateStr;
                        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.ENGLISH);
                        format.setTimeZone(TimeZone.getTimeZone("UTC"));
                        Date date;
                        String deleteID;

                        if (data.toString().contains("Login")) {
                            alert("You are not logged in or have timed out. Go back and log in again.", getActivity());
                        } else {
                            Document doc = parse(data.toString());
                            Elements tables = doc.select(getString(R.string.comms_breadcrumb));
                            if(!tables.isEmpty()) {
                                for (Element t : tables) {
                                    Element table = t.select("tbody").first();
                                    Log.d("dummy", Integer.valueOf(table.children().size()).toString());
                                    switch(table.children().size()) {
                                        case 3: //sent communique
                                            player = table.child(0).select("a").first().text();
                                            Log.d("dummy", "input: " + table.child(0).select("a").first().attr("href"));
                                            Log.d("dummy", p.pattern());
                                            m = p.matcher(table.child(0).select("a").first().attr("href"));
                                            if(m.matches()) {
                                                playerID = m.group(1);
                                            } else {
                                                continue;
                                            }
                                            msg = table.child(1).child(0).text();
                                            dateStr = table.child(2).child(0).text();
                                            dateStr = dateStr.replace("sent ", "");
                                            try {
                                                date = format.parse(dateStr);
                                                Log.d("dummy", "comm: " + player + " | " + msg + " | " + date.toString());
                                                adapter.add(new Communique(msg, player, playerID, date, Communique.OUTBOUND, ""));
                                                //reorderComms();
                                            } catch (Exception e) {
                                                Log.d("dummy", e.getMessage());
                                            }
                                            break;
                                        case 4: //received communique
                                            player = table.child(0).select("a").first().text();
                                            m = p.matcher(table.child(0).select("a").first().attr("href"));
                                            if(m.matches()) {
                                                playerID = m.group(1);
                                            } else {
                                                continue;
                                            }
                                            msg = table.child(1).child(0).text();
                                            dateStr = table.child(3).child(0).text();
                                            dateStr = dateStr.replace("sent ", "");
                                            deleteID = table.child(2).child(1).select("input").first().attr("value");
                                            try {
                                                date = format.parse(dateStr);
                                                Log.d("dummy", "comm: " + player + " | " + msg + " | " + date.toString());
                                                adapter.add(new Communique(msg, player, playerID, date, Communique.INBOUND, deleteID));
                                                //reorderComms();
                                            } catch (Exception e) {
                                                Log.d("dummy", e.getMessage());
                                            }
                                            break;
                                    }

                                }
                                getComms(pageNo+1, inbound);
                            } else {
                                //reorderComms();
                            }
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
    //Adapter to load comms into a ListView (actually a modified LinearLayout)
    public class CommAdapter extends ArrayAdapter<Communique> implements android.view.View.OnClickListener {

        public CommAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
        }

        public CommAdapter(Context context, int resource, List<Communique> items) {
            super(context, resource, items);
        }

        public void onClick(View v) {
            Log.d("dummy", v.toString());
            Communique c = getItem((int) v.findViewById(R.id.comm_label).getTag());
            Log.d("dummy", c.text);
            Intent myIntent = new Intent(getActivity(), CommViewActivity.class);
            HashMap<String, String> map = new HashMap<String, String>();
            map.put("message", c.text);
            map.put("player", c.player);
            map.put("playerID", c.playerID);
            map.put("date", c.date.toString());
            myIntent.putExtra("comm_map", map);
            getActivity().startActivity(myIntent);
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View v = convertView;

            if (v == null) {
                LayoutInflater vi;
                vi = LayoutInflater.from(getContext());
                v = vi.inflate(R.layout.listview_items, null);
            }

            Communique c = getItem(position);

            if (c != null) {
                TextView tt1 = (TextView) v.findViewById(R.id.comm_label);
                LinearLayout ll1 = (LinearLayout) v.findViewById(R.id.comm_container);
                //TextView tt2 = (TextView) v.findViewById(R.id.categoryId);
                //TextView tt3 = (TextView) v.findViewById(R.id.description);

                if (tt1 != null) {
                    tt1.setText(c.player);
                    tt1.setTag(Integer.valueOf(position));
                }
                if(ll1 != null) {
                    ll1.setOnClickListener(this);
                }

                /*if (tt2 != null) {
                    tt2.setText(p.getCategory().getId());
                }

                if (tt3 != null) {
                    tt3.setText(p.getDescription());
                }*/
            }

            return v;
        }

    }

    private class Communique implements java.lang.Comparable, java.util.Comparator {
        public String text;
        public String player;
        public String playerID;
        public Date date;
        public int type;
        public String deleteID = null;

        public static final int INBOUND = 0;
        public static final int OUTBOUND = 1;

        public Communique(String text, String player, String playerID, Date date, int type, String deleteID) {
            this.text = text;
            this.player = player;
            this.playerID = playerID;
            this.date = date;
            this.type = type;
            this.deleteID = deleteID;
        }
        @Override
        public String toString() {
            return this.player;
        }
        @Override
        public boolean equals(Object o) {
            Communique c = (Communique) o;
            return c.text.equals(this.text);
        }
        @Override
        public int compareTo(Object o) {
            Communique c = (Communique) o;
            if(this.date.before(c.date)) {
                return -1;
            }
            else if(this.date.equals(c.date)) {
                return 0;
            } else {
                return 1;
            }
        }
        @Override
        public int compare(Object o1, Object o2) {
            Communique c1 = (Communique) o1;
            Communique c2 = (Communique) o2;
            if(c1.date.before(c2.date)) {
                return -1;
            }
            else if(c1.date.equals(c2.date)) {
                return 0;
            } else {
                return 1;
            }
        }

    }

/** Login Fragment code **/
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

/** Nation Fragment code **/
//TODO: add a way to view other players' nation pages
    //Parse home/nation page to get stats
    private void getNationStats() {
        http.get(getString(R.string.url_nation), new HTTP.DataDownloadListener() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public void dataDownloadedSuccessfully(Object data) {
                        if (data.toString().contains("Login")) {
                            alert("You are not logged in or have timed out. Go back and log in again.", getActivity());
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
    //get the BLOC main/home page, parse out some selected stats (selected in arrays.xml)
    private Map<String, String> parseNation(Document doc) {
        Map<String, String> result = new HashMap<String, String>();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        //Leader
        //Read the enabled policies from preferences, convert from Set<> to List<>
        java.util.Set<String> policies = prefs.getStringSet("leader_labels", new java.util.HashSet<String>());
        List<String> labels = Arrays.asList(policies.toArray(new String[policies.size()]));
        //Grab the values for the selected labels
        result.putAll(readBlocTable(doc.body().select(getString(R.string.leader_breadcrumb)), labels));
        //Gubmint
        policies = prefs.getStringSet("gov_labels", new java.util.HashSet<String>());
        labels = Arrays.asList(policies.toArray(new String[policies.size()]));
        result.putAll(readBlocTable(doc.body().select(getString(R.string.gov_breadcrumb)), labels));
        //Economics
        policies = prefs.getStringSet("econ_labels", new java.util.HashSet<String>());
        labels = Arrays.asList(policies.toArray(new String[policies.size()]));
        result.putAll(readBlocTable(doc.body().select(getString(R.string.econ_breadcrumb)), labels));
        //Foreign
        policies = prefs.getStringSet("for_labels", new java.util.HashSet<String>());
        labels = Arrays.asList(policies.toArray(new String[policies.size()]));
        result.putAll(readBlocTable(doc.body().select(getString(R.string.for_breadcrumb)), labels));
        //Military
        policies = prefs.getStringSet("mil_labels", new java.util.HashSet<String>());
        labels = Arrays.asList(policies.toArray(new String[policies.size()]));
        result.putAll(readBlocTable(doc.body().select(getString(R.string.mil_breadcrumb)), labels));

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

/** Common methods **/
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

    //Uses JSoup to parse HTML text into object
    private Document parse(String html) {
        Long mark1 = System.currentTimeMillis();
        Document doc = Jsoup.parse(html, "blocgame.com");
        Long mark2 = System.currentTimeMillis();
        Log.d("dummy", "Parse full page: " + (mark2 - mark1));
        return doc;
    }

    //Pops up a message to user with an OK to dismiss
    public static void alert(String message, Activity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
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

}
