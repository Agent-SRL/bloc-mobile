package org.sdf.becquerel.bloc.pages;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.sdf.becquerel.bloc.AdapterLinearLayout;
import org.sdf.becquerel.bloc.BlocFragment;

import org.sdf.becquerel.bloc.CommViewActivity;
import org.sdf.becquerel.bloc.HTTP;
import org.sdf.becquerel.bloc.MainActivity;
import org.sdf.becquerel.bloc.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Created by stephen on 22/01/16.
 */
public class CommsFragment extends BlocFragment {
    private List<Communique> commList = null;
    CommAdapter adapter = null;
    public void onClick(View v) {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.comms, container, false);
        Bundle bundle = getArguments();
        commList = new ArrayList<Communique>();
        adapter = new CommAdapter(getActivity().getApplicationContext(), R.layout.listview_items, commList);
        AdapterLinearLayout list = (AdapterLinearLayout) rootView.findViewById(R.id.comms_list);
        list.setAdapter(adapter);
        if (bundle.getInt("navID") == R.id.nav_commsout) {
            //get sent comms
            getComms(1, false);
        } else {
            //get received comms
            getComms(1, true);
        }
        return rootView;
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
                tt1.setSingleLine();
                LinearLayout ll1 = (LinearLayout) v.findViewById(R.id.comm_container);
                //TextView tt2 = (TextView) v.findViewById(R.id.categoryId);
                //TextView tt3 = (TextView) v.findViewById(R.id.description);

                if (tt1 != null) {
                    tt1.setText(c.player + ": " + c.text);
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
}
