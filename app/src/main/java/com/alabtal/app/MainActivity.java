
package com.alabtal.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import com.google.firebase.auth.FirebaseAuth;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;

public class MainActivity extends Activity {

    private FirebaseAuth auth;
    private final ArrayList<String> liveRiderNames = new ArrayList<>();
    private final ArrayList<String> liveRiderStates = new ArrayList<>();

    private final int navy = Color.rgb(18, 38, 67);
    private final int green = Color.rgb(31, 155, 104);
    private final int background = Color.rgb(245, 247, 250);
    private final int textColor = Color.rgb(35, 48, 65);

    private int dp(float value) {
        return (int) (value *
                getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private TextView makeText(String text, float size, int color) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private LinearLayout.LayoutParams params(int width, int height) {
        return new LinearLayout.LayoutParams(
                width < 0 ? width : dp(width),
                height < 0 ? height : dp(height));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();
        getWindow().setStatusBarColor(navy);
        getWindow().setNavigationBarColor(navy);
        showLogin();
    }

    private void showLogin() {
        LinearLayout root = column();
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(26), dp(24), dp(26), dp(24));
        root.setBackgroundColor(background);

        TextView logo = makeText("ALABTAL", 30, navy);
        logo.setTypeface(null, Typeface.BOLD);
        logo.setGravity(Gravity.CENTER);
        root.addView(logo, params(-1, 48));

        TextView subtitle = makeText(
                "LIVE SHIFT MANAGEMENT", 12, Color.GRAY);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle, params(-1, 30));

        LinearLayout card = column();
        card.setPadding(dp(20), dp(22), dp(20), dp(22));
        card.setBackground(shape(Color.WHITE, 18));
        card.setElevation(dp(3));

        LinearLayout.LayoutParams cardParams = params(-1, -2);
        cardParams.topMargin = dp(28);
        root.addView(card, cardParams);

        TextView heading = makeText("Welcome back", 23, navy);
        heading.setTypeface(null, Typeface.BOLD);
        card.addView(heading, params(-1, -2));

        TextView hint = makeText(
                "Sign in to continue to your dashboard",
                13, Color.GRAY);
        LinearLayout.LayoutParams hintParams = params(-1, -2);
        hintParams.topMargin = dp(7);
        card.addView(hint, hintParams);

        TextView userLabel = makeText("Email address",
                13, textColor);
        userLabel.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams label1 = params(-1, -2);
        label1.topMargin = dp(24);
        card.addView(userLabel, label1);

        EditText username = new EditText(this);
        username.setSingleLine(true);
        username.setTextSize(15);
        username.setHint("Enter your email");
        username.setPadding(dp(12), 0, dp(12), 0);
        username.setBackground(shape(background, 10));
        card.addView(username, params(-1, 50));

        TextView passLabel = makeText("Password", 13, textColor);
        passLabel.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams label2 = params(-1, -2);
        label2.topMargin = dp(16);
        card.addView(passLabel, label2);

        EditText password = new EditText(this);
        password.setSingleLine(true);
        password.setTextSize(15);
        password.setHint("Enter your password");
        password.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        password.setPadding(dp(12), 0, dp(12), 0);
        password.setBackground(shape(background, 10));
        card.addView(password, params(-1, 50));

        Button login = new Button(this);
        login.setText("SIGN IN");
        login.setTextColor(Color.WHITE);
        login.setTextSize(14);
        login.setTypeface(null, Typeface.BOLD);
        login.setAllCaps(false);
        login.setBackground(shape(green, 10));

        LinearLayout.LayoutParams loginParams = params(-1, 50);
        loginParams.topMargin = dp(24);
        card.addView(login, loginParams);

        TextView note = makeText(
                "Authorized work account required",
                12, Color.GRAY);
        note.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams noteParams = params(-1, -2);
        noteParams.topMargin = dp(16);
        card.addView(note, noteParams);

        login.setOnClickListener(v -> {
            String user = username.getText().toString().trim();
            String pass = password.getText().toString();

            if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this,
                        "Enter your username and password",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            login.setEnabled(false);
            auth.signInWithEmailAndPassword(user, pass)
                    .addOnCompleteListener(this, task -> {
                        login.setEnabled(true);
                        if (task.isSuccessful()) {
                            showDashboard(user);
                        } else {
                            Toast.makeText(this,
                                    "Sign-in failed. Check your email and password.",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });

        setContentView(root);
    }

    private void addRiderCard(LinearLayout list,
                              String rider, String status) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(12), dp(9), dp(12), dp(9));
        card.setBackground(shape(Color.WHITE, 12));
        card.setElevation(dp(1));

        LinearLayout info = column();
        TextView name = makeText(rider, 14, textColor);
        name.setTypeface(null, Typeface.BOLD);
        info.addView(name, params(-1, -2));

        TextView role = makeText("Rider", 11, Color.GRAY);
        LinearLayout.LayoutParams roleParams = params(-1, -2);
        roleParams.topMargin = dp(2);
        info.addView(role, roleParams);

        card.addView(info, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView badge = makeText(status, 11, green);
        badge.setTypeface(null, Typeface.BOLD);
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(dp(9), dp(6), dp(9), dp(6));
        badge.setBackground(shape(
                Color.rgb(229, 246, 238), 8));
        card.addView(badge, params(-2, -2));

        LinearLayout.LayoutParams cardParams = params(-1, -2);
        cardParams.bottomMargin = dp(8);
        list.addView(card, cardParams);
    }

    private void showDashboard(String username) {
        LinearLayout root = column();
        root.setBackgroundColor(background);

        LinearLayout header = column();
        header.setPadding(dp(20), dp(20), dp(20), dp(18));
        header.setBackground(shape(navy, 0));

        TextView logo = makeText("ALABTAL", 22, Color.WHITE);
        logo.setTypeface(null, Typeface.BOLD);
        header.addView(logo, params(-1, -2));

        TextView title = makeText("Live Shift Dashboard",
                17, Color.WHITE);
        LinearLayout.LayoutParams titleParams = params(-1, -2);
        titleParams.topMargin = dp(8);
        header.addView(title, titleParams);

        TextView welcome = makeText(
                "Signed-in user: " + username, 12,
                Color.rgb(220, 230, 240));
        LinearLayout.LayoutParams welcomeParams = params(-1, -2);
        welcomeParams.topMargin = dp(5);
        header.addView(welcome, welcomeParams);

        Button rosterButton = new Button(this);
        rosterButton.setText("🌐 Import Live 3PL Roster");
        rosterButton.setTextColor(Color.WHITE);
        rosterButton.setTextSize(14);
        rosterButton.setAllCaps(false);
        rosterButton.setBackground(shape(green, 10));

        LinearLayout.LayoutParams rosterParams = params(-1, 48);
        rosterParams.topMargin = dp(14);
        header.addView(rosterButton, rosterParams);

        rosterButton.setOnClickListener(v -> showRosterPage(username));

        root.addView(header, params(-1, -2));

        ScrollView scroll = new ScrollView(this);
        LinearLayout content = column();
        content.setPadding(dp(16), dp(16), dp(16), dp(16));

        TextView section = makeText("Riders", 18, navy);
        section.setTypeface(null, Typeface.BOLD);
        content.addView(section, params(-1, 32));

        if (liveRiderNames.isEmpty()) {
            TextView empty = makeText(
                    "No live roster imported yet. Open Live 3PL Roster and tap Sync.",
                    12, Color.GRAY);
            LinearLayout.LayoutParams emptyParams = params(-1, -2);
            emptyParams.bottomMargin = dp(14);
            content.addView(empty, emptyParams);
        } else {
            TextView count = makeText(
                    "Live riders: " + liveRiderNames.size(), 12, Color.GRAY);
            LinearLayout.LayoutParams countParams = params(-1, -2);
            countParams.bottomMargin = dp(10);
            content.addView(count, countParams);

            for (int i = 0; i < liveRiderNames.size(); i++) {
                addRiderCard(content, liveRiderNames.get(i),
                        liveRiderStates.get(i));
            }
        }

        Button logout = new Button(this);
        logout.setText("Log out");
        logout.setAllCaps(false);
        logout.setOnClickListener(v -> {
            auth.signOut();
            showLogin();
        });
        LinearLayout.LayoutParams logoutParams = params(-1, 48);
        logoutParams.topMargin = dp(12);
        content.addView(logout, logoutParams);

        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    private void showRosterPage(String username) {
        LinearLayout root = column();
        root.setBackgroundColor(background);

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(8), dp(6), dp(12), dp(6));
        toolbar.setBackgroundColor(navy);

        Button back = new Button(this);
        back.setText("← Back");
        back.setTextColor(Color.WHITE);
        back.setAllCaps(false);
        back.setOnClickListener(v -> showDashboard(username));
        toolbar.addView(back, params(-2, 48));

        TextView title = makeText("Live 3PL Roster", 16, Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams titleParams =
                new LinearLayout.LayoutParams(0, -2, 1);
        titleParams.leftMargin = dp(8);
        toolbar.addView(title, titleParams);

        Button syncButton = new Button(this);
        syncButton.setText("Sync");
        syncButton.setTextColor(Color.WHITE);
        syncButton.setAllCaps(false);
        toolbar.addView(syncButton, params(-2, 48));

        root.addView(toolbar, params(-1, -2));

        WebView webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                syncRosterData(view, false);
            }
        });

        syncButton.setOnClickListener(v -> syncRosterData(webView, true));

        root.addView(webView, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);

        webView.loadUrl(
                "https://eg.me.logisticsbackoffice.com/dashboard/rooster/live-3pl"
        );
    }

    private void syncRosterData(WebView webView, boolean notify) {
        String script =
                "(function() {" +
                "var t = document.querySelector('table.cape-table');" +
                "if (!t) return [];" +
                "function txt(e) {" +
                "return (e.innerText || e.textContent || '').trim().replace(/\\s+/g,' ');" +
                "}" +
                "return Array.from(t.querySelectorAll('tbody tr')).map(function(r) {" +
                "var c = Array.from(r.children).filter(function(e) {return e.tagName==='TD';});" +
                "if (c.length < 10) return null;" +
                "var name = txt(c[0]);" +
                "var state = txt(c[9]);" +
                "if (!name) return null;" +
                "return {name:name, state:state || 'Unknown'};" +
                "}).filter(function(x) {return x !== null;});" +
                "})()";

        webView.evaluateJavascript(script, result -> {
            try {
                JSONArray rows = new JSONArray(result == null ? "[]" : result);
                if (rows.length() == 0) {
                    if (notify) {
                        Toast.makeText(this,
                                "No roster rows found. Wait for the table, then tap Sync.",
                                Toast.LENGTH_LONG).show();
                    }
                    return;
                }

                ArrayList<String> names = new ArrayList<>();
                ArrayList<String> states = new ArrayList<>();

                for (int i = 0; i < rows.length(); i++) {
                    JSONObject row = rows.getJSONObject(i);
                    String name = row.optString("name", "").trim();
                    String state = row.optString("state", "Unknown").trim();
                    if (!name.isEmpty()) {
                        names.add(name);
                        states.add(state.isEmpty() ? "Unknown" : state);
                    }
                }

                if (names.isEmpty()) {
                    if (notify) {
                        Toast.makeText(this, "No rider names found in the table.",
                                Toast.LENGTH_LONG).show();
                    }
                    return;
                }

                liveRiderNames.clear();
                liveRiderNames.addAll(names);
                liveRiderStates.clear();
                liveRiderStates.addAll(states);

                if (notify) {
                    Toast.makeText(this,
                            "Imported " + names.size() +
                                    " riders. Tap Back to view the dashboard.",
                            Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                if (notify) {
                    Toast.makeText(this, "Could not read roster table.",
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
