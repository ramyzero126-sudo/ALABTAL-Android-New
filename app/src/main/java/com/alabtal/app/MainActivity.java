
package com.alabtal.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView makeText(String text, float size, int color) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(24), dp(24), dp(24), dp(24));
        root.setBackgroundColor(Color.rgb(245, 247, 250));

        TextView title = makeText("ALABTAL", 30, Color.rgb(20, 40, 70));
        title.setGravity(Gravity.CENTER);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(55)));

        TextView subtitle = makeText(
                "Live Shift Dashboard", 16, Color.DKGRAY);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(40)));

        TextView status = makeText(
                "New application initialized", 14, Color.rgb(70, 100, 80));
        status.setGravity(Gravity.CENTER);
        root.addView(status, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(50)));

        setContentView(root);
    }
}

