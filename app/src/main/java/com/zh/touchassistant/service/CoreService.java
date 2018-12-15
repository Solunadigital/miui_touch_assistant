package com.zh.touchassistant.service;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.view.accessibility.AccessibilityEvent;

import com.zh.touchassistant.AssistantApp;
import com.zh.touchassistant.FloatViewLiveData;
import com.zh.touchassistant.controller.FloatButtonWindowController;
import com.zh.touchassistant.controller.FloatPanelWindowController;
import com.zh.touchassistant.floating.FloatWindowManager;

/**
 * <b>Package:</b> com.zh.touchassistant <br>
 * <b>FileName:</b> CoreService <br>
 * <b>Create Date:</b> 2018/12/6  上午12:39 <br>
 * <b>Author:</b> zihe <br>
 * <b>Description:</b>  <br>
 */
public class CoreService extends AccessibilityService {
    private static final int NOTIFICATION_ID = 1234;

    private FloatButtonWindowController mFloatButtonVC;
    private FloatPanelWindowController mFloatPanelVC;
    private boolean isFirst = true;

    public static class Action {
        public static final String ACTION_SHOW_FLOATING_WINDOW = "com.zh.touchassistant.SHOW_FLOATING_WINDOW";
        public static final String ACTION_HIDE_FLOATING_WINDOW = "com.zh.touchassistant.HIDE_FLOATING_WINDOW";
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ((AssistantApp) getApplication()).setAccessibility(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return super.onStartCommand(intent, flags, startId);
        }
        if (Action.ACTION_SHOW_FLOATING_WINDOW.equals(intent.getAction())) {
            showFloatWindow();
        } else if (Action.ACTION_HIDE_FLOATING_WINDOW.equals(intent.getAction())) {
            hideFloatWindow();
        }
        //保活使用
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            //18以前空通知栏即可
            startForeground(NOTIFICATION_ID, new Notification());
        } else {
            Intent innerIntent = new Intent(this, KeepAliveInnerService.class);
            startService(innerIntent);
            startForeground(NOTIFICATION_ID, new Notification());
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private static class KeepAliveInnerService extends Service {

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            startForeground(NOTIFICATION_ID, new Notification());
            stopForeground(true);
            stopSelf();
            return super.onStartCommand(intent, flags, startId);
        }
    }

    private void showFloatWindow() {
        if (isFirst) {
            AssistantApp assistantApp = (AssistantApp) getApplication();
            FloatWindowManager floatWindowManager = new FloatWindowManager(this);
            //填充和浮动面板浮动按钮
            mFloatPanelVC = new FloatPanelWindowController(this, floatWindowManager);
            mFloatButtonVC = new FloatButtonWindowController(this, floatWindowManager, mFloatPanelVC);
            mFloatButtonVC.setOnFloatButtonPositionUpdateListener(new FloatButtonWindowController.OnFloatButtonPositionUpdateListener() {
                @Override
                public void onFloatButtonPositionUpdate(int newX, int newY) {
                    mFloatPanelVC.followButtonPosition(newX, newY);
                }
            });
            mFloatButtonVC.setOnStatusChangeListener(new FloatButtonWindowController.OnStatusChangeListener() {
                @Override
                public boolean onPrepareStatusChange(int prepareStatus) {
                    return mFloatPanelVC.isCanChangeStatus();
                }

                @Override
                public void onStatusChange(int newStatus) {
                }
            });
            FloatViewLiveData floatViewLiveData = assistantApp.getFloatViewLiveData();
            floatViewLiveData.addOnDataChangeCallback(new FloatViewLiveData.OnDataChangeCallback() {
                @Override
                public void onDataChange(boolean isOpen) {
                    //这里统一做UI切换
                    if (isOpen) {
                        mFloatButtonVC.open();
                        mFloatPanelVC.open();
                    } else {
                        mFloatButtonVC.off();
                        mFloatPanelVC.off();
                    }
                }
            });
            isFirst = false;
        } else {
            mFloatButtonVC.showFloatWindow();
        }
    }

    private void hideFloatWindow() {
        if (mFloatButtonVC != null) {
            mFloatButtonVC.hideFloatWindow();
        }
    }
}
