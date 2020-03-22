package com.hawerner.passmanager;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.FrameLayout;

import java.util.Stack;

public class MyAccessibilityService extends AccessibilityService {

    FrameLayout mLayout;
    int deepForPassword = -1;
    Stack<Integer> childForPassword = new Stack<>();

    String username = "";
    String password = "";

    String currentPackageName = "";
    String waitPackageName = "";
    final String browserPackageName = "hawerner.browser";

    @Override
    public void onServiceConnected(){
        super.onServiceConnected();
        Log.i("Accessibility", "onServiceConnected");
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.flags = AccessibilityServiceInfo.DEFAULT |
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS |
                AccessibilityServiceInfo.FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY |
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED | AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED | AccessibilityEvent.TYPE_WINDOWS_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        setServiceInfo(info);
    }

    private String getEventTypeString(int eventType) {
        switch (eventType) {
            case AccessibilityEvent.TYPE_ANNOUNCEMENT:
                return "TYPE_ANNOUNCEMENT";
            case AccessibilityEvent.TYPE_GESTURE_DETECTION_END:
                return "TYPE_GESTURE_DETECTION_END";
            case AccessibilityEvent.TYPE_GESTURE_DETECTION_START:
                return "TYPE_GESTURE_DETECTION_START";
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                return "TYPE_NOTIFICATION_STATE_CHANGED";
            case AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_END:
                return "TYPE_TOUCH_EXPLORATION_GESTURE_END";
            case AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_START:
                return "TYPE_TOUCH_EXPLORATION_GESTURE_START";
            case AccessibilityEvent.TYPE_TOUCH_INTERACTION_END:
                return "TYPE_TOUCH_INTERACTION_END";
            case AccessibilityEvent.TYPE_TOUCH_INTERACTION_START:
                return "TYPE_TOUCH_INTERACTION_START";
            case AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED:
                return "TYPE_VIEW_ACCESSIBILITY_FOCUSED";
            case AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED:
                return "TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED";
            case AccessibilityEvent.TYPE_VIEW_CLICKED:
                return "TYPE_VIEW_CLICKED";
            case AccessibilityEvent.TYPE_VIEW_FOCUSED:
                return "TYPE_VIEW_FOCUSED";
            case AccessibilityEvent.TYPE_VIEW_HOVER_ENTER:
                return "TYPE_VIEW_HOVER_ENTER";
            case AccessibilityEvent.TYPE_VIEW_HOVER_EXIT:
                return "TYPE_VIEW_HOVER_EXIT";
            case AccessibilityEvent.TYPE_VIEW_LONG_CLICKED:
                return "TYPE_VIEW_LONG_CLICKED";
            case AccessibilityEvent.TYPE_VIEW_SCROLLED:
                return "TYPE_VIEW_SCROLLED";
            case AccessibilityEvent.TYPE_VIEW_SELECTED:
                return "TYPE_VIEW_SELECTED";
            case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
                return "TYPE_VIEW_TEXT_CHANGED";
            case AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED:
                return "TYPE_VIEW_TEXT_SELECTION_CHANGED";
            case AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY:
                return "TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY";
            case AccessibilityEvent.TYPE_WINDOWS_CHANGED:
                return "TYPE_WINDOWS_CHANGED";
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                return "TYPE_WINDOW_CONTENT_CHANGED";
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                return "TYPE_WINDOW_STATE_CHANGED";
        }
        return String.format("us", "unknown (%d)", eventType);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        //Log.v("Accessibility", "onAccessibilityEvent called");
        if (accessibilityEvent.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            ComponentName componentName = new ComponentName(
                    accessibilityEvent.getPackageName().toString(),
                    accessibilityEvent.getClassName().toString()
            );
            try {
                if (accessibilityEvent.getSource().getPackageName().toString().equals(getApplicationContext().getPackageName())) {
                    //hideFloating(); //TODO: Zasto kad se odkomentarise, baguje
                    return;
                }
                Log.i("Accessibility", accessibilityEvent.getSource().getPackageName().toString());
                currentPackageName = accessibilityEvent.getSource().getPackageName().toString();
            }catch (Exception ignored){}
            //Toast.makeText(getApplicationContext(), "Test", Toast.LENGTH_LONG).show();
            try{
                hideFloating();
            }catch (Exception ignored){}
            try{
                checkFloatingIfNeeded(getRootInActiveWindow());
            }
            catch (Exception e){
                if (e.getMessage().equals("password polje")) {
                    showFloating();
                }
                else{
                    Log.i("Accessibility", e.toString());
                    e.printStackTrace();
                }
            }
        }
    }

    private void checkFloatingIfNeeded(AccessibilityNodeInfo node) throws Exception {
        if (currentPackageName.equals(getApplicationContext().getPackageName()) || getApplicationContext().getPackageName().equals("com.android.systemui") || currentPackageName.equals(browserPackageName)) {
            return;
        }
        checkFloatingIfNeeded(node, 0);
    }
    private void checkFloatingIfNeeded(AccessibilityNodeInfo node, int deep) throws Exception {
        if (node == null) {
            //Log.v("Accessibility", "node is null (stopping iteration)");
            return;
        }

        if (node.isPassword()) {
            Log.i("Accessibility", "Nasao sam password polje");
            throw new Exception("password polje");
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            try {
                checkFloatingIfNeeded(node.getChild(i), deep + 1);
            }catch (Exception e){
                //Log.i("Accessibility", e.toString());
                if (e.getMessage().equals("password polje")) {
                    Log.i("Accessibility", "Valjda je " + (deep) + ", " + (i - 1) + " mejl");
                    if (deepForPassword == -1) {
                        deepForPassword = deep;
                        Log.i("Accessibility", "Podesio vrednosti za deep and child");
                    }
                    childForPassword.push(i);
                    throw e;
                }
            }
        }
        /* NOTE: Not sure if this is really required. Documentation is unclear. */
        node.recycle();
        deepForPassword = -1;
        childForPassword = new Stack<>();
        //Log.v("Accessibility", "showIfNeededDone");
    }

    private void showFloating() {
        Log.v("Accessibility", "showFloating()");
        Preferences.init(getApplicationContext());
        if (Preferences.sharedPreferences.getBoolean(Preferences.darkMode, false)){
            setTheme(R.style.AppThemeDark);
        }
        else {
            setTheme(R.style.AppTheme);
        }
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        mLayout = new FrameLayout(this);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
        lp.format = PixelFormat.TRANSLUCENT;
        lp.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.gravity = Gravity.TOP | Gravity.RIGHT;
        LayoutInflater inflater = LayoutInflater.from(this);
        inflater.inflate(R.layout.password_button_overlay, mLayout);
        wm.addView(mLayout, lp);
        ((Button)mLayout.findViewById(R.id.popuniDugme)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popuni();
            }
        });
        ((Button)mLayout.findViewById(R.id.popuniDugme)).setOnLongClickListener(new View.OnLongClickListener(){
            @Override
            public boolean onLongClick(View view) {
                hideFloating();
                return true;
            }
        });
    }

    private void hideFloating(){
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        try {
            wm.removeView(mLayout);
        }catch (NullPointerException ignored){}
    }

    @Override
    public void onInterrupt() {

    }

     private ActivityInfo tryGetActivity(ComponentName componentName) {
        try {
          return getPackageManager().getActivityInfo(componentName, 0);
        }
        catch (PackageManager.NameNotFoundException e) {
          return null;
        }
    }

    public void popuni(){
        username = "test";
        password = "test";
        hideFloating();

        waitPackageName = currentPackageName;

        Intent intent = new Intent(MyAccessibilityService.this, getUsernameAndPassword.class);
        intent.setComponent(new ComponentName("com.hawerner.passmanager", "com.hawerner.passmanager.getUsernameAndPassword"));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        intent.putExtra("isAccessibility", true);
        intent.putExtra("URI", currentPackageName);
        startActivity(intent);

    }
    private void popuni(AccessibilityNodeInfo node) throws Exception {
        if (node == null) {
            Log.v("Accessibility", "node is null (stopping iteration)");
            return;
        }

        //Log.i("Accessibility", node.toString());
        if (node.isPassword()){
            Log.i("Accessibility", "Nasao sam password polje");
            throw new Exception("password polje");
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            try {
                popuni(node.getChild(i));
            }catch (Exception e){
                if (e.toString().equals("java.lang.Exception: password polje")) {
                    try {
                        Log.i("Accessibility", "Trying to paste child " + i);
                        Bundle arguments = new Bundle();
                        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, password);
                        node.getChild(i).performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);

                        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, username);
                        node.getChild(i - 1).performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
                    } catch (Exception e1) {
                        Log.i("Accessibility", "Nesto je crklo");
                        Log.i("Accessibility", e1.toString());
                    }
                }
                throw new Exception("Gotovo");
            }
        }
        /* NOTE: Not sure if this is really required. Documentation is unclear. */
        node.recycle();
        deepForPassword = -1;
        childForPassword = new Stack<>();
        Log.v("Accessibility", "popuniDone");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("Accessibility", "onStartCommand");
        try {
            username = intent.getStringExtra("username");
            password = intent.getStringExtra("password");
        }catch (Exception e) {
            return super.onStartCommand(intent, flags, startId);
        }

        SystemClock.sleep(1000);
        try {
            popuni(getRootInActiveWindow());
        }catch (Exception ignored){
            Log.i("Accessibility", "onStartCommandException");
        }
        return super.onStartCommand(intent, flags, startId);
    }
}
