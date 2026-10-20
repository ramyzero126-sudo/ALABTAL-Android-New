
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
        stopRosterAutoSync();
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
                    rosterHandler.postDelayed(this, 30000);
                }
            }
        };
        rosterHandler.postDelayed(rosterAutoSync, 30000);
    }

    private void showRosterPage(String username) {
        stopRosterAutoSync();
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

        WebView webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                syncRosterData(view, false);
                startRosterAutoSync(view);
            }
        });

        syncButton.setOnClickListener(v -> syncRosterData(webView, true));
        root.addView(webView, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        webView.loadUrl("https://eg.me.logisticsbackoffice.com/dashboard/rooster/live-3pl");
    }

    private void syncRosterData(WebView webView, boolean notify) {
        if (notify) {
            String inspectScript =
                    "(function() {" +
                    "function visible(e) {var r=e.getBoundingClientRect(),s=getComputedStyle(e);" +
                    "return r.width>0 && r.height>0 && s.display!=='none' && s.visibility!=='hidden';}" +
                    "function clean(s){return (s||'').replace(/\\s+/g,' ').trim();}" +
                    "var sels=['[role=dialog]','[aria-modal=true]','dialog'," +
                    "[class*=modal],[class*=Modal],[class*=dialog],[class*=Dialog]'];" +
                    "var found=[];" +
                    "sels.forEach(function(sel){" +
                    "try{document.querySelectorAll(sel).forEach(function(e){" +
                    "if(!visible(e))return;" +
                    "var t=clean(e.innerText||e.textContent);" +
                    "if(t && /HF_[A-Z0-9]+|Accepted Order|Picked up|Near drop off|Delivered/i.test(t)){" +
                    "var id=e.id||'';var cl=typeof e.className==='string'?e.className:'';" +
                    "var item='TAG: '+e.tagName+' ID: '+id+' CLASS: '+cl+'\\nTEXT:\\n'+t;" +
                    "if(!found.includes(item))found.push(item);" +
                    "}" +
                    "});}catch(err){}" +
                    "});" +
                    "if(!found.length){" +
                    "var all=Array.from(document.querySelectorAll('body *')).filter(visible);" +
                    "all.forEach(function(e){" +
                    "var t=clean(e.innerText||e.textContent);" +
                    "if(!t||t.length>2500||!/HF_[A-Z0-9]+/.test(t))return;" +
                    "var par=e.parentElement;" +
                    "if(par&&clean(par.innerText||par.textContent).length<2500)return;" +
                    "found.push('TAG: '+e.tagName+' CLASS: '+(typeof e.className==='string'?e.className:'')+'\\nTEXT:\\n'+t);" +
                    "});" +
                    "}" +
                    "return found.slice(0,6).join('\\n\\n==========\\n\\n').slice(0,6000);" +
                    "})()";
            webView.evaluateJavascript(inspectScript, result -> {
                String text = result == null ? "" : result;
                if (text.length() > 2) {
                    try {
                        Object decoded = new org.json.JSONTokener(text).nextValue();
                        text = decoded == null ? "" : decoded.toString();
                    } catch (Exception ignored) {}
                    final String inspectionText = text;
                    new android.app.AlertDialog.Builder(this)
                            .setTitle("فحص تفاصيل الأوردر")
                            .setMessage(inspectionText)
                            .setPositiveButton("نسخ", (dialog, which) -> {
                                android.content.ClipboardManager cm =
                                        (android.content.ClipboardManager)
                                        getSystemService(CLIPBOARD_SERVICE);
                                cm.setPrimaryClip(android.content.ClipData.newPlainText(
                                        "RosterOrderInspect", inspectionText));
                                Toast.makeText(this, "تم النسخ", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("إغلاق", null)
                            .show();
                } else {
                    Toast.makeText(this,
                            "مش لاقي نص مراحل الأوردر ظاهر في الصفحة.",
                            Toast.LENGTH_LONG).show();
                }
            });
            return;
        }
        String script =
                "(function() {" +
                "var t = document.querySelector('table.cape-table');" +
                "if (!t) return [];" +
                "function txt(e) {return (e.innerText || e.textContent || '').trim().replace(/\\s+/g,' ');}" +
                "if(!window.__alabtalOrderTracker){window.__alabtalOrderTracker=true;document.addEventListener('click',function(ev){var tr=ev.target.closest('tr');if(!tr||!tr.closest('table.cape-table'))return;var first=tr.querySelector('td');if(!first)return;var raw=(first.innerText||first.textContent||'').split(/\\n+/).map(function(x){return x.trim();});var nm='';for(var j=0;j<raw.length;j++){if(/^Name\\s*:/i.test(raw[j])){nm=raw[j].replace(/^Name\\s*:\\s*/i,'');break;}}if(!nm)nm=txt(first);if(nm)window.__alabtalLastOrderRider=nm;},true);}" +
                "var modalText='';var modalRoot=null;var sels=['[role=dialog]','[aria-modal=true]','dialog','[class*=modal],[class*=Modal],[class*=dialog],[class*=Dialog]'];sels.forEach(function(sel){try{document.querySelectorAll(sel).forEach(function(e){var r=e.getBoundingClientRect(),cs=getComputedStyle(e);if(r.width<=0||r.height<=0||cs.display==='none'||cs.visibility==='hidden')return;var z=txt(e);if(z.length>modalText.length&&/HF_[A-Z0-9]+/.test(z)){modalText=z;modalRoot=e;}});}catch(e){}});" +
                "var oid=(modalText.match(/HF_[A-Z0-9]+/)||[''])[0];var orderRider=window.__alabtalLastOrderRider||'';" +
                "var stageNames=['Accepted Order','Picked up','Near drop off','Delivered'];" +
                "var stageNodes=modalRoot?Array.from(modalRoot.querySelectorAll('*')).filter(function(e){var tx=(e.innerText||e.textContent||'').trim().replace(/\\s+/g,' ');var r=e.getBoundingClientRect(),cs=getComputedStyle(e);return r.width>0&&r.height>0&&cs.display!=='none'&&cs.visibility!=='hidden'&&stageNames.indexOf(tx)>=0;}):[];" +
                "function stageFlag(e){var out='';for(var n=e,i=0;n&&i<5&&modalRoot.contains(n);n=n.parentElement,i++){out+=' '+(typeof n.className==='string'?n.className:'')+' '+(n.getAttribute('aria-current')||'')+' '+(n.getAttribute('data-state')||'')+' '+(n.getAttribute('aria-checked')||'');}return out;}" +
                "var activeNodes=stageNodes.filter(function(e){return /active|current|in-progress|selected/i.test(stageFlag(e));});" +
                "var completedNodes=stageNodes.filter(function(e){return /complete|completed|done|success/i.test(stageFlag(e));});" +
                "var stageNode=activeNodes.length?activeNodes[activeNodes.length-1]:(completedNodes.length?completedNodes[completedNodes.length-1]:null);" +
                "var detectedStage=stageNode?(stageNode.innerText||stageNode.textContent||'').trim():'';" +
                "var hs = Array.from(t.querySelectorAll('thead th')).map(txt);" +
                "function idx(re, fallback) {for (var i=0;i<hs.length;i++) if(re.test(hs[i])) return i; return fallback;}" +
                "function val(c, re, fallback) {var i=idx(re,fallback); return i<c.length ? txt(c[i]) : '';}" +
                "return Array.from(t.querySelectorAll('tbody tr')).map(function(r) {" +
                "var c=Array.from(r.querySelectorAll('td'));" +
                "if(c.length<10) return null;" +
                "var raw=(c[0].innerText || c[0].textContent || '').split(/\\n+/).map(function(x){return x.trim();});" +
                "var nm=''; for(var j=0;j<raw.length;j++){if(/^Name\\s*:/i.test(raw[j])){nm=raw[j].replace(/^Name\\s*:\\s*/i,'');break;}}" +
                "if(!nm) nm=txt(c[0]); if(!nm) return null;" +
                "return {name:nm," +
                "state:val(c,/rider state/i,9)||'Unknown'," +
                "session:val(c,/current session/i,1)," +
                "performance:val(c,/performance/i,2)," +
                "worked:val(c,/time worked/i,3)," +
                "acceptance:val(c,/acceptance rate/i,4)," +
                "wallet:val(c,/wallet balance/i,5)," +
                "breaks:val(c,/^breaks$/i,6)," +
                "breakTime:val(c,/breaks time/i,7)," +
                "late:val(c,/late time/i,8)," +
                "location:val(c,/location/i,10)," +
                "orderId:(orderRider===nm?oid:'')," +
                "orderStage:(orderRider===nm?detectedStage:'')," +
                "orderDetails:(orderRider===nm?modalText:'')};" +
                "}).filter(function(x){return x!==null;});" +
                "})()";

        webView.evaluateJavascript(script, result -> {
            try {
                JSONArray rows = new JSONArray(result == null ? "[]" : result);
                if (rows.length() == 0) {
                    if (!notify && rosterSyncAttempts < 10) {
                        rosterSyncAttempts++;
                        webView.postDelayed(() -> syncRosterData(webView, false), 1500);
                        return;
                    }
                    rosterSyncAttempts = 0;
                    if (notify) Toast.makeText(this,
                            "No roster rows found. Wait for the table, then tap Sync.",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                ArrayList<String> names = new ArrayList<>();
                ArrayList<String> states = new ArrayList<>();
                ArrayList<JSONObject> details = new ArrayList<>();

                for (int i = 0; i < rows.length(); i++) {
                    JSONObject row = rows.getJSONObject(i);
                    String name = row.optString("name", "").trim();
                    if (name.isEmpty()) continue;

                    names.add(name);
                    states.add(row.optString("state", "Unknown"));
                    JSONObject detail = new JSONObject();
                    String[] keys = {"session", "performance", "worked", "acceptance",
                            "wallet", "breaks", "breakTime", "late", "location",
                            "orderId", "orderStage", "orderDetails"};
                    for (String key : keys) {
                        detail.put(key, row.optString(key, ""));
                    }
                    details.add(detail);
                }

                if (names.isEmpty()) {
                    if (notify) Toast.makeText(this, "No rider names found in the table.",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                rosterSyncAttempts = 0;
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

                if (notify) Toast.makeText(this,
                        "Updated " + names.size() + " riders. Last sync: "
                                + lastRosterSyncLabel + ". Tap Back to view dashboard.",
                        Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                if (notify) Toast.makeText(this, "Could not read roster table.",
                        Toast.LENGTH_LONG).show();
            }
        });
    }


}
