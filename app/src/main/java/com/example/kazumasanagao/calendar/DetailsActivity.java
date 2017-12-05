package com.example.kazumasanagao.calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by kazumasanagao on 7/19/15.
 */
public class DetailsActivity extends AppCompatActivity {
    private Activity me;
    private ListView listview;
    private List<ItemBean> list;
    private ListAdapter adapter;
    private DatabaseHelper dbhelper;
    private ArrayList<String> databases;
    private String database;
    private SQLiteDatabase db;
    private View mFooter;
    private Calendar oldestDay;
    private Integer maxdays;
    private SimpleDateFormat sdf_sys;
    private DateFormat sdf_usr;
    private Calendar cal;
    private Boolean first_display;
    private LinearLayout layout_ad;
    private AdView adView;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.details);

        me = this;
        maxdays = 20;
        Intent intent = getIntent();
        database = intent.getStringExtra("database");
        databases = new ArrayList<String>();
        listview = (ListView) this.findViewById(R.id.detailsview);
        dbhelper = new DatabaseHelper(me);
        db = dbhelper.getWritableDatabase();
        sdf_sys = new SimpleDateFormat("yyyy-MM-dd");
        sdf_usr = DateFormat.getDateInstance();
        cal = Calendar.getInstance();
        oldestDay = cal;
        list = new ArrayList<ItemBean>();
        first_display = true;

        adView = new AdView(me);
        adView.setAdUnitId("ca-app-pub-1100362701950968/3145709932");
        adView.setAdSize(AdSize.BANNER);
        layout_ad = (LinearLayout) findViewById(R.id.layout_ad2);
        layout_ad.addView(adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        addListData();

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                ItemBean item = (ItemBean) listview.getItemAtPosition(position);
                String date = item.getDate();
                String sys_date = "";
                Date d;
                try {
                    d = sdf_usr.parse(date);
                    sys_date = sdf_sys.format(d);
                } catch (Exception e) {}
                changeStatus(position, sys_date);
            }
        });

        listview.setOnScrollListener(new AbsListView.OnScrollListener() {
            private boolean flag;

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (flag && scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                    additionalReading();
                    flag = false;
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (totalItemCount == firstVisibleItem + visibleItemCount) {
                    flag = true;
                }
            }
        });
        adapter = new ListAdapter(getApplicationContext(), list);
        listview.addFooterView(getFooter());
        listview.setAdapter(adapter);
    }

    @Override
    public void onPause() {
        adView.pause();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        adView.resume();
    }

    @Override
    public void onDestroy() {
        adView.destroy();
        super.onDestroy();
    }

    private View getFooter() {
        if (mFooter == null) {
            mFooter = getLayoutInflater().inflate(R.layout.listview_footer, null);
        }
        return mFooter;
    }

    private void additionalReading() {
        addListData();
        adapter.notifyDataSetChanged();
    }

    public void addListData() {
        cal = oldestDay;
        // １回目の表示は＋１補正しないと昨日からの表示になる
        if (first_display) {
            cal.add(Calendar.DATE, 1);
            first_display = false;
        }
        for (int i = 0; i < maxdays; i++) {
            ItemBean item = new ItemBean();
            cal.add(Calendar.DATE, -1);
            oldestDay = cal;
            Date calf = cal.getTime();
            String date = sdf_sys.format(calf);
            String date_usr = sdf_usr.format(calf);
            item.setDate(date_usr);
            Cursor c = db.rawQuery("SELECT achi FROM '" + database + "' where date='" + date + "'", null);
            Integer status = -1;
            while (c.moveToNext()) {
                status = c.getInt(c.getColumnIndex("achi"));
            }
            c.close();
            if (status == 1) {
                item.setAchi(getString(R.string.achieved));
            } else if (status == 0) {
                item.setAchi(getString(R.string.notachieved));
            } else {
                item.setAchi("");
            }
            list.add(item);
        }
    }

    public void changeStatus(final int position, final String date) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        final Boolean isData;
        Cursor mCount = db.rawQuery("select count(*) from '" + database + "' where date='" + date + "'", null);
        mCount.moveToFirst();
        int count = mCount.getInt(0);
        mCount.close();
        if (count > 0) {
            isData = true;
        } else {
            isData = false;
        }

        String usr_date = "";
        Date d;
        try {
            d = sdf_sys.parse(date);
            usr_date = sdf_usr.format(d);
        } catch (Exception e) {}

        builder.setMessage(usr_date + "\n" + database + getString(R.string.didyou_achi));

        DialogInterface.OnClickListener okListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int button) {
                ItemBean data = list.get(position);

                if (isData) {
                    db.execSQL("UPDATE '" + database + "' SET achi = 1 WHERE date = '" + date + "'");
                    Integer counter = getCounter();
                    if (counter % 5 == 0 && counter != 0) {
                        openCelebrate(counter);
                    }
                } else {
                    ContentValues values = new ContentValues();
                    values.put("date", date);
                    values.put("achi", 1);
                    db.insert("'"+database+"'", null, values);
                    Integer counter = getCounter();
                    if (counter % 5 == 0 && counter != 0) {
                        openCelebrate(counter);
                    }
                }
                data.setAchi(getString(R.string.achieved));
                list.set(position, data);
                View view = getViewAtPosition(position, listview);
                listview.getAdapter().getView(position, view, listview);
            }
        };
        DialogInterface.OnClickListener ngListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int button) {
                ItemBean data = list.get(position);

                if (isData) {
                    db.execSQL("UPDATE '" + database + "' SET achi = 0 WHERE date = '" + date + "'");
                } else {
                    ContentValues values = new ContentValues();
                    values.put("date", date);
                    values.put("achi", 0);
                    db.insert("'"+database+"'", null, values);
                }
                data.setAchi(getString(R.string.notachieved));
                list.set(position, data);
                View view = getViewAtPosition(position, listview);
                listview.getAdapter().getView(position, view, listview);
            }
        };
        DialogInterface.OnClickListener nonListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int button) {
                ItemBean data = list.get(position);

                if (isData) {
                    db.execSQL("DELETE FROM '" + database + "' WHERE date = '" + date + "'");
                }
                data.setAchi("");
                list.set(position, data);
                View view = getViewAtPosition(position, listview);
                listview.getAdapter().getView(position, view, listview);
            }
        };
        builder.setPositiveButton(getString(R.string.yes), okListener);
        builder.setNegativeButton(getString(R.string.no), ngListener);
        builder.setNeutralButton(getString(R.string.later), nonListener);
        builder.show();
    }

    public Integer getCounter() {
        Cursor c = db.rawQuery("SELECT achi FROM '" + database + "' order by date desc", null);
        Integer total = c.getCount();
        c.moveToFirst();
        Integer counter = 0;
        for (int j = 0; j < total; j++) {
            if (c.getInt(c.getColumnIndex("achi")) == 1) {
                counter++;
                c.moveToNext();
            } else {
                break;
            }
        }
        c.close();
        return counter;
    }

    public void openCelebrate(final Integer numOfSuccess) {
        CelebrateDialogFragment dialogFragment = CelebrateDialogFragment.newInstance();
        Bundle args = new Bundle();
        args.putString("database", database);
        args.putString("numOfSuccess", numOfSuccess.toString());
        dialogFragment.setArguments(args);
        dialogFragment.show(this.getSupportFragmentManager(), "fragment_dialog");
    }

    public View getViewAtPosition(int pos, ListView listView) {
        final int firstListItemPosition = listView.getFirstVisiblePosition();
        final int lastListItemPosition = firstListItemPosition + listView.getChildCount() - 1;

        if (pos < firstListItemPosition || pos > lastListItemPosition) {
            return listView.getAdapter().getView(pos, null, listView);
        } else {
            final int childIndex = pos - firstListItemPosition;
            return listView.getChildAt(childIndex);
        }
    }

    public void deleteDB() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(database + getString(R.string.delete));
        DialogInterface.OnClickListener okListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int button) {
                try {

                    db.execSQL("DROP TABLE '" + database + "'");
                    getArray();
                    databases.remove(database);
                    saveArray(databases);
                    // 遷移元の画面に戻る
                    finish();
                } catch (Exception e) {}
            }
        };
        DialogInterface.OnClickListener ngListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int button) {}
        };
        builder.setPositiveButton(getString(R.string.yes), okListener);
        builder.setNegativeButton(getString(R.string.no), ngListener);
        builder.show();
    }

    public class ItemBean {
        private String date = "";
        private String achi = "";
        public void setDate(String date) {
            this.date = date;
        }
        public String getDate() {
            return date;
        }
        public void setAchi(String achi) {
            this.achi = achi;
        }
        public String getAchi() {
            return achi;
        }
    }

    class ListAdapter extends ArrayAdapter<ItemBean> {
        private LayoutInflater mInflater;
        private TextView mDate;
        private TextView mAchi;

        public ListAdapter(Context context, List<ItemBean> objects) {
            super(context, 0, objects);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.details_row, null);
            }
            final ItemBean item = this.getItem(position);
            if (item != null) {
                mDate = (TextView) convertView.findViewById(R.id.dateText);
                mDate.setText(item.getDate());
                mAchi = (TextView) convertView.findViewById(R.id.achiText);
                mAchi.setText(item.getAchi());
            }
            Integer backgroundDrawable;
            if (item.getAchi().equals(getString(R.string.achieved))) {
                backgroundDrawable = Color.parseColor("#00bfff"); // deepskyblue
            } else if (item.getAchi().equals(getString(R.string.notachieved))) {
                backgroundDrawable = Color.parseColor("#ff4500"); // oragered
            } else {
                backgroundDrawable = Color.TRANSPARENT;
            }
            convertView.setBackgroundColor(backgroundDrawable);
            return convertView;
        }
    }

    public void saveArray(ArrayList<String> arraylist) {
        JSONArray array = new JSONArray();
        for (int i = 0, length = arraylist.size(); i < length; i++) {
            try {
                array.put(i, arraylist.get(i));
            } catch (JSONException e) {}
        }

        SharedPreferences.Editor editor = getApplicationContext().getSharedPreferences("shared_preference", Context.MODE_PRIVATE).edit();
        editor.putString("databases", array.toString());
        editor.commit();
    }

    public void getArray() {
        Bundle bundle = new Bundle();
        Map<String, ?> prefKV = getApplicationContext().getSharedPreferences("shared_preference", Context.MODE_PRIVATE).getAll();
        Set<String> keys = prefKV.keySet();
        for(String key : keys){
            Object value = prefKV.get(key);
            if(value instanceof String){
                bundle.putString(key, (String) value);
            }else if(value instanceof Integer){
                bundle.putString(key, value.toString());
            }
        }
        String stringList = bundle.getString("databases", "there are no data yet.");
        ArrayList<String> arraylist = new ArrayList<String>();
        try {
            JSONArray array = new JSONArray(stringList);
            for(int i = 0, length = array.length(); i < length; i++){
                arraylist.add(array.optString(i));
            }
            databases = arraylist;
        } catch (JSONException e) {}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_details, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_delete) {
            deleteDB();
        }

        return super.onOptionsItemSelected(item);
    }
}


