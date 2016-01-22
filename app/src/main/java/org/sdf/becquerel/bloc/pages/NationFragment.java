package org.sdf.becquerel.bloc.pages;
import android.app.Fragment;
import android.content.SharedPreferences;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by stephen on 22/01/16.
 */
public class NationFragment extends BlocFragment{

    public void onClick(View v) {}
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.nation, container, false);
        getNationStats();
        return rootView;
    }

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
                            buildTable(stats);
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
        Map<String, String> result = new java.util.LinkedHashMap<String, String>();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        //Leader
        //Read the enabled policies from preferences, convert from Set<> to List<>
        java.util.Set<String> policies = prefs.getStringSet("leader_labels", new java.util.HashSet<String>());
        List<String> labels = Arrays.asList(policies.toArray(new String[policies.size()]));
        //Grab the values for the selected labels
        if(!labels.isEmpty()) {
            result.put("Leader", "BREAK");
            result.putAll(readBlocTable(doc.body().select(getString(R.string.leader_breadcrumb)), labels));
        }
        //Gubmint
        policies = prefs.getStringSet("gov_labels", new java.util.HashSet<String>());
        labels = Arrays.asList(policies.toArray(new String[policies.size()]));
        if(!labels.isEmpty()) {
            result.put("Government", "BREAK");
            result.putAll(readBlocTable(doc.body().select(getString(R.string.gov_breadcrumb)), labels));
        }
        //Economics
        policies = prefs.getStringSet("econ_labels", new java.util.HashSet<String>());
        labels = Arrays.asList(policies.toArray(new String[policies.size()]));
        if(!labels.isEmpty()) {
            result.put("Economy", "BREAK");
            result.putAll(readBlocTable(doc.body().select(getString(R.string.econ_breadcrumb)), labels));
        }
        //Foreign
        policies = prefs.getStringSet("for_labels", new java.util.HashSet<String>());
        labels = Arrays.asList(policies.toArray(new String[policies.size()]));
        if(!labels.isEmpty()) {
            result.put("Foreign", "BREAK");
            result.putAll(readBlocTable(doc.body().select(getString(R.string.for_breadcrumb)), labels));
        }
        //Military
        policies = prefs.getStringSet("mil_labels", new java.util.HashSet<String>());
        labels = Arrays.asList(policies.toArray(new String[policies.size()]));
        if(!labels.isEmpty()) {
            result.put("Military", "BREAK");
            result.putAll(readBlocTable(doc.body().select(getString(R.string.mil_breadcrumb)), labels));
        }

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
                rightText = rightCell.text();
                if(e.child(1).child(0).attr("class").equals("dropdown")) {
                    //then the cell has a dropdown info
                    rightText += " (" + parseDropdownText(e.child(1).child(0).select("ul").html().toString()) + ")";
                }
                //strip out any <font> tags, etc.
                rightText = rightText.replaceAll("<.+>([^<]*)</.+>", "$1");
                rightText = rightText.replace("<br>", "");
                result.put(leftCell.text(), rightText);
            }
        }
        return result;
    }
    private static String parseDropdownText(String in) {
        //inexplicably the dropdowns are always <ul>'s but sometimes have <li>'s, sometime <font>'s and sometimes just unformatted text. Thanks rummy.
        //So instead we parse them with special regexes for each. Will have to update this code whenever Rummy changes the layout.
        Pattern p;
        Matcher m;
        if(in.contains("Land Use")) {
            Double[] d = new Double[4];
            java.text.DecimalFormat df = new java.text.DecimalFormat("#.##");
            p = Pattern.compile("\\(.*?[0-9,]+ km.*?\\)|([0-9,]+) km", Pattern.DOTALL);
            m = p.matcher(in);
            int i = 0;
            while(m.find()) {
                if(m.group(1) != null) {
                    System.out.println(m.group(1));
                    d[i] = Double.valueOf(m.group(1).replace(",", ""));
                    i++;
                }
            }
            Double total = d[0] + d[1] + d[2] + d[3];

            return String.format("Urban:%s \n Oil:%s \n Mines:%s \n Farms:%s", df.format(((Double) (d[0]/total)).doubleValue() * 100) + "%",
                    df.format(((Double) (d[1]/total)).doubleValue() * 100) + "%",
                    df.format(((Double) (d[2]/total)).doubleValue() * 100) + "%",
                    df.format(((Double) (d[3]/total)).doubleValue() * 100) + "%");
        }
        else if(in.contains("every ten minutes")) {
            p = Pattern.compile("\\$([0-9]+)k");
            m = p.matcher(in);
            if(m.find()) {
                Integer hourly = 6 * Integer.valueOf(m.group(1).replace(",",""));
                return "+$" + hourly + "k/hr";
            }
        }
        else if(in.contains("Next month") && in.contains("$")) {
            p = Pattern.compile("([\\+|-]\\$[0-9]+) ");
            m = p.matcher(in);
            Integer change = 0;
            while(m.find()) {
                change += Integer.valueOf(m.group(1).replace("$", ""));
            }
            String sign = "+";
            if(change < 0)
                sign = "-";
            return sign + "$" + ((Integer) Math.abs(change)).toString() + "M/mo";
        }
        else if(in.contains("Next month")) {
            p = Pattern.compile("([\\+|-][0-9]+ )");
            m = p.matcher(in);
            Integer change = 0;
            while(m.find()) {
                change += Integer.valueOf(m.group(1).trim());
            }
            String sign = "+";
            if(change < 0)
                sign = "-";
            return sign + ((Integer) Math.abs(change)).toString() + "/mo";
        }
        //else
        return in;
    }
    //Builds a two-column table for the home/nation page
    private void buildTable(Map<String, String> data) {
        Map<String, Integer> tableIDs = new HashMap<String, Integer>();
        tableIDs.put("Leader", R.id.nation_table_leader);
        tableIDs.put("Government", R.id.nation_table_gov);
        tableIDs.put("Economy", R.id.nation_table_econ);
        tableIDs.put("Foreign", R.id.nation_table_for);
        tableIDs.put("Military", R.id.nation_table_mil);
        int counter = -1;
        List<TableLayout> tables = new ArrayList<TableLayout>();
        for (Map.Entry<String, String> s : data.entrySet()) {
            if(s.getValue().equals("BREAK")) {
                counter++;
                tables.add(counter, (TableLayout) rootView.findViewById(tableIDs.get(s.getKey())));
                tables.get(counter).setVisibility(View.VISIBLE);
                Log.d("dummy", "making table: " + s.getKey() + " - " + tableIDs.get(s.getKey()).toString());
            } else {
                TableRow row = new TableRow(getActivity().getApplicationContext());
                TextView tl = new TextView(getActivity().getApplicationContext());
                tl.setText(s.getKey());
                tl.setTextColor(0xFF000000);
                TextView tr = new TextView(getActivity().getApplicationContext());
                tr.setText(s.getValue());
                tr.setTextColor(0xFF000000);
                row.addView(tl);
                row.addView(tr);
                tables.get(counter).addView(row);
                Log.d("dummy", "adding row: " + s.getKey() + " : " + s.getValue());
            }
        }
        //getActivity().setContentView(R.layout.activity_main);
    }

}
