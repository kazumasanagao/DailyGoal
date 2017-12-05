package com.example.kazumasanagao.calendar;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.facebook.FacebookSdk;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.widget.ShareDialog;

/**
 * Created by kazumasanagao on 8/8/15.
 */
public class CelebrateDialogFragment extends DialogFragment {
    public static CelebrateDialogFragment newInstance()
    {
        return new CelebrateDialogFragment();
    }

    String database;
    String numOfSuccess;
    View mFooter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.celebrate_dialog, null, false);

        database = getArguments().getString("database");
        numOfSuccess = getArguments().getString("numOfSuccess");
        String Title = getString(R.string.congrats_title, database, numOfSuccess);

        TextView title = (TextView)view.findViewById(R.id.celebrateTitle);
        title.setText(Title);

        String[] arr = {"Facebook","Twitter"};
        ListView list = (ListView)view.findViewById(R.id.shareList);
        ArrayAdapter<String> adapter = new ArrayAdapter<String> (getActivity(), R.layout.sharebutton, arr);
        list.addFooterView(getFooter());
        list.setAdapter(adapter);
        list.setOnItemClickListener(new MyClickAdapter());

        Button closeButton = (Button)view.findViewById(R.id.celeClose);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
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

    class MyClickAdapter implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
            if (position == 0) {
                openFB();
            }
            if (position == 1) {
                openTwitter();
            }
        }
    }

    private View getFooter() {
        if (mFooter == null) {
            mFooter = getActivity().getLayoutInflater().inflate(R.layout.celebrate_footer, null);
        }
        return mFooter;
    }

    public void openFB() {
        FacebookSdk.sdkInitialize(getActivity());
        ShareDialog shareDialog = new ShareDialog(this);
        Uri imgUri = Uri.parse("http://dailygoal-web.chample.in/fb_logo.png");
        String title = database;
        if (title.length() > 17) {
            title = title.substring(0,17) + "…";
        }

        if (ShareDialog.canShow(ShareLinkContent.class)) {
            ShareLinkContent linkContent = new ShareLinkContent.Builder()
                    .setContentTitle(title)
                    .setContentDescription(
                            getString(R.string.fb_share, numOfSuccess))
                    .setContentUrl(Uri.parse("http://dailygoal-web.chample.in"))
                    .setImageUrl(imgUri)

                    .build();

            shareDialog.show(linkContent);
        }
    }

    public void openTwitter() {
        String mes = getString(R.string.twitter_share, database, numOfSuccess);
        String appname = getString(R.string.app_name);
        String url = "http://twitter.com/share?text="+mes+"%0a"+appname+"&url=https://play.google.com/store/apps/details?id=in.chample.dailygoal%26hl="+getString(R.string.country);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }

}
