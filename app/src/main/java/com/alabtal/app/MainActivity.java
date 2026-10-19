
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
    private int rosterSyncAttempts = 0;


    private final int navy = Color.rgb(10, 22, 40);
    private final int green = Color.rgb(16, 185, 129);
    private final int background = Color.rgb(7, 15, 29);
    private final int textColor = Color.rgb(240, 246, 252);

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
        loadSavedRoster();
        getWindow().setStatusBarColor(navy);
        getWindow().setNavigationBarColor(navy);
        showLogin();
    }

    private void loadSavedRoster() {
        android.content.SharedPreferences prefs =
                getSharedPreferences("live_roster", MODE_PRIVATE);
        try {
            JSONArray names = new JSONArray(prefs.getString("names", "[]"));
            JSONArray states = new JSONArray(prefs.getString("states", "[]"));
            liveRiderNames.clear();
            liveRiderStates.clear();
            for (int i = 0; i < names.length(); i++) {
                liveRiderNames.add(names.optString(i, ""));
                liveRiderStates.add(i < states.length()
                        ? states.optString(i, "Unknown") : "Unknown");
            }
        } catch (Exception ignored) {
            liveRiderNames.clear();
            liveRiderStates.clear();
        }
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

        private void addRiderCard(LinearLayout list, String rider, String status) {
        int muted = Color.rgb(160, 178, 199);
        String state = status == null || status.trim().isEmpty()
                ? "Unknown" : status.trim();
        String normalized = state.toLowerCase(java.util.Locale.ROOT);

        int badgeText = Color.rgb(252, 196, 91);
        int badgeBackground = Color.rgb(76, 61, 35);

        if (normalized.contains("offline") || normalized.contains("inactive")
                || normalized.contains("not active")) {
            badgeText = Color.rgb(252, 129, 129);
            badgeBackground = Color.rgb(76, 39, 52);
        } else if (normalized.contains("active") || normalized.contains("online")
                || normalized.contains("available")) {
            badgeText = Color.rgb(52, 211, 153);
            badgeBackground = Color.rgb(20, 69, 65);
        }

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackground(shape(Color.rgb(18, 33, 54), 16));
        card.setElevation(dp(2));

        LinearLayout info = column();

        TextView name = makeText(rider, 15, textColor);
        name.setTypeface(null, Typeface.BOLD);
        info.addView(name, params(-1, -2));

        TextView role = makeText("مندوب توصيل  •  RIDER", 11, muted);
        LinearLayout.LayoutParams roleParams = params(-1, -2);
        roleParams.topMargin = dp(5);
        info.addView(role, roleParams);

        card.addView(info, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView badge = makeText(state, 11, badgeText);
        badge.setTypeface(null, Typeface.BOLD);
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(dp(10), dp(7), dp(10), dp(7));
        badge.setBackground(shape(badgeBackground, 10));
        card.addView(badge, params(-2, -2));

        LinearLayout.LayoutParams cardParams = params(-1, -2);
        cardParams.bottomMargin = dp(10);
        list.addView(card, cardParams);
    }

        private void showDashboard(String username) {
        LinearLayout root = column();
        root.setBackgroundColor(background);

        LinearLayout header = column();
        header.setPadding(dp(20), dp(22), dp(20), dp(20));
        header.setBackground(shape(navy, 0));

        TextView brand = makeText("ALABTAL  •  OPERATIONS", 12, green);
        brand.setTypeface(null, Typeface.BOLD);
        header.addView(brand, params(-1, -2));

        TextView title = makeText("لوحة متابعة الشيفت", 24, Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = params(-1, -2);
        titleParams.topMargin = dp(10);
        header.addView(title, titleParams);

        TextView subtitle = makeText("LIVE SHIFT DASHBOARD", 12,
                Color.rgb(169, 190, 213));
        subtitle.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams subtitleParams = params(-1, -2);
        subtitleParams.topMargin = dp(4);
        header.addView(subtitle, subtitleParams);

        TextView welcome = makeText("أهلاً، " + username + "  •  Signed in",
                12, Color.rgb(200, 215, 231));
        LinearLayout.LayoutParams welcomeParams = params(-1, -2);
        welcomeParams.topMargin = dp(12);
        header.addView(welcome, welcomeParams);

        Button rosterButton = new Button(this);
        rosterButton.setText("🌐  فتح الروستر الحي  |  LIVE 3PL ROSTER");
        rosterButton.setTextColor(Color.WHITE);
        rosterButton.setTextSize(13);
        rosterButton.setAllCaps(false);
        rosterButton.setBackground(shape(green, 12));

        LinearLayout.LayoutParams rosterParams = params(-1, 50);
        rosterParams.topMargin = dp(18);
        header.addView(rosterButton, rosterParams);
        rosterButton.setOnClickListener(v -> showRosterPage(username));

        root.addView(header, params(-1, -2));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        LinearLayout content = column();
        content.setPadding(dp(16), dp(20), dp(16), dp(20));

        TextView section = makeText("المناديب  |  RIDERS", 18, Color.WHITE);
        section.setTypeface(null, Typeface.BOLD);
        content.addView(section, params(-1, -2));

        TextView hint = makeText("حالات المناديب المستوردة من اللايف",
                12, Color.rgb(156, 176, 199));
        LinearLayout.LayoutParams hintParams = params(-1, -2);
        hintParams.topMargin = dp(5);
        hintParams.bottomMargin = dp(14);
        content.addView(hint, hintParams);

        if (liveRiderNames.isEmpty()) {
            LinearLayout emptyCard = column();
            emptyCard.setPadding(dp(16), dp(18), dp(16), dp(18));
            emptyCard.setBackground(shape(Color.rgb(18, 33, 54), 16));

            TextView emptyTitle = makeText("لا توجد بيانات للمناديب بعد",
                    15, Color.WHITE);
            emptyTitle.setTypeface(null, Typeface.BOLD);
            emptyCard.addView(emptyTitle, params(-1, -2));

            TextView empty = makeText(
                    "افتح الروستر الحي واضغط Sync لاستيراد البيانات.\n"
                    + "Open Live 3PL Roster and tap Sync.",
                    12, Color.rgb(160, 178, 199));
            LinearLayout.LayoutParams emptyParams = params(-1, -2);
            emptyParams.topMargin = dp(8);
            emptyCard.addView(empty, emptyParams);
            content.addView(emptyCard, params(-1, -2));
        } else {
            LinearLayout countCard = new LinearLayout(this);
            countCard.setOrientation(LinearLayout.HORIZONTAL);
            countCard.setGravity(Gravity.CENTER_VERTICAL);
            countCard.setPadding(dp(14), dp(13), dp(14), dp(13));
            countCard.setBackground(shape(Color.rgb(18, 46, 57), 14));

            LinearLayout countInfo = column();
            TextView countLabel = makeText(
                    "إجمالي المناديب  •  TOTAL RIDERS",
                    11, Color.rgb(160, 210, 202));
            countInfo.addView(countLabel, params(-1, -2));

            TextView count = makeText(
                    String.valueOf(liveRiderNames.size()), 25, Color.WHITE);
            count.setTypeface(null, Typeface.BOLD);
            LinearLayout.LayoutParams numberParams = params(-1, -2);
            numberParams.topMargin = dp(3);
            countInfo.addView(count, numberParams);

            countCard.addView(countInfo, new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

            TextView liveTag = makeText("● LIVE DATA", 10, green);
            liveTag.setTypeface(null, Typeface.BOLD);
            countCard.addView(liveTag, params(-2, -2));

            LinearLayout.LayoutParams countCardParams = params(-1, -2);
            countCardParams.bottomMargin = dp(16);
            content.addView(countCard, countCardParams);

            for (int i = 0; i < liveRiderNames.size(); i++) {
                String riderState = i < liveRiderStates.size()
                        ? liveRiderStates.get(i) : "Unknown";
                addRiderCard(content, liveRiderNames.get(i), riderState);
            }
        }

        Button logout = new Button(this);
        logout.setText("تسجيل الخروج  |  LOG OUT");
        logout.setTextColor(Color.WHITE);
        logout.setAllCaps(false);
        logout.setBackground(shape(Color.rgb(29, 44, 65), 12));
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
                    if (!notify && rosterSyncAttempts < 10) {
                        rosterSyncAttempts++;
                        webView.postDelayed(
                                () -> syncRosterData(webView, false), 1500);
                        return;
                    }
                    rosterSyncAttempts = 0;
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

                rosterSyncAttempts = 0;
                liveRiderNames.clear();
                liveRiderNames.addAll(names);
                liveRiderStates.clear();
                liveRiderStates.addAll(states);

                try {
                    org.json.JSONArray savedNames = new org.json.JSONArray();
                    org.json.JSONArray savedStates = new org.json.JSONArray();
                    for (String name : liveRiderNames) savedNames.put(name);
                    for (String state : liveRiderStates) savedStates.put(state);
                    getSharedPreferences("live_roster", MODE_PRIVATE).edit()
                            .putString("names", savedNames.toString())
                            .putString("states", savedStates.toString())
                            .apply();
                } catch (Exception ignored) {
                }

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
