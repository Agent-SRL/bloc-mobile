package org.sdf.becquerel.bloc;

import org.sdf.becquerel.bloc.pages.*;
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

public abstract class BlocFragment extends Fragment implements View.OnClickListener {
    public BlocFragment() {}
    public BlocFragment(Integer fragmentID) {
        Log.d("dummy", "fragment = " + fragmentID.toString());
    }
    protected View rootView;
    protected HTTP http = new HTTP();

    //See strings.xml
    private List<String> econLabels = null;
    private List<String> milLabels = null;

   // public void onClick(View v) {}

    public static BlocFragment fragmentChooser(Integer fragmentID, Integer navID) {
        Bundle args;
        switch(fragmentID) {
            case R.layout.login:
                return new LoginFragment();
            case R.layout.nation:
                return new NationFragment();
            case R.layout.domestic:
                PolicyFragment df = new PolicyFragment();
                args = new Bundle();
                args.putInt("ID", R.layout.domestic);
                df.setArguments(args);
                return df;
            case R.layout.military:
                PolicyFragment mf = new PolicyFragment();
                args = new Bundle();
                args.putInt("ID", R.layout.military);
                mf.setArguments(args);
                return mf;
            case R.layout.comms:
                args = new Bundle();
                args.putInt("navID", navID);
                CommsFragment cf = new CommsFragment();
                cf.setArguments(args);
                return cf;
            case R.layout.market:
                return new MarketFragment();
            case R.layout.declarations:
                break;

            default:
                break;
        }
        return null;
    }
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
            case R.layout.declarations:
               break;

            default:
                //
                break;
        }

        return rootView;
    }


/** Common methods **/
    //Shamelessly hard-coded. The bottom bar is a table, the relevant values are in columns 2-6
    protected static String parseBottomBar(Document doc) {
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
    protected Document parse(String html) {
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
