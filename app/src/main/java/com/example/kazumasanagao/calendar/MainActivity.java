package com.example.kazumasanagao.calendar;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.InputFilter;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class MainActivity extends AppCompatActivity {
    private AppCompatActivity me;
    private ListView listview;
    private TextView addlist;
    private List<ItemBean> list;
    private ListAdapter adapter;
    private SQLiteDatabase db;
    private DatabaseHelper dbhelper;
    private ArrayList<String> databases;
    private LinearLayout layout_ad;
    private AdView adView;
    private View mFooter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        me = this;
        dbhelper = new DatabaseHelper(me);
        db = dbhelper.getWritableDatabase();
        databases = new ArrayList<String>();
        getArray();
        list = new ArrayList<ItemBean>();
        listview = (ListView)this.findViewById(R.id.listview);
        setList();

        adView = new AdView(me);
        adView.setAdUnitId("ca-app-pub-1100362701950968/3145709932");
        adView.setAdSize(AdSize.BANNER);
        layout_ad = (LinearLayout) findViewById(R.id.layout_ad);
        layout_ad.addView(adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                moveToDetails(position);
            }
        });

        listview.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                AlertDialog.Builder builder = new AlertDialog.Builder(me);
                builder.setTitle(getString(R.string.move));
                builder.setItems(new String[]{getString(R.string.totop), getString(R.string.up), getString(R.string.down), getString(R.string.tobottom)}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int n) {
                        if (n == 0) {
                            String rm = databases.remove(position);
                            databases.add(0, rm);
                            saveArray(databases);
                            reload();
                        }
                        if (n == 1) {
                            if (position > 0) {
                                String rm = databases.remove(position);
                                databases.add(position - 1, rm);
                                saveArray(databases);
                                reload();
                            }
                        }
                        if (n == 2) {
                            if (position < databases.size() - 1) {
                                String rm = databases.remove(position);
                                databases.add(position + 1, rm);
                                saveArray(databases);
                                reload();
                            }
                        }
                        if (n == 3) {
                            String rm = databases.remove(position);
                            databases.add(rm);
                            saveArray(databases);
                            reload();
                        }
                    }
                });
                builder.create();
                builder.show();
                return true;
            }
        });

        adapter = new ListAdapter(getApplicationContext(),list);
        listview.addFooterView(getFooter());
        listview.setAdapter(adapter);


        addlist = (TextView)this.findViewById(R.id.addlist);
        addlist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText editView = new EditText(MainActivity.this);
                InputFilter[] _inputFilter = new InputFilter[1];
                _inputFilter[0] = new InputFilter.LengthFilter(30);
                editView.setFilters(_inputFilter);
                editView.setInputType(InputType.TYPE_CLASS_TEXT);
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(getString(R.string.inputgoal))
                        .setView(editView)
                        .setPositiveButton(getString(R.string.save), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                String TABLE_NAME = '"'+editView.getText().toString()+'"';
                                String table_name = editView.getText().toString();
                                Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='" + table_name + "';", null);
                                if (!(c.moveToFirst())) {
                                    String query = "create table " + TABLE_NAME + "(" +
                                            "date TEXT PRIMARY KEY," +
                                            "achi INTEGER);";
                                    try {
                                        db.execSQL(query);
                                        databases.add(0, table_name);
                                        saveArray(databases);
                                        reload();
                                    } catch (Exception e) {
                                        Toast.makeText(me,
                                                getString(R.string.invalid),
                                                Toast.LENGTH_LONG).show();
                                    }
                                } else {
                                    Toast.makeText(me,
                                            getString(R.string.samegoal),
                                            Toast.LENGTH_LONG).show();
                                }
                            }
                        })
                        .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        })
                        .show();
            }
        });
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

    @Override
    protected void onRestart() {
        super.onRestart();
        reload();
    }

    public void reload() {
        getArray(); //DB削除のとき、この文が無いとsetList内で「databaseが無い」というExceptionが出る
        list.clear();
        setList();
        adapter.notifyDataSetChanged();
        getArray(); //setList内でスクラップを掃除した後の情報を取得するため
    }

    private View getFooter() {
        if (mFooter == null) {
            mFooter = getLayoutInflater().inflate(R.layout.main_footer, null);
        }
        return mFooter;
    }

    public void setList() {
        Integer n = databases.size();
        ArrayList<String> trash = new ArrayList<String>();
        for (Integer i = 0; i < n; i++) {
            String database = databases.get(i);
            try {
                Cursor c1 = db.rawQuery("SELECT * FROM '" + database + "' WHERE achi = 1", null);
                Cursor c2 = db.rawQuery("SELECT achi FROM '" + database + "' order by date desc", null);
                Integer days = c1.getCount();
                Integer total = c2.getCount();
                Long rate = Math.round(100 * (double) days / (double) total);
                c2.moveToFirst();
                Integer counter = 0;
                for (int j = 0; j < total; j++) {
                    if (c2.getInt(c2.getColumnIndex("achi")) == 1) {
                        counter++;
                        c2.moveToNext();
                    } else {
                        break;
                    }
                }
                c1.close();
                c2.close();
                ItemBean item = new ItemBean();
                item.setGoal(databases.get(i));
                item.setPerc(days + "/" + total + "\n" + rate + "%");
                if (counter > 1) {
                    item.setSeq(counter + getString(R.string.in_a_row));
                } else {
                    item.setSeq("");
                }
                list.add(item);
            } catch(Exception e) {
                Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='" + database + "';", null);
                if (!(c.moveToFirst())) {
                    trash.add(database);
                }
            }
        }
        Integer trash_length = trash.size();
        for (Integer j = 0; j < trash_length; j++) {
            String trashdb = trash.get(j);
            try {
                databases.remove(trashdb);
                saveArray(databases);
            } catch (Exception e) {}
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

    public void moveToDetails(int position) {
        Intent intent = new Intent(this, DetailsActivity.class);
        ItemBean item = (ItemBean) listview.getItemAtPosition(position);
        String database = item.getGoal();
        intent.putExtra("database", database);
        startActivity(intent);
    }

    public class ItemBean {
        private String goal = "";
        private String perc = "";
        private String seq = "";
        public void setGoal(String goal) {
            this.goal = goal;
        }
        public String getGoal() {
            return goal;
        }
        public void setPerc(String perc) {
            this.perc = perc;
        }
        public String getPerc() {
            return perc;
        }
        public void setSeq(String seq) {
            this.seq = seq;
        }
        public String getSeq() {
            return seq;
        }
    }

    class ListAdapter extends ArrayAdapter<ItemBean> {
        private LayoutInflater mInflater;
        private TextView mGoal;
        private TextView mPerc;
        private TextView mSeq;
        public ListAdapter(Context context, List<ItemBean> objects) {
            super(context, 0, objects);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.row, null);
            }
            final ItemBean item = this.getItem(position);
            if(item != null){
                mGoal = (TextView)convertView.findViewById(R.id.goalText);
                mGoal.setText(item.getGoal());
                mPerc = (TextView)convertView.findViewById(R.id.percText);
                mPerc.setText(item.getPerc());
                mSeq = (TextView)convertView.findViewById(R.id.seqText);
                mSeq.setText(item.getSeq());
            }
            return convertView;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            showFragmentDialog(REGISTER_DIALOG);
        }
        if (id == R.id.action_alerm) {
            showFragmentDialog(ALERM_DIALOG);
        }
        return super.onOptionsItemSelected(item);
    }

    final int REGISTER_DIALOG = 0;
    final int ALERM_DIALOG = 1;
    public void showFragmentDialog(int id)
    {
        switch(id){
            case REGISTER_DIALOG:
                CustomDialogFragment dialogFragment = CustomDialogFragment.newInstance();
                dialogFragment.show(getSupportFragmentManager(), "fragment_dialog");
                break;
            case ALERM_DIALOG:
                AlarmDialogFragment dialogFragment1 = AlarmDialogFragment.newInstance();
                dialogFragment1.show(getSupportFragmentManager(), "fragment_dialog");
                break;
        }
    }

    /*
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        Boolean isChanged;
        isChanged = data.getBooleanExtra("isChanged", true);
        if(isChanged) {
            reload();
        }
    }
    */
}
