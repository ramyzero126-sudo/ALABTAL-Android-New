
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
    private WebView rosterWebView;
    private boolean rosterPageLoaded = false;
    private boolean dashboardVisible = false;
    private String currentUsername = "";

    private int rosterSyncAttempts = 0;
    private int rosterRefreshTicks = 0;


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

        private void addRiderCard(LinearLayout list, String rider,
                              String status, JSONObject detail) {
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

        if (detail == null) detail = new JSONObject();

        LinearLayout card = column();
        card.setPadding(dp(9), dp(8), dp(9), dp(8));
        android.graphics.drawable.GradientDrawable cardBackground =
                new android.graphics.drawable.GradientDrawable();
        cardBackground.setColor(Color.rgb(16, 16, 16));
        cardBackground.setCornerRadius(dp(12));
        cardBackground.setStroke(dp(1), Color.rgb(249, 115, 22));
        card.setBackground(cardBackground);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = makeText(rider, 13, Color.WHITE);
        name.setTypeface(null, Typeface.BOLD);
        top.addView(name, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView badge = makeText(state, 10, badgeText);
        badge.setTypeface(null, Typeface.BOLD);
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(dp(7), dp(3), dp(7), dp(3));
        badge.setBackground(shape(badgeBackground, 9));
        top.addView(badge, params(-2, -2));
        card.addView(top, params(-1, -2));

        String session = detail.optString("session", "");
        String wallet = detail.optString("wallet", "");
        String performance = detail.optString("performance", "");
        String acceptance = detail.optString("acceptance", "");
        String worked = detail.optString("worked", "");

        String line1 = "الشيفت: " + (session.isEmpty() ? "—" : session)
                + "   •   المحفظة: " + (wallet.isEmpty() ? "—" : wallet);
        String line2 = "التوصيلات/الأداء: "
                + (performance.isEmpty() ? "—" : performance)
                + "   •   القبول: " + (acceptance.isEmpty() ? "—" : acceptance);
        String line3 = "وقت العمل: " + (worked.isEmpty() ? "—" : worked);

        TextView metrics = makeText(line1 + "\n" + line2 + "\n" + line3,
                10, muted);
        metrics.setMaxLines(3);
        LinearLayout.LayoutParams metricParams = params(-1, -2);
        metricParams.topMargin = dp(5);
        card.addView(metrics, metricParams);

        String orderId = detail.optString("orderId", "").trim();
        String orderStage = detail.optString("orderStage", "").trim();
        String orderDetails = detail.optString("orderDetails", "").trim();
        if (!orderId.isEmpty() || !orderStage.isEmpty() || !orderDetails.isEmpty()) {
            TextView orderView = makeText(
                    "الأوردر: " + (orderId.isEmpty() ? "—" : orderId)
                    + "\nالمرحلة: " + (orderStage.isEmpty() ? "غير متاحة" : orderStage)
                    + (orderDetails.isEmpty() ? "" : "\n" + orderDetails),
                    10, Color.rgb(249, 115, 22));
            orderView.setMaxLines(4);
            orderView.setPadding(dp(8), dp(6), dp(8), dp(6));
            orderView.setBackground(shape(Color.rgb(29, 29, 29), 8));
            LinearLayout.LayoutParams orderParams = params(-1, -2);
            orderParams.topMargin = dp(5);
            card.addView(orderView, orderParams);
        }

        LinearLayout.LayoutParams cardParams = params(-1, -2);
        cardParams.bottomMargin = dp(8);
        list.addView(card, cardParams);
    }

    private void showDashboard(String username) {
        currentUsername = username;
        dashboardVisible = true;
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

        TextView hint = makeText("آخر تحديث ناجح: " + lastRosterSyncLabel,
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
                addRiderCard(content, liveRiderNames.get(i), riderState,
                            i < liveRiderDetails.size()
                                    ? liveRiderDetails.get(i) : new JSONObject());
            }
        }

        Button logout = new Button(this);
        logout.setText("تسجيل الخروج  |  LOG OUT");
        logout.setTextColor(Color.WHITE);
        logout.setAllCaps(false);
        logout.setBackground(shape(Color.rgb(29, 44, 65), 12));
        logout.setOnClickListener(v -> {
            stopRosterAutoSync();
            dashboardVisible = false;
            auth.signOut();
            showLogin();
        });

        LinearLayout.LayoutParams logoutParams = params(-1, 48);
        logoutParams.topMargin = dp(12);
        content.addView(logout, logoutParams);

        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
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
                "var n=Number(v); if(!isFinite(n)||n<0)return text(v);" +
                "n=Math.floor(n); var h=Math.floor(n/3600);" +
                "var m=Math.floor((n%3600)/60);" +
                "return h+'h '+m+'m';" +
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
                "for(var i=0;i<all.length;i++) {" +
                "var r=all[i]||{};" +
                "var name=text(val(r,['name','full_name','employee_name'])).trim();" +
                "if(!name)continue;" +
                "var status=text(val(r,['status','rider_status']));" +
                "var bal=val(r,['wallet_info.balance','wallet.balance','wallet_balance']);" +
                "var perf=val(r,['performance.utilization_rate','utilization_rate','performance']);" +
                "if(perf&&typeof perf==='object')perf=val(perf,['utilization_rate','rate','value']);" +
                "var acc=val(r,['acceptance_rate','performance.acceptance_rate']);" +
                "var worked=val(r,['time_spent.worked_seconds','worked_seconds','worked_time']);" +
                "var late=val(r,['time_spent.late_seconds','late_seconds']);" +
                "var breaks=val(r,['time_spent.break_seconds','break_seconds']);" +
                "var loc=val(r,['current_location']);" +
                "var lat=val(r,['current_location.latitude','latitude']);" +
                "var lon=val(r,['current_location.longitude','longitude']);" +
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
                "late:duration(late),breakTime:duration(breaks)," +
                "location:(lat!==''&&lon!==''?text(lat)+', '+text(lon):text(loc))," +
                "latitude:text(lat),longitude:text(lon)," +
                "hasActiveDeliveries:Number(val(r,['deliveries_info.active_deliveries_count'," +
                "'active_deliveries_count']))>0};" +
                "mapped.push({name:name,state:status||'Unknown',detail:d});" +
                "}" +
                "return JSON.stringify({ok:true,riders:mapped,count:mapped.length,pages:page+1});" +
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
                    if (notify) {
                        int status = response.optInt("status", 0);
                        String error = response.optString("error", "");
                        String message = status > 0
                                ? "Logistics API HTTP " + status
                                : "Logistics API error: " + error;
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    }
                    return;
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
