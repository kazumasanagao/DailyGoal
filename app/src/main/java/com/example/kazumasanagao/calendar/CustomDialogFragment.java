package com.example.kazumasanagao.calendar;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by kazumasanagao on 7/27/15.
 */
public class CustomDialogFragment extends DialogFragment
{

    public static CustomDialogFragment newInstance()
    {
        return new CustomDialogFragment();
    }

    private ListView mylist;
    private List<ItemBean> list;
    private ListAdapter adapter;
    private ArrayList<String> databases;
    private TextView titleView;
    private Calendar selectedDate;
    private Calendar today;
    private SimpleDateFormat sdf_sys;
    private DateFormat sdf_usr;
    private DatabaseHelper dbhelper;
    private SQLiteDatabase db;
    private boolean isIni;
    private Map<String ,Integer> candidates;
    private AlertDialog.Builder builder;
    private AlertDialog.Builder builder2;
    private Boolean Back_or_forward;
    private View mFooter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.custom_dialog, null, false);
        mylist = (ListView) view.findViewById(R.id.dialogview);
        candidates = new HashMap<String, Integer>();
        dbhelper = new DatabaseHelper(getActivity());
        db = dbhelper.getWritableDatabase();
        titleView = (TextView)view.findViewById(R.id.titleView);
        today = Calendar.getInstance();
        selectedDate = Calendar.getInstance();
        sdf_sys = new SimpleDateFormat("yyyy-MM-dd");
        sdf_usr = DateFormat.getDateInstance();
        databases = new ArrayList<String>();
        getArray();
        Back_or_forward = true;


        Date d = selectedDate.getTime();
        String date_usr = sdf_usr.format(d);
        titleView.setText(date_usr);

        list = new ArrayList<ItemBean>();
        setList();

        adapter = new ListAdapter(getActivity(),list);
        mylist.addFooterView(getFooter());
        mylist.setAdapter(adapter);


        builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(getString(R.string.notsaveyet));
        DialogInterface.OnClickListener okListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int button) {
                saveData(candidates);
                //dismiss();
            }
        };
        DialogInterface.OnClickListener cancelListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int button) {
                dismiss();
            }
        };
        builder.setNeutralButton(getString(R.string.save), okListener);
        builder.setNegativeButton(getString(R.string.dontsave), cancelListener);

        builder2 = new AlertDialog.Builder(getActivity());
        builder2.setMessage(getString(R.string.notsaveyet));
        DialogInterface.OnClickListener okListener2 = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int button) {
                saveData(candidates);
                if (Back_or_forward) {
                    DateBack();
                } else {
                    DateForward();
                }
            }
        };
        DialogInterface.OnClickListener cancelListener2 = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int button) {
                candidates.clear();
                if (Back_or_forward) {
                    DateBack();
                } else {
                    DateForward();
                }
            }
        };
        builder2.setNeutralButton(getString(R.string.save),okListener2);
        builder2.setNegativeButton(getString(R.string.dontsave), cancelListener2);

        Button saveButton = (Button) view.findViewById(R.id.savechanges);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveData(candidates);
            }
        });

        Button closeButton = (Button) view.findViewById(R.id.close);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (candidates.isEmpty()) {
                    dismiss();
                } else {
                    builder.show();
                }
            }
        });

        Button dateBack = (Button) view.findViewById(R.id.dateBack);
        Button dateForward = (Button) view.findViewById(R.id.dateForward);

        dateBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DateBack();
            }
        });
        dateForward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DateForward();
            }
        });

        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getDialog().getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);

    }

    public Integer getCounter(String database) {
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

    private View getFooter() {
        if (mFooter == null) {
            mFooter = getActivity().getLayoutInflater().inflate(R.layout.custom_footer, null);
        }
        return mFooter;
    }

    public class ItemBean {
        private String dbname = "";
        private Integer status = -1;

        public void setDBname(String dbname) {
            this.dbname = dbname;
        }
        public String getDBname() {
            return dbname;
        }
        public void setStatus(Integer status) {
            this.status = status;
        }
        public Integer getStatus() {
            return status;
        }
    }

    class ListAdapter extends ArrayAdapter<ItemBean> {
        private LayoutInflater mInflater;
        private RadioGroup group;
        private TextView mDBname;
        private RadioButton mRadio1;
        private RadioButton mRadio2;
        private RadioButton mRadio3;
        private Integer status;

        public ListAdapter(Context context, List<ItemBean> objects) {
            super(context, 0, objects);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.dialog_row, null);
            }
            final ItemBean item = this.getItem(position);
            if(item != null){
                mDBname = (TextView)convertView.findViewById(R.id.dbname);
                group = (RadioGroup)convertView.findViewById(R.id.group);
                mRadio1 = (RadioButton)convertView.findViewById(R.id.radio1);
                mRadio2 = (RadioButton)convertView.findViewById(R.id.radio2);
                mRadio3 = (RadioButton)convertView.findViewById(R.id.radio3);

                mDBname.setText(item.getDBname());
                status = item.getStatus();
                if (status == 1) {
                    isIni = true;
                    mRadio1.setChecked(true);
                    isIni = false;
                } else if (status == 0) {
                    isIni = true;
                    mRadio2.setChecked(true);
                    isIni = false;
                } else {
                    isIni = true;
                    mRadio3.setChecked(true);
                    isIni = false;
                }



                group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(RadioGroup group, int checkedId) {

                        Integer new_status;

                        if (isIni) {
                            isIni = false;
                        } else {
                            if (checkedId == R.id.radio1) {
                                new_status = 1;
                            } else if (checkedId == R.id.radio2) {
                                new_status = 0;
                            } else {
                                new_status = -1;
                            }
                            if (item.getStatus().equals(new_status)) {
                                candidates.remove(item.getDBname());
                            } else {
                                candidates.put(item.getDBname(), new_status);
                            }

                        }

                    }
                });

            }
            return convertView;
        }
    }

    public void setList() {
        Date d = selectedDate.getTime();
        String date_sys = sdf_sys.format(d);
        Integer n = databases.size();

        for (Integer i = 0; i < n; i++) {
            String database = databases.get(i);
            try {
                Cursor c = db.rawQuery("SELECT achi FROM '" + database + "' where date='" + date_sys + "'", null);
                Integer status = -1;
                if (c.moveToFirst()) {
                    status = c.getInt(c.getColumnIndex("achi"));
                }
                c.close();

                ItemBean item = new ItemBean();
                item.setDBname(database);
                item.setStatus(status);

                list.add(item);
            } catch(Exception e) {}
        }
    }

    public void getArray() {
        String list;
        Bundle bundle = new Bundle();
        Map<String, ?> prefKV = getActivity().getSharedPreferences("shared_preference", Context.MODE_PRIVATE).getAll();
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

    public void saveData(Map mp) {
        Iterator it = mp.entrySet().iterator();
        Date d = selectedDate.getTime();
        String date = sdf_sys.format(d);
        Map<String, Integer> congrats = new HashMap<>();

        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            String database = (String)pair.getKey();
            Integer status = (Integer)pair.getValue();
            try {
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

                if (isData) {
                    if (status > -1) {
                        db.execSQL("UPDATE '" + database + "' SET achi = " + status + " WHERE date = '" + date + "'");
                        Integer counter = getCounter(database);
                        if (counter % 5 == 0 && status == 1) {
                            congrats.put(database, counter);
                        }
                    } else {
                        db.execSQL("DELETE FROM '" + database + "' WHERE date = '" + date + "'");
                    }
                } else {
                    if (status > -1) {
                        ContentValues values = new ContentValues();
                        values.put("date", date);
                        values.put("achi", status);
                        db.insert("'"+database+"'", null, values);

                        Integer counter = getCounter(database);
                        if (counter % 5 == 0 && status == 1) {
                            congrats.put(database, counter);
                        }
                    }
                }
            } catch (Exception e) {}
        }
        candidates.clear();
        list.clear();
        setList();
        adapter.notifyDataSetChanged();
        Toast.makeText(getActivity(),R.string.save_success, Toast.LENGTH_SHORT).show();
        if (!congrats.isEmpty()) {
            openCelebrate(congrats);
        }
    }

    public void openCelebrate(Map<String, Integer> map) {
        Iterator it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            final String database = (String)pair.getKey();
            Integer NumOfSuccess = (Integer)pair.getValue();
            final String numOfSuccess = NumOfSuccess.toString();

            CelebrateDialogFragment dialogFragment = CelebrateDialogFragment.newInstance();
            Bundle args = new Bundle();
            args.putString("database", database);
            args.putString("numOfSuccess", numOfSuccess);
            dialogFragment.setArguments(args);
            dialogFragment.show(getActivity().getSupportFragmentManager(), "fragment_dialog");
        }
    }

    public void DateBack() {
        Back_or_forward = true;
        if (candidates.isEmpty()) {
            selectedDate.add(Calendar.DATE, -1);
            Date d = selectedDate.getTime();
            String date_usr = sdf_usr.format(d);
            titleView.setText(date_usr);

            candidates.clear();
            list.clear();
            setList();
            adapter.notifyDataSetChanged();
        } else {
            builder2.show();
        }
    }

    public void DateForward() {
        Back_or_forward = false;
        if (selectedDate.compareTo(today) < 0) {
            if (candidates.isEmpty()) {
                selectedDate.add(Calendar.DATE, 1);
                Date d = selectedDate.getTime();
                String date_usr = sdf_usr.format(d);
                titleView.setText(date_usr);

                candidates.clear();
                list.clear();
                setList();
                adapter.notifyDataSetChanged();
            } else {
                builder2.show();
            }
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        Intent intent = new Intent(getActivity(), MainActivity.class);
        startActivity(intent);
    }

}