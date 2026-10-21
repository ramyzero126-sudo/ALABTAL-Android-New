
package com.alabtal.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
    private final ArrayList<JSONObject> liveRiderDetails = new ArrayList<>();
    private final Handler rosterHandler = new Handler(Looper.getMainLooper());
    private Runnable rosterAutoSync;
    private String lastRosterSyncLabel = "لم تتم المزامنة";
    private String rosterSyncStatus = "لم تتم المزامنة";
    private boolean rosterSyncSuccessful = false;
    private WebView rosterWebView;
    private boolean rosterPageLoaded = false;
    private boolean dashboardVisible = false;
    private String currentUsername = "";

    private int rosterSyncAttempts = 0;
    private int rosterRefreshTicks = 0;


    private final int navy = Color.rgb(18, 18, 18);
    private final int green = Color.rgb(255, 121, 0);
    private final int background = Color.rgb(8, 8, 8);
    private final int textColor = Color.rgb(240, 240, 240);

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
            JSONArray details = new JSONArray(prefs.getString("details", "[]"));
            liveRiderNames.clear();
            liveRiderStates.clear();
            liveRiderDetails.clear();
            for (int i = 0; i < names.length(); i++) {
                liveRiderNames.add(names.optString(i, ""));
                liveRiderStates.add(i < states.length()
                        ? states.optString(i, "Unknown") : "Unknown");
                JSONObject detail = details.optJSONObject(i);
                liveRiderDetails.add(detail == null ? new JSONObject() : detail);
            }
            lastRosterSyncLabel = prefs.getString("last_sync", "لم تتم المزامنة");
        } catch (Exception ignored) {
            liveRiderNames.clear();
            liveRiderStates.clear();
            liveRiderDetails.clear();
        }
    }

    private void showLogin() {
        LinearLayout root = column();
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(26), dp(24), dp(26), dp(24));
        root.setBackgroundColor(background);

        TextView logo = makeText("ALABTAL", 30, green);
        logo.setTypeface(null, Typeface.BOLD);
        logo.setGravity(Gravity.CENTER);
        root.addView(logo, params(-1, 48));

        TextView subtitle = makeText(
                "LIVE SHIFT MANAGEMENT", 12, Color.GRAY);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle, params(-1, 30));

        LinearLayout card = column();
        card.setPadding(dp(20), dp(22), dp(20), dp(22));
        card.setBackground(shape(Color.rgb(22, 22, 22), 18));
        card.setElevation(dp(3));

        LinearLayout.LayoutParams cardParams = params(-1, -2);
        cardParams.topMargin = dp(28);
        root.addView(card, cardParams);

        TextView heading = makeText("Welcome back", 23, Color.WHITE);
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
        username.setTextColor(Color.WHITE);
        username.setHintTextColor(Color.GRAY);
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
        password.setTextColor(Color.WHITE);
        password.setHintTextColor(Color.GRAY);
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

        private String formatShiftTime(String value) {
        if (value == null || value.trim().isEmpty()) return "—";
        String v = value.trim();
        int t = v.indexOf("T");
        if (t >= 0 && v.length() >= t + 6) return v.substring(t + 1, t + 6);
        int space = v.lastIndexOf(" ");
        if (space >= 0 && v.length() >= space + 6) return v.substring(space + 1, space + 6);
        return v;
    }


    private void addRiderMetric(LinearLayout row, String label, String value) {
        int orange = Color.rgb(255, 151, 55);

        LinearLayout tile = column();
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(5), dp(9), dp(5), dp(9));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(24, 21, 18));
        bg.setCornerRadius(dp(11));
        bg.setStroke(dp(1), Color.rgb(105, 62, 30));
        tile.setBackground(bg);

        TextView title = makeText(label, 11, orange);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(null, Typeface.BOLD);
        title.setMaxLines(2);
        tile.addView(title, params(-1, -2));

        String shown = value == null || value.trim().isEmpty()
                ? "—" : value.trim();

        TextView data = makeText(shown, 13, Color.WHITE);
        data.setGravity(Gravity.CENTER);
        data.setTypeface(null, Typeface.BOLD);
        data.setMaxLines(3);
        data.setPadding(0, dp(5), 0, 0);
        tile.addView(data, params(-1, -2));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        lp.leftMargin = dp(3);
        lp.rightMargin = dp(3);
        row.addView(tile, lp);
    }


    private final java.util.HashMap<String, Long> workTimerBaseSeconds =
            new java.util.HashMap<>();
    private final java.util.HashMap<String, Long> workTimerBaseAtMillis =
            new java.util.HashMap<>();

    private long parseWorkedSeconds(JSONObject detail, String worked) {
        Object raw = detail.opt("workedSeconds");
        if (raw instanceof Number) {
            long seconds = ((Number) raw).longValue();
            if (seconds >= 0) return seconds;
        }

        if (worked == null || worked.trim().isEmpty()) return -1L;
        String value = worked.trim();

        try {
            if (value.matches("\\d+")) return Long.parseLong(value);

            if (value.matches("\\d{1,}:\\d{1,2}:\\d{1,2}")) {
                String[] parts = value.split(":");
                return Long.parseLong(parts[0]) * 3600L
                        + Long.parseLong(parts[1]) * 60L
                        + Long.parseLong(parts[2]);
            }

            java.util.regex.Matcher hm = java.util.regex.Pattern
                    .compile("(\\d+)\\s*h", java.util.regex.Pattern.CASE_INSENSITIVE)
                    .matcher(value);
            java.util.regex.Matcher mm = java.util.regex.Pattern
                    .compile("(\\d+)\\s*m", java.util.regex.Pattern.CASE_INSENSITIVE)
                    .matcher(value);
            java.util.regex.Matcher sm = java.util.regex.Pattern
                    .compile("(\\d+)\\s*s", java.util.regex.Pattern.CASE_INSENSITIVE)
                    .matcher(value);

            long hours = hm.find() ? Long.parseLong(hm.group(1)) : 0L;
            long minutes = mm.find() ? Long.parseLong(mm.group(1)) : 0L;
            long secs = sm.find() ? Long.parseLong(sm.group(1)) : 0L;

            if (hm.reset().find() || mm.reset().find() || sm.reset().find()) {
                return hours * 3600L + minutes * 60L + secs;
            }
        } catch (Exception ignored) {
        }
        return -1L;
    }

    private void addWorkTimeMetric(LinearLayout row, JSONObject detail,
                                   String worked, String riderName) {
        final int orange = Color.rgb(255, 151, 55);
        final String key = detail.optString("employee_id",
                detail.optString("rider_id",
                detail.optString("id", riderName == null ? "" : riderName)));

        long serverSeconds = parseWorkedSeconds(detail, worked);
        long now = System.currentTimeMillis();

        if (serverSeconds >= 0) {
            Long savedBase = workTimerBaseSeconds.get(key);
            Long savedAt = workTimerBaseAtMillis.get(key);

            if (savedBase == null || savedAt == null) {
                workTimerBaseSeconds.put(key, serverSeconds);
                workTimerBaseAtMillis.put(key, now);
            } else {
                long currentEstimate = savedBase + Math.max(0L, (now - savedAt) / 1000L);
                if (serverSeconds > currentEstimate) {
                    workTimerBaseSeconds.put(key, serverSeconds);
                    workTimerBaseAtMillis.put(key, now);
                }
            }
        }

        final Long storedBase = workTimerBaseSeconds.get(key);
        final Long storedAt = workTimerBaseAtMillis.get(key);

        LinearLayout tile = column();
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(5), dp(9), dp(5), dp(9));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(24, 21, 18));
        bg.setCornerRadius(dp(11));
        bg.setStroke(dp(1), Color.rgb(105, 62, 30));
        tile.setBackground(bg);

        TextView title = makeText("وقت العمل", 11, orange);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(null, Typeface.BOLD);
        tile.addView(title, params(-1, -2));

        TextView data = makeText("—", 13, Color.WHITE);
        data.setGravity(Gravity.CENTER);
        data.setTypeface(null, Typeface.BOLD);
        data.setMaxLines(2);
        data.setPadding(0, dp(5), 0, 0);
        tile.addView(data, params(-1, -2));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        lp.leftMargin = dp(3);
        lp.rightMargin = dp(3);
        row.addView(tile, lp);

        if (storedBase == null || storedAt == null) return;

        data.post(new Runnable() {
            @Override
            public void run() {
                if (data.getParent() == null) return;

                long elapsed = Math.max(0L,
                        (System.currentTimeMillis() - storedAt) / 1000L);
                long total = storedBase + elapsed;
                long hours = total / 3600L;
                long minutes = (total % 3600L) / 60L;
                long seconds = total % 60L;

                data.setText(String.format(java.util.Locale.ROOT,
                        "%02d:%02d:%02d", hours, minutes, seconds));

                if (data.isShown()) data.postDelayed(this, 1000L);
            }
        });
    }

    private void addRiderInfoBox(LinearLayout parent, String label,
                                 String value, boolean orderBox) {
        int orange = Color.rgb(255, 151, 55);

        LinearLayout box = column();
        box.setPadding(dp(10), dp(8), dp(10), dp(8));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(orderBox
                ? Color.rgb(32, 22, 15) : Color.rgb(23, 23, 23));
        bg.setCornerRadius(dp(10));
        bg.setStroke(dp(1), orderBox
                ? Color.rgb(132, 75, 30) : Color.rgb(53, 43, 35));
        box.setBackground(bg);

        TextView title = makeText(label, 11, orange);
        title.setTypeface(null, Typeface.BOLD);
        box.addView(title, params(-1, -2));

        String shown = value == null || value.trim().isEmpty()
                ? (orderBox ? "جاري تحديد مكان الأوردر" : "غير متاح")
                : value.trim();

        TextView data = makeText(shown, 13, Color.rgb(245, 245, 245));
        data.setMaxLines(orderBox ? 5 : 3);
        LinearLayout.LayoutParams textLp = params(-1, -2);
        textLp.topMargin = dp(3);
        box.addView(data, textLp);

        LinearLayout.LayoutParams lp = params(-1, -2);
        lp.topMargin = dp(6);
        parent.addView(box, lp);
    }

    private void addRiderCard(LinearLayout list, String rider,
                              String status, JSONObject detail) {
        final int orange = Color.rgb(255, 139, 35);
        String state = status == null || status.trim().isEmpty()
                ? "Unknown" : status.trim();
        String normalized = state.toLowerCase(java.util.Locale.ROOT);

        int badgeText = Color.rgb(255, 190, 90);
        int badgeBackground = Color.rgb(72, 48, 22);

        if (normalized.contains("offline") || normalized.contains("inactive")
                || normalized.contains("not active")) {
            badgeText = Color.rgb(255, 125, 125);
            badgeBackground = Color.rgb(72, 30, 30);
        } else if (normalized.contains("active") || normalized.contains("online")
                || normalized.contains("available") || normalized.equals("working")) {
            badgeText = Color.rgb(100, 220, 155);
            badgeBackground = Color.rgb(24, 62, 43);
        }

        if (detail == null) detail = new JSONObject();

        String session = detail.optString("session", "");
        String wallet = detail.optString("wallet", "");
        String performance = detail.optString("performance", "");
        String acceptance = detail.optString("acceptance", "");
        String worked = detail.optString("worked", "");
        String plannedStart = detail.optString("plannedShiftStart", "");
        String plannedEnd = detail.optString("plannedShiftEnd", "");
        String orderLocation = detail.optString("orderLocation", "").trim();
        int orderCount = Math.max(0, detail.optInt("activeDeliveriesCount", 0));
        String orderCountLabel = detail.optBoolean("hasActiveDeliveries", false)
                ? String.valueOf(orderCount) : "0";

        LinearLayout card = column();
        card.setPadding(dp(12), dp(12), dp(12), dp(12));

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.rgb(12, 12, 12));
        cardBg.setCornerRadius(dp(16));
        cardBg.setStroke(dp(1), orange);
        card.setBackground(cardBg);
        card.setAlpha(0f);
        card.setTranslationY(dp(5));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView name = makeText(
                rider == null || rider.trim().isEmpty()
                        ? "مندوب بدون اسم" : rider,
                16, Color.WHITE);
        name.setTypeface(null, Typeface.BOLD);
        name.setMaxLines(3);
        name.setShadowLayer(dp(4), 0, 0, Color.rgb(125, 54, 8));
        top.addView(name, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView badge = makeText(state, 11, badgeText);
        badge.setTypeface(null, Typeface.BOLD);
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(dp(8), dp(5), dp(8), dp(5));
        badge.setBackground(shape(badgeBackground, 10));
        top.addView(badge, params(-2, -2));
        card.addView(top, params(-1, -2));

        LinearLayout metrics = new LinearLayout(this);
        metrics.setOrientation(LinearLayout.HORIZONTAL);
        metrics.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout.LayoutParams metricsLp = params(-1, -2);
        metricsLp.topMargin = dp(12);

        addRiderMetric(metrics, "الأوردرات النشطة", orderCountLabel);
        addRiderMetric(metrics, "نسبة القبول", acceptance);
        addWorkTimeMetric(metrics, detail, worked, name.getText().toString());

        String planned = (!plannedStart.isEmpty() || !plannedEnd.isEmpty())
                ? formatShiftTime(plannedStart) + " - "
                    + formatShiftTime(plannedEnd)
                : "—";
        addRiderMetric(metrics, "الشيفت المخطط", planned);
        card.addView(metrics, metricsLp);

        LinearLayout details = column();
        LinearLayout.LayoutParams detailsLp = params(-1, -2);
        detailsLp.topMargin = dp(7);
        card.addView(details, detailsLp);

        addRiderInfoBox(details, "المحفظة", wallet, false);
        addRiderInfoBox(details, "الأداء والتوصيلات", performance, false);
        addRiderInfoBox(details, "الشيفت الحالي", session, false);
        addRiderInfoBox(details, "مكان الأوردر", orderLocation, true);

        LinearLayout.LayoutParams cardLp = params(-1, -2);
        cardLp.bottomMargin = dp(11);
        list.addView(card, cardLp);

        card.animate().alpha(1f).translationY(0f).setDuration(260).start();

        // نبضة برتقالية واحدة لكل كارت، بدون حلقة رسوم متحركة دائمة.
        card.postDelayed(() -> {
            if (card.isShown()) {
                cardBg.setStroke(dp(2), Color.rgb(255, 180, 90));
                card.invalidate();
                card.postDelayed(() -> {
                    if (card.isShown()) {
                        cardBg.setStroke(dp(1), orange);
                        card.invalidate();
                    }
                }, 350);
            }
        }, 300);
    }

    private void showDashboard(String username) {
        currentUsername = username;
        dashboardVisible = true;
        LinearLayout root = column();
        root.setBackgroundColor(background);

        LinearLayout header = column();
        header.setPadding(dp(20), dp(22), dp(20), dp(20));
        header.setBackground(shape(navy, 0));

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);

        TextView brand = makeText("ALABTAL", 21, green);
        brand.setTypeface(null, Typeface.BOLD);
        toolbar.addView(brand, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        Button globeButton = new Button(this);
        globeButton.setText("🌐");
        globeButton.setTextSize(19);
        globeButton.setAllCaps(false);
        globeButton.setMinWidth(0);
        globeButton.setMinimumWidth(0);
        globeButton.setPadding(dp(8), 0, dp(8), 0);
        globeButton.setTextColor(green);
        globeButton.setBackground(shape(Color.rgb(30, 30, 30), 10));
        toolbar.addView(globeButton, params(46, 44));
        globeButton.setOnClickListener(v -> showRosterPage(username));

        Button bellButton = new Button(this);
        bellButton.setText("🔔");
        bellButton.setTextSize(17);
        bellButton.setAllCaps(false);
        bellButton.setMinWidth(0);
        bellButton.setMinimumWidth(0);
        bellButton.setPadding(dp(5), 0, dp(5), 0);
        bellButton.setTextColor(green);
        bellButton.setBackground(shape(Color.rgb(30, 30, 30), 10));
        LinearLayout.LayoutParams bellParams = params(46, 44);
        bellParams.leftMargin = dp(7);
        toolbar.addView(bellButton, bellParams);
        bellButton.setOnClickListener(v ->
                Toast.makeText(this,
                        "الإشعارات غير مربوطة بمصدر حقيقي حتى الآن",
                        Toast.LENGTH_SHORT).show());

        Button topLogout = new Button(this);
        topLogout.setText("خروج");
        topLogout.setTextSize(12);
        topLogout.setTypeface(null, Typeface.BOLD);
        topLogout.setAllCaps(false);
        topLogout.setMinWidth(0);
        topLogout.setMinimumWidth(0);
        topLogout.setPadding(dp(8), 0, dp(8), 0);
        topLogout.setTextColor(Color.WHITE);
        topLogout.setBackground(shape(green, 10));
        LinearLayout.LayoutParams topLogoutParams = params(-2, 44);
        topLogoutParams.leftMargin = dp(7);
        toolbar.addView(topLogout, topLogoutParams);
        topLogout.setOnClickListener(v -> {
            stopRosterAutoSync();
            dashboardVisible = false;
            auth.signOut();
            showLogin();
        });

        header.addView(toolbar, params(-1, 48));


        final int ledOrange = Color.rgb(255, 145, 45);

        LinearLayout ledBanner = new LinearLayout(this);
        ledBanner.setGravity(Gravity.CENTER_VERTICAL);
        ledBanner.setPadding(dp(10), dp(7), dp(10), dp(7));

        GradientDrawable ledBg = new GradientDrawable();
        ledBg.setColor(Color.rgb(9, 9, 9));
        ledBg.setCornerRadius(dp(10));
        ledBg.setStroke(dp(1), ledOrange);
        ledBanner.setBackground(ledBg);

        TextView ledText = makeText("", 17, ledOrange);
        ledText.setTypeface(null, Typeface.BOLD);
        ledText.setSingleLine(true);
        ledText.setGravity(Gravity.CENTER);
        ledText.setShadowLayer(dp(7), 0, 0, Color.rgb(180, 70, 0));
        ledBanner.addView(ledText, new LinearLayout.LayoutParams(
                -1, dp(30)));

        LinearLayout.LayoutParams ledParams = params(-1, 48);
        ledParams.topMargin = dp(12);
        header.addView(ledBanner, ledParams);

        // Typewriter: كتابة حقيقية حرفاً بحرف، وليس نصاً متحركاً.
        final String ledPhrase = "AL ABTAL TALABAT طنطا";
        final Handler typeHandler = new Handler(Looper.getMainLooper());
        final Runnable[] typing = new Runnable[1];
        typing[0] = new Runnable() {
            int index = 0;

            @Override public void run() {
                if (!ledBanner.isShown() || !dashboardVisible) return;

                if (index < ledPhrase.length()) {
                    index++;
                    ledText.setText(ledPhrase.substring(0, index) + " ▌");
                    typeHandler.postDelayed(this,
                            ledPhrase.charAt(index - 1) == ' ' ? 150 : 90);
                } else {
                    ledText.setText(ledPhrase + "  •");
                    // انتهت الكتابة؛ لا توجد حلقة توقيت دائمة.
                }
            }
        };
        ledBanner.postDelayed(typing[0], 250);

        // توهج بسيط لمرة واحدة، مع إعادة الإطار للونه الأصلي.
        ledBanner.postDelayed(() -> {
            if (ledBanner.isShown()) {
                ledBg.setStroke(dp(2), Color.rgb(255, 190, 105));
                ledText.setShadowLayer(dp(9), 0, 0, ledOrange);
                ledBanner.invalidate();
                ledBanner.postDelayed(() -> {
                    if (ledBanner.isShown()) {
                        ledBg.setStroke(dp(1), ledOrange);
                        ledText.setShadowLayer(dp(7), 0, 0,
                                Color.rgb(180, 70, 0));
                        ledBanner.invalidate();
                    }
                }, 500);
            }
        }, 500);

        TextView welcome = makeText("أهلاً، " + username + "  •  Signed in",
                12, Color.rgb(200, 215, 231));
        LinearLayout.LayoutParams welcomeParams = params(-1, -2);
        welcomeParams.topMargin = dp(12);
        header.addView(welcome, welcomeParams);

        root.addView(header, params(-1, -2));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        LinearLayout content = column();
        content.setPadding(dp(16), dp(20), dp(16), dp(20));

        TextView section = makeText("المناديب  |  RIDERS", 18, Color.WHITE);
        section.setTypeface(null, Typeface.BOLD);
        content.addView(section, params(-1, -2));

        TextView syncStatusView = makeText(
                "● " + rosterSyncStatus,
                12, rosterSyncSuccessful ? green : Color.rgb(255, 100, 80));
        syncStatusView.setTypeface(null, Typeface.BOLD);
        content.addView(syncStatusView, params(-1, -2));

        TextView hint = makeText("آخر تحديث ناجح: " + lastRosterSyncLabel,
                12, Color.rgb(156, 176, 199));
        LinearLayout.LayoutParams hintParams = params(-1, -2);
        hintParams.topMargin = dp(5);
        hintParams.bottomMargin = dp(10);
        content.addView(hint, hintParams);

        int readyCount = 0;
        int workingCount = 0;
        int breakCount = 0;
        int tempCount = 0;
        int lateCount = 0;

        for (int i = 0; i < liveRiderNames.size(); i++) {
            String state = i < liveRiderStates.size()
                    ? liveRiderStates.get(i) : "";
            String st = state == null ? ""
                    : state.toLowerCase(java.util.Locale.ROOT);

            if (st.contains("break")) {
                breakCount++;
            } else if (st.contains("temp")) {
                tempCount++;
            } else if (st.contains("late")) {
                lateCount++;
            } else if (st.contains("ready") || st.contains("available")) {
                readyCount++;
            } else if (st.contains("working") || st.equals("active")
                    || st.contains("online")) {
                workingCount++;
            }
        }

        String[] statusLabels = {
                "Ready", "Working", "Break",
                "Temp", "Late", "الإجمالي"
        };
        int[] statusValues = {
                readyCount, workingCount, breakCount,
                tempCount, lateCount, liveRiderNames.size()
        };

        for (int r = 0; r < 2; r++) {
            LinearLayout statusRow = new LinearLayout(this);
            statusRow.setOrientation(LinearLayout.HORIZONTAL);
            statusRow.setGravity(Gravity.CENTER_VERTICAL);

            LinearLayout.LayoutParams statusRowParams = params(-1, -2);
            statusRowParams.bottomMargin = dp(7);
            content.addView(statusRow, statusRowParams);

            for (int c = 0; c < 3; c++) {
                int index = r * 3 + c;
                addRiderMetric(statusRow, statusLabels[index],
                        String.valueOf(statusValues[index]));
            }
        }

        // إجمالي وقت العمل المسجل والأوردرات النشطة
        long totalWorkedSeconds = 0L;
        int ridersWithWorkTime = 0;
        int totalActiveOrders = 0;

        for (int i = 0; i < liveRiderDetails.size(); i++) {
            JSONObject detail = liveRiderDetails.get(i);
            if (detail == null) continue;

            long seconds = parseWorkedSeconds(
                    detail, detail.optString("worked", ""));
            if (seconds >= 0) {
                totalWorkedSeconds += seconds;
                ridersWithWorkTime++;
            }

            totalActiveOrders += Math.max(
                    0, detail.optInt("activeDeliveriesCount", 0));
        }

        long totalHours = totalWorkedSeconds / 3600L;
        long totalMinutes = (totalWorkedSeconds % 3600L) / 60L;
        String totalWorkLabel = ridersWithWorkTime == 0
                ? "لا توجد بيانات"
                : String.format(java.util.Locale.ROOT,
                        "%d س %02d د", totalHours, totalMinutes);

        LinearLayout summaryRow = new LinearLayout(this);
        summaryRow.setOrientation(LinearLayout.HORIZONTAL);
        summaryRow.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout.LayoutParams summaryParams = params(-1, -2);
        summaryParams.bottomMargin = dp(14);
        content.addView(summaryRow, summaryParams);

        addRiderMetric(summaryRow, "إجمالي وقت العمل المسجل",
                totalWorkLabel);
        addRiderMetric(summaryRow, "الأوردرات النشطة",
                String.valueOf(totalActiveOrders));

        Button refreshButton = new Button(this);
        refreshButton.setText("⟳  تحديث البيانات  •  تلقائي كل 30 ثانية");
        refreshButton.setTextColor(Color.WHITE);
        refreshButton.setTextSize(13);
        refreshButton.setTypeface(null, Typeface.BOLD);
        refreshButton.setAllCaps(false);
        refreshButton.setBackground(shape(green, 12));
        LinearLayout.LayoutParams refreshParams = params(-1, 46);
        refreshParams.bottomMargin = dp(16);
        content.addView(refreshButton, refreshParams);
        refreshButton.setOnClickListener(v -> {
            if (rosterWebView != null) {
                refreshButton.setEnabled(false);
                refreshButton.setText("⟳  جاري تحديث البيانات...");
                syncRosterData(rosterWebView, true);
                refreshButton.postDelayed(() -> {
                    if (refreshButton.getParent() != null) {
                        refreshButton.setEnabled(true);
                        refreshButton.setText("⟳  تحديث البيانات  •  تلقائي كل 30 ثانية");
                    }
                }, 2500);
            } else {
                Toast.makeText(this, "مصدر البيانات غير جاهز", Toast.LENGTH_SHORT).show();
            }
        });

        if (liveRiderNames.isEmpty()) {
            LinearLayout emptyCard = column();
            emptyCard.setPadding(dp(16), dp(18), dp(16), dp(18));
            emptyCard.setBackground(shape(Color.rgb(22, 22, 22), 16));
            android.graphics.drawable.GradientDrawable emptyBg =
                    (android.graphics.drawable.GradientDrawable) emptyCard.getBackground();
            emptyBg.setStroke(dp(1), green);

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
            countCard.setBackground(shape(Color.rgb(24, 24, 24), 14));

            LinearLayout countInfo = column();
            TextView countLabel = makeText(
                    "إجمالي المناديب  •  TOTAL RIDERS",
                    11, Color.rgb(255, 160, 70));
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
                addRiderCard(content, liveRiderNames.get(i), riderState,
                            i < liveRiderDetails.size()
                                    ? liveRiderDetails.get(i) : new JSONObject());
            }
        }

        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        // Bottom navigation: Home and Bookings
        LinearLayout bottomNav = new LinearLayout(this);
        bottomNav.setOrientation(LinearLayout.HORIZONTAL);
        bottomNav.setGravity(Gravity.CENTER);
        bottomNav.setPadding(dp(10), dp(8), dp(10), dp(8));
        bottomNav.setBackground(shape(Color.rgb(18, 18, 18), 0));

        Button homeTab = new Button(this);
        homeTab.setText("⌂  الرئيسية");
        homeTab.setAllCaps(false);
        homeTab.setTextColor(Color.WHITE);
        homeTab.setTextSize(13);
        homeTab.setBackground(shape(green, 12));

        Button bookingsTab = new Button(this);
        bookingsTab.setText("▣  الحجوزات");
        bookingsTab.setAllCaps(false);
        bookingsTab.setTextColor(green);
        bookingsTab.setTextSize(13);
        bookingsTab.setBackground(shape(Color.rgb(35, 28, 22), 12));

        Button ordersTab = new Button(this);
        ordersTab.setText("▤  الأوردرات");
        ordersTab.setAllCaps(false);
        ordersTab.setTextColor(green);
        ordersTab.setTextSize(13);
        ordersTab.setBackground(shape(Color.rgb(35, 28, 22), 12));

        LinearLayout.LayoutParams tabParams =
                new LinearLayout.LayoutParams(0, dp(46), 1f);
        tabParams.leftMargin = dp(5);
        tabParams.rightMargin = dp(5);
        bottomNav.addView(homeTab, tabParams);
        bottomNav.addView(bookingsTab, tabParams);
        bottomNav.addView(ordersTab, tabParams);
        root.addView(bottomNav, params(-1, 62));

        homeTab.setOnClickListener(v -> showDashboard(username));
        bookingsTab.setOnClickListener(v -> showBookings(username));
        ordersTab.setOnClickListener(v -> showOrders(username));

        setContentView(root);

        ensureRosterWebView();
        attachRosterWebView(root, false);
        if (!rosterPageLoaded) {
            rosterPageLoaded = true;
            rosterWebView.loadUrl("https://eg.me.logisticsbackoffice.com/dashboard/rooster/live-3pl");
        } else if (rosterAutoSync == null) {
            syncRosterData(rosterWebView, false);
            startRosterAutoSync(rosterWebView);
        }
    }



    private void showOrders(String username) {
        currentUsername = username;
        dashboardVisible = true;

        LinearLayout root = column();
        root.setBackgroundColor(background);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), dp(14), dp(16), dp(14));
        header.setBackground(shape(navy, 0));

        Button back = new Button(this);
        back.setText("‹");
        back.setTextSize(25);
        back.setTextColor(green);
        back.setAllCaps(false);
        back.setBackground(shape(Color.rgb(35, 28, 22), 10));
        header.addView(back, params(48, 46));
        back.setOnClickListener(v -> showDashboard(username));

        TextView title = makeText("الأوردرات  |  ORDERS", 18, green);
        title.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams titleParams =
                new LinearLayout.LayoutParams(0, -2, 1f);
        titleParams.leftMargin = dp(12);
        header.addView(title, titleParams);
        root.addView(header, params(-1, -2));

        ScrollView scroll = new ScrollView(this);
        LinearLayout body = column();
        body.setPadding(dp(16), dp(16), dp(16), dp(20));

        int totalOrders = 0;
        for (JSONObject rider : liveRiderDetails) {
            JSONArray orders = rider.optJSONArray("latestDeliveries");
            if (orders != null) totalOrders += orders.length();
        }

        TextView summary = makeText(
                "إجمالي الأوردرات المعروضة: " + totalOrders,
                16, Color.WHITE);
        summary.setTypeface(null, Typeface.BOLD);
        body.addView(summary, params(-1, -2));

        if (totalOrders == 0) {
            TextView empty = makeText(
                    "لا توجد أوردرات محفوظة. حدّث الروستر وانتظر اكتمال المزامنة.",
                    14, Color.rgb(190, 190, 190));
            empty.setPadding(dp(4), dp(18), dp(4), dp(18));
            body.addView(empty, params(-1, -2));
        }

        for (int i = 0; i < liveRiderDetails.size(); i++) {
            JSONObject rider = liveRiderDetails.get(i);
            JSONArray orders = rider.optJSONArray("latestDeliveries");
            if (orders == null) continue;

            String riderName = i < liveRiderNames.size()
                    ? liveRiderNames.get(i) : "مندوب";

            for (int j = 0; j < orders.length(); j++) {
                JSONObject order = orders.optJSONObject(j);
                if (order == null) continue;

                String code = order.optString("order_code",
                        order.optString("orderCode",
                                order.optString("code", ""))).trim();
                String pickup = order.optString("pickup_address",
                        order.optString("pickupAddress", "")).trim();
                String dropoff = order.optString("dropoff_address",
                        order.optString("dropoffAddress",
                                order.optString("delivery_address", ""))).trim();
                String status = order.optString("status", "").trim();

                LinearLayout card = column();
                card.setPadding(dp(14), dp(14), dp(14), dp(14));
                card.setBackground(shape(Color.rgb(22, 22, 22), 14));

                LinearLayout.LayoutParams cp = params(-1, -2);
                cp.topMargin = dp(10);
                body.addView(card, cp);

                TextView orderTitle = makeText(
                        code.isEmpty() ? "أوردر بدون رقم ظاهر" : "أوردر: " + code,
                        16, green);
                orderTitle.setTypeface(null, Typeface.BOLD);
                card.addView(orderTitle, params(-1, -2));
                card.addView(makeText(
                        "المندوب: " + riderName, 13, Color.WHITE),
                        params(-1, -2));

                if (!status.isEmpty()) {
                    card.addView(makeText(
                            "الحالة: " + status, 12,
                            Color.rgb(190, 190, 190)), params(-1, -2));
                }

                TextView pickupView = makeText(
                        "عنوان الاستلام: " +
                        (pickup.isEmpty() ? "غير متاح في البيانات" : pickup),
                        13, Color.WHITE);
                pickupView.setPadding(0, dp(8), 0, dp(4));
                pickupView.setLineSpacing(dp(3), 1f);
                card.addView(pickupView, params(-1, -2));

                TextView dropoffView = makeText(
                        "عنوان التسليم: " +
                        (dropoff.isEmpty() ? "غير متاح في البيانات" : dropoff),
                        13, Color.WHITE);
                dropoffView.setPadding(0, dp(4), 0, 0);
                dropoffView.setLineSpacing(dp(3), 1f);
                card.addView(dropoffView, params(-1, -2));
            }
        }

        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        Button home = new Button(this);
        home.setText("العودة للرئيسية");
        home.setAllCaps(false);
        home.setTextColor(Color.WHITE);
        home.setBackground(shape(green, 12));
        LinearLayout.LayoutParams hp = params(-1, 48);
        hp.setMargins(dp(16), dp(8), dp(16), dp(16));
        root.addView(home, hp);
        home.setOnClickListener(v -> showDashboard(username));

        setContentView(root);
    }

    private void showBookings(String username) {
        currentUsername = username;
        dashboardVisible = true;

        LinearLayout root = column();
        root.setBackgroundColor(background);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), dp(14), dp(16), dp(14));
        header.setBackground(shape(navy, 0));

        Button back = new Button(this);
        back.setText("‹");
        back.setTextSize(25);
        back.setTextColor(green);
        back.setAllCaps(false);
        back.setBackground(shape(Color.rgb(35, 28, 22), 10));
        header.addView(back, params(48, 46));
        back.setOnClickListener(v -> showDashboard(username));

        TextView title = makeText("الحجوزات  |  BOOKINGS", 18, green);
        title.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams titleParams =
                new LinearLayout.LayoutParams(0, -2, 1f);
        titleParams.leftMargin = dp(12);
        header.addView(title, titleParams);
        root.addView(header, params(-1, -2));

        LinearLayout body = column();
        body.setPadding(dp(16), dp(20), dp(16), dp(20));

        TextView heading = makeText("إدارة الحجوزات", 21, Color.WHITE);
        heading.setTypeface(null, Typeface.BOLD);
        body.addView(heading, params(-1, -2));

        TextView subtitle = makeText(
                "صفحة الحجوزات في ALABTAL",
                13, Color.rgb(190, 190, 190));
        LinearLayout.LayoutParams subtitleParams = params(-1, -2);
        subtitleParams.topMargin = dp(6);
        body.addView(subtitle, subtitleParams);

        LinearLayout info = column();
        info.setPadding(dp(16), dp(16), dp(16), dp(16));
        GradientDrawable infoBg = shape(Color.rgb(24, 21, 18), 14);
        infoBg.setStroke(dp(1), green);
        info.setBackground(infoBg);

        TextView infoTitle = makeText(
                "بانتظار ربط بيانات الحجوزات",
                16, green);
        infoTitle.setTypeface(null, Typeface.BOLD);
        info.addView(infoTitle, params(-1, -2));

        TextView infoText = makeText(
                "واجهة الحجوزات جاهزة. لعرض الإجمالي والمحجوز وغير المحجوز "
                + "والنسبة الفعلية، يجب تحديد مصدر بيانات الحجوزات من موقع اللوجستكس.",
                13, Color.WHITE);
        infoText.setLineSpacing(dp(4), 1f);
        LinearLayout.LayoutParams infoTextParams = params(-1, -2);
        infoTextParams.topMargin = dp(10);
        info.addView(infoText, infoTextParams);

        LinearLayout.LayoutParams infoParams = params(-1, -2);
        infoParams.topMargin = dp(20);
        body.addView(info, infoParams);

        root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1f));

        Button home = new Button(this);
        home.setText("العودة للرئيسية");
        home.setAllCaps(false);
        home.setTextColor(Color.WHITE);
        home.setBackground(shape(green, 12));
        LinearLayout.LayoutParams homeParams = params(-1, 48);
        homeParams.setMargins(dp(16), dp(8), dp(16), dp(16));
        root.addView(home, homeParams);
        home.setOnClickListener(v -> showDashboard(username));

        setContentView(root);
    }

    private void ensureRosterWebView() {
        if (rosterWebView != null) return;

        rosterWebView = new WebView(this);
        WebSettings settings = rosterWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        rosterWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                if (url != null && url.contains("/dashboard/rooster/review")) {
                    String probe =
                        "(function(){" +
                        "var a=performance.getEntriesByType('resource').map(function(x){" +
                        "try{var u=new URL(x.name);return u.origin+u.pathname;}catch(e){return '';}" +
                        "}).filter(function(x){return /api|booking|review|reservation|rooster/i.test(x);});" +
                        "return JSON.stringify(Array.from(new Set(a)).slice(-80));" +
                        "})()";

                    view.evaluateJavascript(probe, result -> {
                        String output = result == null ? "لم تظهر عناوين بيانات." :
                            result.replace("\\\\", "").replace("\\\"[", "[").replace("]\\\"", "]");
                        new android.app.AlertDialog.Builder(MainActivity.this)
                            .setTitle("Review API URLs")
                            .setMessage(output)
                            .setPositiveButton("حسناً", null)
                            .show();
                    });
                    return;
                }

                syncRosterData(view, false);
                startRosterAutoSync(view);
            }
        });
    }

    private void attachRosterWebView(LinearLayout root, boolean fullSize) {
        if (rosterWebView == null) return;

        android.view.ViewParent parent = rosterWebView.getParent();
        if (parent instanceof ViewGroup) {
            ((ViewGroup) parent).removeView(rosterWebView);
        }

        if (fullSize) {
            rosterWebView.setAlpha(1f);
            rosterWebView.setImportantForAccessibility(
                    View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
            root.addView(rosterWebView,
                    new LinearLayout.LayoutParams(-1, 0, 1));
        } else {
            // Keep the roster page attached while the dashboard is visible.
            rosterWebView.setAlpha(0f);
            rosterWebView.setImportantForAccessibility(
                    View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            root.addView(rosterWebView,
                    new LinearLayout.LayoutParams(1, 1));
        }
    }

    private void stopRosterAutoSync() {
        if (rosterAutoSync != null) {
            rosterHandler.removeCallbacks(rosterAutoSync);
            rosterAutoSync = null;
        }
    }

    private void startRosterAutoSync(WebView webView) {
        stopRosterAutoSync();
        rosterAutoSync = new Runnable() {
            @Override
            public void run() {
                if (webView.getParent() != null) {
                    syncRosterData(webView, false);
                    rosterRefreshTicks++;
                    if (rosterRefreshTicks >= 4) {
                        rosterRefreshTicks = 0;
                        webView.postDelayed(() -> {
                            if (webView.getParent() != null) {
                                webView.reload();
                            }
                        }, 2500);
                    }
                    rosterHandler.postDelayed(this, 30000);
                }
            }
        };
        rosterHandler.postDelayed(rosterAutoSync, 30000);
    }

    private void showRosterPage(String username) {
        dashboardVisible = false;
        currentUsername = username;
        ensureRosterWebView();
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
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, -2, 1);
        titleParams.leftMargin = dp(8);
        toolbar.addView(title, titleParams);

        Button syncButton = new Button(this);
        syncButton.setText("Sync");
        syncButton.setTextColor(Color.WHITE);
        syncButton.setAllCaps(false);
        toolbar.addView(syncButton, params(-2, 48));

        Button reviewButton = new Button(this);
        reviewButton.setText("Review");
        reviewButton.setTextColor(Color.WHITE);
        reviewButton.setAllCaps(false);
        toolbar.addView(reviewButton, params(-2, 48));

        reviewButton.setOnClickListener(v ->
                rosterWebView.loadUrl(
                        "https://eg.me.logisticsbackoffice.com/dashboard/rooster/review?page=1&sizePerPage=10"));

        root.addView(toolbar, params(-1, -2));

        syncButton.setOnClickListener(v -> syncRosterData(rosterWebView, true));
        setContentView(root);
        attachRosterWebView(root, true);
        if (!rosterPageLoaded) {
            rosterPageLoaded = true;
            rosterWebView.loadUrl("https://eg.me.logisticsbackoffice.com/dashboard/rooster/live-3pl");
        } else {
            syncRosterData(rosterWebView, false);
            if (rosterAutoSync == null) startRosterAutoSync(rosterWebView);
        }
    }

    private void syncRosterData(WebView webView, boolean notify) {
        if (webView == null) return;

        String script =
                "window.__alabtalRosterResult=null;(async function() {" +
                "try {" +
                "if (!location.hostname.endsWith('logisticsbackoffice.com'))" +
                " return JSON.stringify({ok:false,error:'wrong_page'});" +
                "var all=[]; var page=0; var size=100; var maxPages=50;" +
                "function listOf(j) {" +
                "if(Array.isArray(j))return j;" +
                "if(!j||typeof j!=='object')return null;" +
                "var paths=[j.riders,j.items,j.content,j.results,j.data," +
                "j.data&&j.data.riders,j.data&&j.data.items," +
                "j.data&&j.data.content,j.data&&j.data.results," +
                "j.result&&j.result.riders,j.result&&j.result.items];" +
                "for(var i=0;i<paths.length;i++)if(Array.isArray(paths[i]))return paths[i];" +
                "return null;" +
                "}" +
                "function val(o, paths) {" +
                "for(var i=0;i<paths.length;i++) {" +
                "var v=o; var parts=paths[i].split('.');" +
                "for(var k=0;k<parts.length&&v!=null;k++)v=v[parts[k]];" +
                "if(v!==undefined&&v!==null&&v!=='')return v;" +
                "} return '';" +
                "}" +
                "function text(v) {" +
                "if(v===null||v===undefined)return '';" +
                "if(typeof v==='object')return JSON.stringify(v);" +
                "return String(v);" +
                "}" +
                "function percent(v) {" +
                "if(v===''||v===null||v===undefined)return '';" +
                "var n=Number(v); return isFinite(n)?(n<=1&&n>=0?" +
                "(n*100).toFixed(1):n.toFixed(1))+'%':text(v);" +
                "}" +
                "function duration(v) {" +
                "if(v===null||v===undefined||v==='')return '';" +
                "if(typeof v==='string'&&/^\\d{1,}:\\d{1,2}:\\d{1,2}$/.test(v.trim()))return v.trim();" +
                "if(typeof v==='string'&&isNaN(Number(v)))return text(v);" +
                "var n=Number(v); if(!isFinite(n)||n<0)return text(v);" +
                "n=Math.floor(n); var h=Math.floor(n/3600);" +
                "var m=Math.floor((n%3600)/60); var sec=n%60;" +
                "return String(h).padStart(2,'0')+':'+" +
                "String(m).padStart(2,'0')+':'+String(sec).padStart(2,'0');" +
                "}" +
                "for(;page<maxPages;page++) {" +
                "var url='/api/rider-live-operations/v1/external/city/214/riders?page='+page+'&size='+size;" +
                "var res=await fetch(url,{method:'GET',credentials:'include'," +
                "headers:{'Accept':'application/json'}});" +
                "if(!res.ok)return JSON.stringify({ok:false,status:res.status,page:page});" +
                "var j=await res.json(); var rows=listOf(j);" +
                "if(!rows)return JSON.stringify({ok:false,error:'unexpected_response',page:page});" +
                "if(rows.length===0)break;" +
                "all=all.concat(rows);" +
                "var totalPages=val(j,['total_pages','totalPages','data.total_pages','data.totalPages']);" +
                "if(totalPages!==''&&page+1>=Number(totalPages))break;" +
                "if(rows.length<size)break;" +
                "}" +
                "var mapped=[];" +
                "var shiftRows=[];var shiftMap={};" +
                "function findShiftArray(o,depth){if(depth>6||o==null)return null;if(Array.isArray(o)){for(var z=0;z<o.length;z++){var q=o[z];if(q&&typeof q==='object'&&q.employee_id!=null&&q.start&&q.end)return o;}for(var a=0;a<o.length;a++){var nested=findShiftArray(o[a],depth+1);if(nested)return nested;}return null;}if(typeof o==='object'){var ks=Object.keys(o);for(var b=0;b<ks.length;b++){var found=findShiftArray(o[ks[b]],depth+1);if(found)return found;}}return null;}" +
                "try{var clock=new Date();var from=new Date(clock.getTime()-2*86400000);var to=new Date(clock.getTime()+8*86400000);for(var sp=0;sp<50;sp++){var su='/api/rooster/v3/shifts?city_id=214&distinct_day_plans=false&end_at='+encodeURIComponent(to.toISOString())+'&page='+sp+'&size=100&start_at='+encodeURIComponent(from.toISOString())+'&with_evaluations=true&with_field_id_number&with_time_zone=Africa%2FCairo';var sr=await fetch(su,{method:'GET',credentials:'include',headers:{'Accept':'application/json'}});if(!sr.ok)break;var sj=await sr.json();var srows=findShiftArray(sj,0);if(!srows||!srows.length)break;shiftRows=shiftRows.concat(srows);if(srows.length<100)break;}}catch(se){}" +
                "for(var si=0;si<shiftRows.length;si++){var sh=shiftRows[si]||{};var eid=text(sh.employee_id);if(!eid)continue;if(!shiftMap[eid])shiftMap[eid]=[];shiftMap[eid].push(sh);}" +
                "for(var i=0;i<all.length;i++) {" +
                "var r=all[i]||{};" +
                "var name=text(val(r,['name','full_name','employee_name'])).trim();" +
                "if(!name)continue;" +
                "var status=text(val(r,['status','rider_status']));" +
                "var bal=val(r,['wallet_info.balance','wallet.balance','wallet_balance']);" +
                "var perf=val(r,['performance.utilization_rate','utilization_rate','performance']);" +
                "if(perf&&typeof perf==='object')perf=val(perf,['utilization_rate','rate','value']);" +
                "var acc=val(r,['acceptance_rate','performance.acceptance_rate']);" +
                "var worked=val(r,['time_spent.worked_seconds','time_spent.workedSeconds'," +
                "'time_spent.total_worked_seconds','worked_seconds','workedSeconds'," +
                "'worked_time_seconds','worked_time','total_worked_seconds']);" +
                "var late=val(r,['time_spent.late_seconds','late_seconds']);" +
                "var breaks=val(r,['time_spent.break_seconds','break_seconds']);" +
                "var loc=val(r,['current_location']);" +
                "var lat=val(r,['current_location.latitude','latitude']);" +
                "var lon=val(r,['current_location.longitude','longitude']);" +
                "var deliveries=val(r,['deliveries_info.latest_deliveries','latest_deliveries']);" +
                "var orderLocation='';var latestOrderCode='';" +
                "var pickupAddress='';var dropoffAddress='';" +
                "if(Array.isArray(deliveries)&&deliveries.length){" +
                "var od=deliveries[0]||{};" +
                "latestOrderCode=text(val(od,['order_code','orderCode','code']));" +
                "pickupAddress=text(val(od,['pickup_address','pickupAddress']));" +
                "dropoffAddress=text(val(od,['dropoff_address','dropoffAddress','delivery_address']));" +
                "var locParts=[];" +
                "if(latestOrderCode)locParts.push('أوردر '+latestOrderCode);" +
                "if(dropoffAddress)locParts.push('تسليم: '+dropoffAddress);" +
                "if(pickupAddress)locParts.push('استلام: '+pickupAddress);" +
                "orderLocation=locParts.join(' | ');}" +
                "var shift=text(val(r,['current_session','session','shift.name','shift_info.name']));" +
                "if(!shift) {" +
                "var ss=val(r,['shift_start','shift.start_time','shift_info.start_time']);" +
                "var se=val(r,['shift_end','shift.end_time','shift_info.end_time']);" +
                "if(ss||se)shift=text(ss)+(se?' - '+text(se):'');" +
                "}" +
                "var d={employeeId:text(val(r,['employee_id','id']))," +
                "zone:text(val(r,['zone.name','zone','starting_point.name']))," +
                "startingPoint:text(val(r,['starting_point.name','starting_point_name']))," +
                "session:shift,wallet:text(bal),performance:percent(perf)," +
                "acceptance:percent(acc),worked:duration(worked)," +
                "workedSeconds:(worked!==''&&isFinite(Number(worked))?Number(worked):-1)," +
                "late:duration(late),breakTime:duration(breaks)," +
                "location:(lat!==''&&lon!==''?text(lat)+', '+text(lon):text(loc))," +
                "latitude:text(lat),longitude:text(lon)," +
                "orderLocation:orderLocation,latestOrderCode:latestOrderCode," +
                "pickupAddress:pickupAddress,dropoffAddress:dropoffAddress," +
                "latestDeliveries:(Array.isArray(deliveries)?deliveries:[])," +
                "activeDeliveriesCount:Number(val(r,['deliveries_info.active_deliveries_count'," +
                "'active_deliveries_count']))||((Array.isArray(deliveries))?deliveries.length:0)," +
                "hasActiveDeliveries:Number(val(r,['deliveries_info.active_deliveries_count'," +
                "'active_deliveries_count']))>0||((Array.isArray(deliveries))&&deliveries.length>0)};" +
                "var candidates=(shiftMap[d.employeeId]||[]).filter(function(x){return x.end&&new Date(x.end).getTime()>=Date.now();}).sort(function(a,b){return new Date(a.start).getTime()-new Date(b.start).getTime();});" +
                "if(candidates.length){var ps=candidates[0];d.plannedShiftStart=text(ps.start);d.plannedShiftEnd=text(ps.end);d.plannedShiftState=text(ps.state);d.plannedShiftTag=text(ps.tag);}" +
                "mapped.push({name:name,state:status||'Unknown',detail:d});" +
                "}" +
                "var sample=all[0]||{};" +
                "var sampleDeliveries=sample.deliveries_info&&sample.deliveries_info.latest_deliveries;" +
                "var schema={root:Object.keys(sample),timeSpent:Object.keys(sample.time_spent||{}),deliveriesInfo:Object.keys(sample.deliveries_info||{}),firstDelivery:Object.keys(Array.isArray(sampleDeliveries)&&sampleDeliveries.length?sampleDeliveries[0]||{}:{})};" +
                "return JSON.stringify({ok:true,riders:mapped,count:mapped.length,pages:page+1,schema:schema});" +
                "} catch(e) {return JSON.stringify({ok:false,error:String(e)});}" +
                "})().then(function(v){window.__alabtalRosterResult=v;},function(e){window.__alabtalRosterResult=JSON.stringify({ok:false,error:String(e)});});" +
                "'started'";

        webView.evaluateJavascript(script, ignored ->
                pollRosterApiResult(webView, notify, 0));
    }

    private void pollRosterApiResult(WebView webView, boolean notify, int attempt) {
        webView.evaluateJavascript("window.__alabtalRosterResult || null", result -> {
            if (result == null || "null".equals(result)) {
                if (attempt < 59) {
                    webView.postDelayed(() -> pollRosterApiResult(webView, notify, attempt + 1), 500);
                } else if (notify) {
                    Toast.makeText(this, "API timed out waiting for response.", Toast.LENGTH_LONG).show();
                }
                return;
            }
            try {
                Object value = new org.json.JSONTokener(result).nextValue();
                if (!(value instanceof String)) return;
                handleRosterApiResult((String) value, notify);
            } catch (Exception e) {
                if (notify) Toast.makeText(this,
                        "Could not decode API result.",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleRosterApiResult(String result, boolean notify) {
            try {
                JSONObject response = new JSONObject(result);
                if (!response.optBoolean("ok", false)) {
                    rosterSyncSuccessful = false;
                    int status = response.optInt("status", 0);
                    rosterSyncStatus = status > 0
                            ? "فشل الاتصال (HTTP " + status + ")"
                            : "فشلت المزامنة";
                    if (dashboardVisible && !currentUsername.isEmpty()) {
                        showDashboard(currentUsername);
                    }
                    if (notify) {
                        String error = response.optString("error", "");
                        String message = status > 0
                                ? "Logistics API HTTP " + status
                                : "Logistics API error: " + error;
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    }
                    return;
                }

                if (notify && response.has("schema")) {
                    JSONObject schema = response.optJSONObject("schema");
                    if (schema != null) {
                        new android.app.AlertDialog.Builder(this)
                                .setTitle("API field names")
                                .setMessage(schema.toString(2))
                                .setPositiveButton("OK", null)
                                .show();
                    }
                }

                JSONArray riders = response.optJSONArray("riders");
                if (riders == null || riders.length() == 0) {
                    if (notify) Toast.makeText(this,
                            "API connected, but no rider records were found.",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                ArrayList<String> names = new ArrayList<>();
                ArrayList<String> states = new ArrayList<>();
                ArrayList<JSONObject> details = new ArrayList<>();

                for (int i = 0; i < riders.length(); i++) {
                    JSONObject item = riders.optJSONObject(i);
                    if (item == null) continue;
                    String name = item.optString("name", "").trim();
                    if (name.isEmpty()) continue;
                    names.add(name);
                    states.add(item.optString("state", "Unknown"));
                    JSONObject detail = item.optJSONObject("detail");
                    details.add(detail == null ? new JSONObject() : detail);
                }

                if (names.isEmpty()) {
                    if (notify) Toast.makeText(this,
                            "API response contained no usable rider names.",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                boolean changed = !liveRiderNames.equals(names)
                        || !liveRiderStates.equals(states)
                        || !liveRiderDetails.toString().equals(details.toString());

                liveRiderNames.clear();
                liveRiderNames.addAll(names);
                liveRiderStates.clear();
                liveRiderStates.addAll(states);
                liveRiderDetails.clear();
                liveRiderDetails.addAll(details);

                rosterSyncSuccessful = true;
                rosterSyncStatus = "متصل • تمت المزامنة بنجاح";
                lastRosterSyncLabel = new java.text.SimpleDateFormat(
                        "HH:mm:ss", java.util.Locale.getDefault())
                        .format(new java.util.Date());

                JSONArray savedNames = new JSONArray();
                JSONArray savedStates = new JSONArray();
                JSONArray savedDetails = new JSONArray();
                for (String name : names) savedNames.put(name);
                for (String state : states) savedStates.put(state);
                for (JSONObject detail : details) savedDetails.put(detail);

                getSharedPreferences("live_roster", MODE_PRIVATE).edit()
                        .putString("names", savedNames.toString())
                        .putString("states", savedStates.toString())
                        .putString("details", savedDetails.toString())
                        .putString("last_sync", lastRosterSyncLabel)
                        .apply();

                rosterSyncAttempts = 0;
                if (notify) Toast.makeText(this,
                        "API synced " + names.size() + " riders. Last sync: "
                                + lastRosterSyncLabel,
                        Toast.LENGTH_LONG).show();

                if (dashboardVisible && !currentUsername.isEmpty() && changed) {
                    showDashboard(currentUsername);
                }
            } catch (Exception e) {
                if (notify) Toast.makeText(this,
                        "Could not parse Logistics API response.",
                        Toast.LENGTH_LONG).show();
            }
    }

}
