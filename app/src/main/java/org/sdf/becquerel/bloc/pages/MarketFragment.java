package org.sdf.becquerel.bloc.pages;
import android.app.Fragment;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by stephen on 22/01/16.
 */
public class MarketFragment extends BlocFragment implements android.text.TextWatcher {
    private List<Integer> marketToggleIDs = null;
    private List<Integer> marketButtonIDs = null;
    private HashMap<String, Integer> marketPriceList = null;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        TypedArray tmpArr;
        Integer[] newArr;
        Bundle bundle = getArguments();
        Integer fragment = bundle.getInt("ID");
        rootView = inflater.inflate(fragment, container, false);
        Button b2 = null;
        tmpArr = getActivity().getResources().obtainTypedArray(R.array.market_button_ids);
        newArr = new Integer[tmpArr.length()];
        for (int i = 0; i < tmpArr.length(); i++) {
            b2 = (Button) rootView.findViewById(tmpArr.getResourceId(i, 0));
            b2.setOnClickListener(this);
            newArr[i] = tmpArr.getResourceId(i, 0);
        }
        marketButtonIDs = Arrays.asList(newArr);
        tmpArr = getActivity().getResources().obtainTypedArray(R.array.market_toggle_ids);
        newArr = new Integer[tmpArr.length()];
        for (int i = 0; i < tmpArr.length(); i++) {
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
        return rootView;
    }

    public void onClick(View v) {
        Integer id = v.getId();
        if((marketToggleIDs != null) && marketToggleIDs.contains(id)) {
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
            //unfortunately MG is called "weapons" for sell/buy 1 and 5, but called "mg" for 20, so we need a special case
            if(baseURL.equals("http://blocgame.com/buyweapons")) {
                trades.add("http://blocgame.com/buymg100.php");
            }
            else if(baseURL.equals("http://blocgame.com/sellweapons")) {
                trades.add("http://blocgame.com/sellmg100.php");
            }
            else {
                trades.add(baseURL + "100.php");
            }
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
}