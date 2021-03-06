package com.sentaroh.android.SMBSync2;

/*
The MIT License (MIT)
Copyright (c) 2011-2018 Sentaroh

Permission is hereby granted, free of charge, to any person obtaining a copy of 
this software and associated documentation files (the "Software"), to deal 
in the Software without restriction, including without limitation the rights to use,
copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
and to permit persons to whom the Software is furnished to do so, subject to 
the following conditions:

The above copyright notice and this permission notice shall be included in all copies or 
substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE 
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.

*/

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.content.FileProvider;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.ClipboardManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;

import com.sentaroh.android.SMBSync2.Log.LogFileListDialogFragment;
import com.sentaroh.android.SMBSync2.Log.LogUtil;
import com.sentaroh.android.Utilities.ContextButton.ContextButtonUtil;
import com.sentaroh.android.Utilities.ContextMenu.CustomContextMenu;
import com.sentaroh.android.Utilities.Dialog.CommonDialog;
import com.sentaroh.android.Utilities.Dialog.MessageDialogFragment;
import com.sentaroh.android.Utilities.Dialog.ProgressBarDialogFragment;
import com.sentaroh.android.Utilities.LocalMountPoint;
import com.sentaroh.android.Utilities.NotifyEvent;
import com.sentaroh.android.Utilities.NotifyEvent.NotifyEventListener;
import com.sentaroh.android.Utilities.SafManager;
import com.sentaroh.android.Utilities.ThemeUtil;
import com.sentaroh.android.Utilities.ThreadCtrl;
import com.sentaroh.android.Utilities.Widget.CustomTabContentView;
import com.sentaroh.android.Utilities.Widget.CustomTextView;
import com.sentaroh.android.Utilities.Widget.CustomViewPager;
import com.sentaroh.android.Utilities.Widget.CustomViewPagerAdapter;
import com.sentaroh.android.Utilities.ZipUtil;

import java.io.File;
import java.util.ArrayList;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.sentaroh.android.SMBSync2.Constants.ACTIVITY_REQUEST_CODE_SDCARD_STORAGE_ACCESS;
import static com.sentaroh.android.SMBSync2.Constants.APPLICATION_TAG;
import static com.sentaroh.android.SMBSync2.Constants.SMBSYNC2_CONFIRM_REQUEST_COPY;
import static com.sentaroh.android.SMBSync2.Constants.SMBSYNC2_CONFIRM_REQUEST_DELETE_DIR;
import static com.sentaroh.android.SMBSync2.Constants.SMBSYNC2_CONFIRM_REQUEST_DELETE_FILE;
import static com.sentaroh.android.SMBSync2.Constants.SMBSYNC2_CONFIRM_REQUEST_MOVE;
import static com.sentaroh.android.SMBSync2.Constants.SMBSYNC2_CONFIRM_RESP_CANCEL;
import static com.sentaroh.android.SMBSync2.Constants.SMBSYNC2_CONFIRM_RESP_NO;
import static com.sentaroh.android.SMBSync2.Constants.SMBSYNC2_CONFIRM_RESP_NOALL;
import static com.sentaroh.android.SMBSync2.Constants.SMBSYNC2_CONFIRM_RESP_YES;
import static com.sentaroh.android.SMBSync2.Constants.SMBSYNC2_CONFIRM_RESP_YESALL;
import static com.sentaroh.android.SMBSync2.Constants.SMBSYNC2_SERIALIZABLE_FILE_NAME;
import static com.sentaroh.android.SMBSync2.Constants.SMBSYNC2_TAB_NAME_HIST;
import static com.sentaroh.android.SMBSync2.Constants.SMBSYNC2_TAB_NAME_MESSAGE;
import static com.sentaroh.android.SMBSync2.Constants.SMBSYNC2_TAB_NAME_TASK;
import static com.sentaroh.android.SMBSync2.ScheduleConstants.SCHEDULER_INTENT_SET_TIMER;
import static com.sentaroh.android.SMBSync2.ScheduleConstants.SCHEDULER_INTENT_SET_TIMER_IF_NOT_SET;

@SuppressLint("NewApi")
public class ActivityMain extends AppCompatActivity {

    private boolean isTaskTermination = false; // kill is disabled(enable is kill by onDestroy)

    private TabHost mMainTabHost = null;
    private Context mContext = null;
    private AppCompatActivity mActivity = null;

    private GlobalParameters mGp = null;
    private SyncTaskUtil mTaskUtil = null;

    private CommonUtilities mUtil = null;
    private CustomContextMenu ccMenu = null;

    private final static int NORMAL_START = 0;
    private final static int RESTART_WITH_OUT_INITIALYZE = 1;
    private final static int RESTART_BY_KILLED = 2;
    private final static int RESTART_BY_DESTROYED = 3;
    private int restartType = NORMAL_START;

    private ServiceConnection mSvcConnection = null;
    private CommonDialog commonDlg = null;
    private Handler mUiHandler = new Handler();

    private ActionBar mActionBar = null;

    private String mCurrentTab = null;

    private boolean enableMainUi = true;

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered.");
        out.putString("currentTab", mCurrentTab);
    }

    @Override
    protected void onRestoreInstanceState(Bundle in) {
        super.onRestoreInstanceState(in);
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered.");
        mCurrentTab = in.getString("currentTab");
        if (mGp.activityIsFinished) restartType = RESTART_BY_KILLED;
        else restartType = RESTART_BY_DESTROYED;
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        mContext = getApplicationContext();
        mGp= GlobalWorkArea.getGlobalParameters(mContext);
        mGp.safMgr.loadSafFile();
        mActivity = this;
        if (mGp.themeColorList == null) {
            mGp.themeColorList = ThemeUtil.getThemeColorList(this);
        }
        setTheme(mGp.applicationTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_screen);

        mGp.themeColorList = ThemeUtil.getThemeColorList(mActivity);
        mUtil = new CommonUtilities(this.getApplicationContext(), "Main", mGp);
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered, " + "resartStatus=" + restartType);
        mActionBar = getSupportActionBar();
        mActionBar.setDisplayShowHomeEnabled(false);
        mActionBar.setHomeButtonEnabled(false);
        if (mGp.settingFixDeviceOrientationToPortrait)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        else setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        ccMenu = new CustomContextMenu(this.getResources(), getSupportFragmentManager());
        commonDlg = new CommonDialog(this, getSupportFragmentManager());
        checkRequiredPermissions();
        mTaskUtil = new SyncTaskUtil(mUtil, this, commonDlg, ccMenu, mGp, getSupportFragmentManager());
        mGp.msgListAdapter = new AdapterSyncMessage(this, R.layout.msg_list_item_view, mGp.msgList);

        if (mGp.syncTaskList == null)
            mGp.syncTaskList = SyncTaskUtil.createSyncTaskList(mContext, mGp, mUtil);

        mGp.syncTaskAdapter = new AdapterSyncTask(mActivity, R.layout.sync_task_item_view, mGp.syncTaskList, mGp);

        if (mGp.syncHistoryList == null) mGp.syncHistoryList = mUtil.loadHistoryList();

        mGp.syncHistoryAdapter = new AdapterSyncHistory(mActivity, R.layout.sync_history_list_item_view, mGp.syncHistoryList);
        mCurrentTab = SMBSYNC2_TAB_NAME_TASK;

        createTabView();
        initAdapterAndView();
        mGp.initJcifsOption();
        listSettingsOption();

        ScheduleUtil.sendTimerRequest(mContext, SCHEDULER_INTENT_SET_TIMER_IF_NOT_SET);
        setSyncTaskContextButtonHide();

        Thread th1 = new Thread() {
            @Override
            public void run() {
                mUtil.addDebugMsg(1, "I", "Initialyze application specific directory started");
                mUtil.initAppSpecificExternalDirectory(mContext);
                mUtil.addDebugMsg(1, "I", "Initialyze application specific directory ended");
            }
        };
        th1.start();
    }

    @SuppressLint("NewApi")
    @Override
    protected void onStart() {
        super.onStart();
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered, " + "resartStatus=" + restartType);

    }

    @Override
    protected void onResume() {
        super.onResume();
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered, " + "resartStatus=" + restartType);
        if (restartType == RESTART_WITH_OUT_INITIALYZE) {
            mGp.safMgr.loadSafFile();
            setActivityForeground(true);
            ScheduleUtil.setSchedulerInfo(mGp);
            mGp.progressSpinSyncprof.setText(mGp.progressSpinSyncprofText);
            mGp.progressSpinMsg.setText(mGp.progressSpinMsgText);
        } else {
            NotifyEvent svc_ntfy = new NotifyEvent(mContext);
            svc_ntfy.setListener(new NotifyEventListener() {
                @Override
                public void positiveResponse(Context c, Object[] o) {
                    setCallbackListener();
                    setActivityForeground(true);
                    if (restartType == NORMAL_START) {
                        setUiEnabled();
                        checkStorageStatus();
                        if (mGp.msgList.size()>1) mMainTabHost.setCurrentTabByTag(SMBSYNC2_TAB_NAME_MESSAGE);
                    } else if (restartType == RESTART_BY_KILLED) {
                        setUiEnabled();
                        restoreTaskData();
                        mUtil.addLogMsg("W", mContext.getString(R.string.msgs_smbsync_main_restart_by_killed));
                        mMainTabHost.setCurrentTabByTag(SMBSYNC2_TAB_NAME_MESSAGE);
                    } else if (restartType == RESTART_BY_DESTROYED) {
                        setUiEnabled();
                        restoreTaskData();
                        mUtil.addLogMsg("W", mContext.getString(R.string.msgs_smbsync_main_restart_by_destroyed));
                        mMainTabHost.setCurrentTabByTag(SMBSYNC2_TAB_NAME_MESSAGE);
                    }
                    setMessageContextButtonListener();
                    setMessageContextButtonNormalMode();

                    setSyncTaskContextButtonListener();
                    setSyncTaskListItemClickListener();
                    setSyncTaskListLongClickListener();
                    setSyncTaskContextButtonNormalMode();

                    setHistoryContextButtonListener();
                    setHistoryViewItemClickListener();
                    setHistoryViewLongClickListener();
                    setHistoryContextButtonNormalMode();

                    deleteTaskData();
                    ScheduleUtil.setSchedulerInfo(mGp);
                    restartType = RESTART_WITH_OUT_INITIALYZE;
                    reshowDialogWindow();
                    if (isUiEnabled()) mGp.msgListView.setFastScrollEnabled(true);
                }

                @Override
                public void negativeResponse(Context c, Object[] o) {}
            });
            openService(svc_ntfy);
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered, " + "resartStatus=" + restartType);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered " + ",currentView=" + mCurrentTab +
                ", getChangingConfigurations=" + String.format("0x%08x", getChangingConfigurations()));
        setActivityForeground(false);
        if (!isTaskTermination) saveTaskData();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
    }

//    @Override
//    protected void onNewIntent(Intent received_intent) {
//        super.onNewIntent(received_intent);
//        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
//
////        if (received_intent.getAction()!=null && !received_intent.getAction().equals("")) {
////            Intent in=new Intent(received_intent.getAction());
////            in.setClass(this, SyncReceiver.class);
////            if (received_intent.getExtras() != null) in.putExtras(received_intent.getExtras());
////            sendBroadcast(in,null);
////        }
//
//    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered, " +
                "isFinishing=" + isFinishing() +
                ", changingConfigurations=" + String.format("0x%08x", getChangingConfigurations()));
        setActivityForeground(false);
        unsetCallbackListener();

        if (isFinishing()) {
            deleteTaskData();
            mGp.logCatActive=false;
            mGp.clearParms();
        }
        mGp.activityIsFinished = isFinishing();
        closeService();
        LogUtil.flushLog(mContext, mGp);

        System.gc();

    }

    private void setActivityForeground(boolean fore_ground) {
        if (mSvcClient != null) {
            try {
                if (fore_ground) mSvcClient.aidlSetActivityInForeground();
                else mSvcClient.aidlSetActivityInBackground();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void showSystemInfo() {
        final Dialog dialog = new Dialog(mActivity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.common_dialog);

        final TextView tv_title=(TextView)dialog.findViewById(R.id.common_dialog_title);
        final TextView tv_msg_old=(TextView)dialog.findViewById(R.id.common_dialog_msg);
        tv_msg_old.setVisibility(TextView.GONE);
        final CustomTextView tv_msg=(CustomTextView)dialog.findViewById(R.id.common_dialog_custom_text_view);
        tv_msg.setVisibility(TextView.VISIBLE);
        final Button btn_copy=(Button)dialog.findViewById(R.id.common_dialog_btn_ok);
        final Button btn_close=(Button)dialog.findViewById(R.id.common_dialog_btn_cancel);
        final Button btn_send=(Button)dialog.findViewById(R.id.common_dialog_extra_button);
        btn_send.setText(mContext.getString(R.string.msgs_info_storage_send_btn_title));
        btn_send.setVisibility(Button.VISIBLE);

        tv_title.setText(mContext.getString(R.string.msgs_menu_list_storage_info));
        btn_close.setText(mContext.getString(R.string.msgs_common_dialog_close));
        btn_copy.setText(mContext.getString(R.string.msgs_info_storage_copy_clipboard));

        ArrayList<String>sil= CommonUtilities.listSystemInfo(mGp);
        String si_text="";
        for(String si_item:sil) si_text+=si_item+"\n";

        tv_msg.setText(si_text);

        CommonDialog.setDlgBoxSizeLimit(dialog,true);

        btn_copy.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                android.content.ClipboardManager cm=(android.content.ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData cd=cm.getPrimaryClip();
                cm.setPrimaryClip(ClipData.newPlainText("SMBSync2 storage info", tv_msg.getText().toString()));
                Toast.makeText(mContext,
                        mContext.getString(R.string.msgs_info_storage_copy_completed), Toast.LENGTH_LONG).show();
            }
        });

        btn_close.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        btn_send.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent();
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setAction(Intent.ACTION_SEND);
                intent.setType("message/rfc822");
//                intent.setType("text/plain");
//                intent.setType("application/zip");

                intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"gm.developer.fhoshino@gmail.com"});
//                intent.putExtra(Intent.EXTRA_CC, new String[]{"cc@example.com"});
//                intent.putExtra(Intent.EXTRA_BCC, new String[]{"bcc@example.com"});
                intent.putExtra(Intent.EXTRA_SUBJECT, "SMBSync2 System Info");
                intent.putExtra(Intent.EXTRA_TEXT, tv_msg.getText().toString());
//                intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(lf));
                mContext.startActivity(intent);
            }
        });

        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                btn_close.performClick();
            }
        });

        dialog.show();
    }

//    private void showBatteryOptimization() {
//        if (Build.VERSION.SDK_INT >= 23) {
//            Intent intent = new Intent();
//            String packageName = mContext.getPackageName();
//            PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
//            if (pm.isIgnoringBatteryOptimizations(packageName)) {
//                intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
//                startActivity(intent);
//                mUtil.addDebugMsg(1, "I", "Invoke battery optimization settings");
//            } else {
//                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
//                intent.setData(Uri.parse("package:" + packageName));
//                startActivity(intent);
//                mUtil.addDebugMsg(1, "I", "Request ignore battery optimization");
//            }
//        }
//    }

    class ViewSaveArea {
        public int current_tab_pos = 0;
        public int current_pager_pos = 0;
        public int prof_list_view_pos_x = 0, prof_list_view_pos_y = 0;
        public boolean prof_adapter_show_cb = false;
        public int msg_list_view_pos_x = 0, msg_list_view_pos_y = 0;
        public int sync_list_view_pos_x = 0, sync_list_view_pos_y = 0;
        public boolean sync_adapter_show_cb = false;

        public int prog_bar_view_visibility = ProgressBar.GONE,
                prog_spin_view_visibility = ProgressBar.GONE, confirm_view_visibility = ProgressBar.GONE;

        public String prog_prof = "", prog_msg = "";

        public ArrayList<SyncHistoryItem> sync_hist_list = null;

        public String confirm_msg = "";
        public String progress_bar_msg = "";
        public int progress_bar_progress = 0, progress_bar_max = 0;

        public ButtonViewContent confirm_cancel = new ButtonViewContent();
        public ButtonViewContent confirm_yes = new ButtonViewContent();
        public ButtonViewContent confirm_yes_all = new ButtonViewContent();
        public ButtonViewContent confirm_no = new ButtonViewContent();
        public ButtonViewContent confirm_no_all = new ButtonViewContent();
        public ButtonViewContent prog_bar_cancel = new ButtonViewContent();
        public ButtonViewContent prog_bar_immed = new ButtonViewContent();
        public ButtonViewContent prog_spin_cancel = new ButtonViewContent();
    }

    class ButtonViewContent {
        public String button_text = "";
        public boolean button_visible = true, button_enabled = true, button_clickable = true;
    }

    private void saveButtonStatus(Button btn, ButtonViewContent sv) {
        sv.button_text = btn.getText().toString();
        sv.button_clickable = btn.isClickable();
        sv.button_enabled = btn.isEnabled();
        sv.button_visible = btn.isShown();
    }

    private void restoreButtonStatus(Button btn, ButtonViewContent sv, OnClickListener ocl) {
        btn.setText(sv.button_text);
        btn.setClickable(sv.button_clickable);
        btn.setEnabled(sv.button_enabled);
//		if (sv.button_visible) btn.setVisibility(Button.VISIBLE);
//		else btn.setVisibility(Button.GONE);
        btn.setOnClickListener(ocl);
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mUtil != null) {
            mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " Entered, ",
                    "New orientation=" + newConfig.orientation +
                            ", New language=", newConfig.locale.getLanguage());
        }
        screenReload(false);
    }

    private void screenReload(boolean force_reload) {
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " Entered");
        ViewSaveArea vsa = null;
        vsa = saveViewContent();
        releaseImageResource();
        setContentView(R.layout.main_screen);
        mActionBar = getSupportActionBar();

        mGp.syncHistoryListView.setAdapter(null);

        mGp.syncTaskListView.setAdapter(null);
        ArrayList<SyncTaskItem> pfl = mGp.syncTaskAdapter.getArrayList();

        mGp.msgListView.setAdapter(null);

        ArrayList<SyncMessageItem> mfl=new ArrayList<SyncMessageItem>();
        if (mGp.msgListAdapter!=null) mfl=mGp.msgListAdapter.getMessageList();

        createTabView();
        mMainTabHost.setOnTabChangedListener(null);

        mGp.syncTaskAdapter = new AdapterSyncTask(mActivity, R.layout.sync_task_item_view, pfl, mGp);
        mGp.syncTaskAdapter.setShowCheckBox(vsa.prof_adapter_show_cb);
        mGp.syncTaskAdapter.notifyDataSetChanged();

        mGp.msgListAdapter = new AdapterSyncMessage(this, R.layout.msg_list_item_view, mfl);

        mGp.syncHistoryAdapter = new AdapterSyncHistory(mActivity, R.layout.sync_history_list_item_view, vsa.sync_hist_list);
        mGp.syncHistoryAdapter.setShowCheckBox(vsa.sync_adapter_show_cb);
        mGp.syncHistoryAdapter.notifyDataSetChanged();

        if (isUiEnabled()) mGp.msgListView.setFastScrollEnabled(true);

        initAdapterAndView();

        restoreViewContent(vsa);
        mMainTabHost.setOnTabChangedListener(new MainOnTabChange());

        setMessageContextButtonListener();
        setMessageContextButtonNormalMode();

        setSyncTaskContextButtonListener();
        setSyncTaskListItemClickListener();
        setSyncTaskListLongClickListener();
//		setMsglistLongClickListener();

        setHistoryContextButtonListener();

        setHistoryViewItemClickListener();
        setHistoryViewLongClickListener();

        if (mCurrentTab.equals(SMBSYNC2_TAB_NAME_TASK)) {
            if (mGp.syncHistoryAdapter.isShowCheckBox()) setHistoryContextButtonSelectMode();
            else setHistoryContextButtonNormalMode();

            if (mGp.syncTaskAdapter.isShowCheckBox()) setSyncTaskContextButtonSelectMode();
            else setSyncTaskContextButtonNormalMode();
        } else if (mCurrentTab.equals(SMBSYNC2_TAB_NAME_HIST)) {
            if (mGp.syncTaskAdapter.isShowCheckBox()) setSyncTaskContextButtonSelectMode();
            else setSyncTaskContextButtonNormalMode();

            if (mGp.syncHistoryAdapter.isShowCheckBox()) setHistoryContextButtonSelectMode();
            else setHistoryContextButtonNormalMode();
        }

        if (isUiEnabled()) setUiEnabled();
        else setUiDisabled();
        vsa = null;
    }

    private int newSyncTaskListViewPos = -1;

    private ViewSaveArea saveViewContent() {
        ViewSaveArea vsa = new ViewSaveArea();
        vsa.current_tab_pos = mMainTabHost.getCurrentTab();
        vsa.current_pager_pos = mMainViewPager.getCurrentItem();

        vsa.prof_list_view_pos_x = mGp.syncTaskListView.getFirstVisiblePosition();
        if (mGp.syncTaskListView.getChildAt(0) != null)
            vsa.prof_list_view_pos_y = mGp.syncTaskListView.getChildAt(0).getTop();
        vsa.prof_adapter_show_cb = mGp.syncTaskAdapter.isShowCheckBox();
        vsa.msg_list_view_pos_x = mGp.msgListView.getFirstVisiblePosition();
        if (mGp.msgListView.getChildAt(0) != null)
            vsa.msg_list_view_pos_y = mGp.msgListView.getChildAt(0).getTop();
        vsa.sync_list_view_pos_x = mGp.syncHistoryListView.getFirstVisiblePosition();
        if (mGp.syncHistoryListView.getChildAt(0) != null)
            vsa.sync_list_view_pos_y = mGp.syncHistoryListView.getChildAt(0).getTop();
        vsa.sync_adapter_show_cb = mGp.syncHistoryAdapter.isShowCheckBox();

        vsa.prog_prof = mGp.progressSpinSyncprof.getText().toString();
        vsa.prog_msg = mGp.progressSpinMsg.getText().toString();
        vsa.progress_bar_progress = mGp.progressBarPb.getProgress();
        vsa.progress_bar_max = mGp.progressBarPb.getMax();

        vsa.prog_bar_view_visibility = mGp.progressBarView.getVisibility();
        vsa.confirm_view_visibility = mGp.confirmView.getVisibility();
        vsa.prog_spin_view_visibility = mGp.progressSpinView.getVisibility();

        saveButtonStatus(mGp.confirmCancel, vsa.confirm_cancel);
        saveButtonStatus(mGp.confirmYes, vsa.confirm_yes);
        saveButtonStatus(mGp.confirmYesAll, vsa.confirm_yes_all);
        saveButtonStatus(mGp.confirmNo, vsa.confirm_no);
        saveButtonStatus(mGp.confirmNoAll, vsa.confirm_no_all);
        saveButtonStatus(mGp.progressBarCancel, vsa.prog_bar_cancel);
        saveButtonStatus(mGp.progressSpinCancel, vsa.prog_spin_cancel);
        saveButtonStatus(mGp.progressBarImmed, vsa.prog_bar_immed);

        vsa.confirm_msg = mGp.confirmMsg.getText().toString();

        vsa.progress_bar_msg = mGp.progressBarMsg.getText().toString();

        vsa.sync_hist_list = mGp.syncHistoryAdapter.getSyncHistoryList();

        return vsa;
    }

    private void restoreViewContent(ViewSaveArea vsa) {
        mMainTabHost.setCurrentTab(vsa.current_tab_pos);
        mMainViewPager.setCurrentItem(vsa.current_pager_pos);
        mGp.syncTaskListView.setSelectionFromTop(vsa.prof_list_view_pos_x, vsa.prof_list_view_pos_y);
        mGp.msgListView.setSelectionFromTop(vsa.msg_list_view_pos_x, vsa.msg_list_view_pos_y);
        mGp.syncHistoryListView.setSelectionFromTop(vsa.sync_list_view_pos_x, vsa.sync_list_view_pos_y);

        mGp.confirmMsg.setText(vsa.confirm_msg);

        restoreButtonStatus(mGp.confirmCancel, vsa.confirm_cancel, mGp.confirmCancelListener);
        restoreButtonStatus(mGp.confirmYes, vsa.confirm_yes, mGp.confirmYesListener);
        restoreButtonStatus(mGp.confirmYesAll, vsa.confirm_yes_all, mGp.confirmYesAllListener);
        restoreButtonStatus(mGp.confirmNo, vsa.confirm_no, mGp.confirmNoListener);
        restoreButtonStatus(mGp.confirmNoAll, vsa.confirm_no_all, mGp.confirmNoAllListener);
        restoreButtonStatus(mGp.progressBarCancel, vsa.prog_bar_cancel, mGp.progressBarCancelListener);
        restoreButtonStatus(mGp.progressSpinCancel, vsa.prog_spin_cancel, mGp.progressSpinCancelListener);
        restoreButtonStatus(mGp.progressBarImmed, vsa.prog_bar_immed, mGp.progressBarImmedListener);

        mGp.progressBarMsg.setText(vsa.progress_bar_msg);
        mGp.progressBarPb.setMax(vsa.progress_bar_max);
        mGp.progressBarPb.setProgress(vsa.progress_bar_progress);

        mGp.progressSpinSyncprof.setText(vsa.prog_prof);
        mGp.progressSpinMsg.setText(vsa.prog_msg);
        mGp.scheduleInfoView.setText(mGp.scheduleInfoText);

        if (vsa.prog_bar_view_visibility != LinearLayout.GONE) {
            mGp.progressBarView.bringToFront();
            mGp.progressBarView.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);
            mGp.progressBarView.setVisibility(LinearLayout.VISIBLE);
        } else mGp.progressBarView.setVisibility(LinearLayout.GONE);

        if (vsa.prog_spin_view_visibility != LinearLayout.GONE) {
            mGp.progressSpinView.bringToFront();
            mGp.progressSpinView.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);
            mGp.progressSpinView.setVisibility(LinearLayout.VISIBLE);
        } else mGp.progressSpinView.setVisibility(LinearLayout.GONE);

        if (vsa.confirm_view_visibility != LinearLayout.GONE) {
            mGp.confirmView.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);
            mGp.confirmView.setVisibility(LinearLayout.VISIBLE);
            mGp.confirmView.bringToFront();
        } else {
            mGp.confirmView.setVisibility(LinearLayout.GONE);
        }

    }

    private void initAdapterAndView() {
        mGp.msgListView.setAdapter(mGp.msgListAdapter);
        mGp.msgListView.setDrawingCacheEnabled(true);

        mGp.syncTaskListView.setAdapter(mGp.syncTaskAdapter);
        mGp.syncTaskListView.setDrawingCacheEnabled(true);

        mGp.syncHistoryListView.setAdapter(mGp.syncHistoryAdapter);
        mGp.syncHistoryAdapter.notifyDataSetChanged();
    }

    private LinearLayout mSyncTaskView;
    private LinearLayout mHistoryView;
    private LinearLayout mMessageView;

    private CustomViewPager mMainViewPager;
    private CustomViewPagerAdapter mMainViewPagerAdapter;

    private TabWidget mMainTabWidget;

    private void createTabView() {
        mMainTabHost = (TabHost) findViewById(android.R.id.tabhost);
        mMainTabHost.setup();
        mMainTabWidget = (TabWidget) findViewById(android.R.id.tabs);

        mMainTabWidget.setStripEnabled(false);
        mMainTabWidget.setShowDividers(LinearLayout.SHOW_DIVIDER_NONE);

        CustomTabContentView tabViewProf = new CustomTabContentView(this, getString(R.string.msgs_tab_name_prof));
        mMainTabHost.addTab(mMainTabHost.newTabSpec(SMBSYNC2_TAB_NAME_TASK).setIndicator(tabViewProf).setContent(android.R.id.tabcontent));

        CustomTabContentView tabViewHist = new CustomTabContentView(this, getString(R.string.msgs_tab_name_history));
        mMainTabHost.addTab(mMainTabHost.newTabSpec(SMBSYNC2_TAB_NAME_HIST).setIndicator(tabViewHist).setContent(android.R.id.tabcontent));

        CustomTabContentView tabViewMsg = new CustomTabContentView(this, getString(R.string.msgs_tab_name_msg));
        mMainTabHost.addTab(mMainTabHost.newTabSpec(SMBSYNC2_TAB_NAME_MESSAGE).setIndicator(tabViewMsg).setContent(android.R.id.tabcontent));

        LinearLayout ll_main = (LinearLayout) findViewById(R.id.main_screen_view);
        ll_main.setBackgroundColor(mGp.themeColorList.window_background_color_content);

        LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mSyncTaskView = (LinearLayout) vi.inflate(R.layout.main_sync_task, null);
        mSyncTaskView.setBackgroundColor(mGp.themeColorList.window_background_color_content);
        mHistoryView = (LinearLayout) vi.inflate(R.layout.main_history, null);
        mHistoryView.setBackgroundColor(mGp.themeColorList.window_background_color_content);
        mMessageView = (LinearLayout) vi.inflate(R.layout.main_message, null);
        mMessageView.setBackgroundColor(mGp.themeColorList.window_background_color_content);

        mGp.msgListView = (ListView) mMessageView.findViewById(R.id.main_message_list_view);
        mGp.syncTaskListView = (ListView) mSyncTaskView.findViewById(R.id.main_sync_task_view_list);
        mGp.syncHistoryListView = (ListView) mHistoryView.findViewById(R.id.main_history_list_view);

        mGp.scheduleInfoView = (TextView) findViewById(R.id.main_schedule_view_info);
        mGp.scheduleInfoView.setTextColor(mGp.themeColorList.text_color_primary);

        mGp.confirmView = (LinearLayout) findViewById(R.id.main_dialog_confirm_view);
        mGp.confirmView.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);
        mGp.confirmView.setVisibility(LinearLayout.GONE);
        mGp.confirmMsg = (TextView) findViewById(R.id.main_dialog_confirm_msg);
        mGp.confirmMsg.setTextColor(mGp.themeColorList.text_color_primary);
        mGp.confirmCancel = (Button) findViewById(R.id.main_dialog_confirm_sync_cancel);
        setButtonColor(mGp.confirmCancel);
        if (mGp.themeColorList.theme_is_light)
            mGp.confirmCancel.setTextColor(mGp.themeColorList.text_color_primary);
        mGp.confirmYes = (Button) findViewById(R.id.copy_delete_confirm_yes);
        setButtonColor(mGp.confirmYes);
        mGp.confirmYes.setTextColor(mGp.themeColorList.text_color_primary);
        mGp.confirmNo = (Button) findViewById(R.id.copy_delete_confirm_no);
        setButtonColor(mGp.confirmNo);
        mGp.confirmNo.setTextColor(mGp.themeColorList.text_color_primary);
        mGp.confirmYesAll = (Button) findViewById(R.id.copy_delete_confirm_yesall);
        setButtonColor(mGp.confirmYesAll);
        mGp.confirmYesAll.setTextColor(mGp.themeColorList.text_color_primary);
        mGp.confirmNoAll = (Button) findViewById(R.id.copy_delete_confirm_noall);
        setButtonColor(mGp.confirmNoAll);
        mGp.confirmNoAll.setTextColor(mGp.themeColorList.text_color_primary);

        mGp.progressBarView = (LinearLayout) findViewById(R.id.main_dialog_progress_bar_view);
        mGp.progressBarView.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);
        mGp.progressBarView.setVisibility(LinearLayout.GONE);
        mGp.progressBarMsg = (TextView) findViewById(R.id.main_dialog_progress_bar_msg);
        mGp.progressBarMsg.setTextColor(mGp.themeColorList.text_color_primary);
        mGp.progressBarPb = (ProgressBar) findViewById(R.id.main_dialog_progress_bar_progress);

        mGp.progressBarCancel = (Button) findViewById(R.id.main_dialog_progress_bar_btn_cancel);
        setButtonColor(mGp.progressBarCancel);
        if (mGp.themeColorList.theme_is_light)
            mGp.progressBarCancel.setTextColor(mGp.themeColorList.text_color_primary);
        mGp.progressBarImmed = (Button) findViewById(R.id.main_dialog_progress_bar_btn_immediate);
        setButtonColor(mGp.progressBarImmed);
        if (mGp.themeColorList.theme_is_light)
            mGp.progressBarImmed.setTextColor(mGp.themeColorList.text_color_primary);


        mGp.progressSpinView = (LinearLayout) findViewById(R.id.main_dialog_progress_spin_view);
        mGp.progressSpinView.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);
        mGp.progressSpinView.setVisibility(LinearLayout.GONE);
        mGp.progressSpinSyncprof = (TextView) findViewById(R.id.main_dialog_progress_spin_syncprof);
        mGp.progressSpinSyncprof.setTextColor(mGp.themeColorList.text_color_primary);
        mGp.progressSpinMsg = (TextView) findViewById(R.id.main_dialog_progress_spin_syncmsg);
        mGp.progressSpinMsg.setTextColor(mGp.themeColorList.text_color_primary);
        mGp.progressSpinCancel = (Button) findViewById(R.id.main_dialog_progress_spin_btn_cancel);
        setButtonColor(mGp.progressSpinCancel);
        if (mGp.themeColorList.theme_is_light)
            mGp.progressSpinCancel.setTextColor(mGp.themeColorList.text_color_primary);

        createContextView();

        mMainViewPagerAdapter = new CustomViewPagerAdapter(this,
                new View[]{mSyncTaskView, mHistoryView, mMessageView});
        mMainViewPager = (CustomViewPager) findViewById(R.id.main_screen_pager);
//	    mMainViewPager.setBackgroundColor(mThemeColorList.window_color_background);
        mMainViewPager.setAdapter(mMainViewPagerAdapter);
        mMainViewPager.setOnPageChangeListener(new MainPageChangeListener());
        if (restartType == NORMAL_START) {
            mMainTabHost.setCurrentTabByTag(SMBSYNC2_TAB_NAME_TASK);
            mMainViewPager.setCurrentItem(0);
        }
        mMainTabHost.setOnTabChangedListener(new MainOnTabChange());

    }

    private void setButtonColor(Button btn) {
//		if (Build.VERSION.SDK_INT<11) {
//			btn.setBackgroundColor(Color.DKGRAY);
//		}
    }

    private class MainOnTabChange implements OnTabChangeListener {
        @Override
        public void onTabChanged(String tabId) {
            mUtil.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName() + " entered. tab=" + tabId + ",v=" + mCurrentTab);

            mActionBar.setIcon(R.drawable.smbsync);
            mActionBar.setHomeButtonEnabled(false);
            mActionBar.setTitle(R.string.app_name);

            mMainViewPager.setCurrentItem(mMainTabHost.getCurrentTab());

            if (mGp.syncTaskAdapter.isShowCheckBox()) {
                mGp.syncTaskAdapter.setShowCheckBox(false);
                mGp.syncTaskAdapter.setAllItemChecked(false);
                mGp.syncTaskAdapter.notifyDataSetChanged();
                setSyncTaskContextButtonNormalMode();
            }

            if (mGp.syncHistoryAdapter.isShowCheckBox()) {
                mGp.syncHistoryAdapter.setShowCheckBox(false);
                mGp.syncHistoryAdapter.setAllItemChecked(false);
                mGp.syncHistoryAdapter.notifyDataSetChanged();
                setHistoryContextButtonNormalMode();
            }

            if (tabId.equals(SMBSYNC2_TAB_NAME_TASK) && newSyncTaskListViewPos != -1) {
                mGp.syncTaskListView.post(new Runnable() {
                    @Override
                    public void run() {
                        mGp.syncTaskListView.setSelection(newSyncTaskListViewPos);
                        newSyncTaskListViewPos = -1;
                    }
                });
            } else if (tabId.equals(SMBSYNC2_TAB_NAME_MESSAGE)) {
                if (!mGp.freezeMessageViewScroll) {
                    mGp.uiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mGp!=null && mGp.msgListView!=null && mGp.msgListAdapter!=null)
                                mGp.msgListView.setSelection(mGp.msgListAdapter.getCount() - 1);
                        }
                    });
                }
            }
            mCurrentTab = tabId;
            refreshOptionMenu();
        }
    }

    private class MainPageChangeListener implements ViewPager.OnPageChangeListener {
        @Override
        public void onPageSelected(int position) {
	    	mUtil.addDebugMsg(2,"I","onPageSelected entered, pos="+position);
            mMainTabWidget.setCurrentTab(position);
            mMainTabHost.setCurrentTab(position);
            if (isUiEnabled()) setUiEnabled();
        }

        @Override
        public void onPageScrollStateChanged(int state) {
	    	mUtil.addDebugMsg(2,"I","onPageScrollStateChanged entered, state="+state);
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
	    	mUtil.addDebugMsg(2,"I","onPageScrolled entered, pos="+position);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mUtil.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName(), " entered");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_top, menu);
        return true;//super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        mUtil.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName(), " entered, isUiEnabled()="+isUiEnabled());
        boolean pm_bo = false;
//        if (Build.VERSION.SDK_INT >= 23) {
//            menu.findItem(R.id.menu_top_show_battery_optimization).setVisible(true);
//            String packageName = mContext.getPackageName();
//            PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
//            pm_bo = pm.isIgnoringBatteryOptimizations(packageName);
//            String bo_title = "";
//            if (pm_bo)
//                bo_title = mContext.getString(R.string.msgs_menu_battery_optimization_disabled);
//            else bo_title = mContext.getString(R.string.msgs_menu_battery_optimization_enabled);
//            menu.findItem(R.id.menu_top_show_battery_optimization).setTitle(bo_title);
//        } else {
//            menu.findItem(R.id.menu_top_show_battery_optimization).setVisible(false);
//        }
        LogCatUtil.prepareOptionMenu(mGp, mUtil, menu);

        if (Build.VERSION.SDK_INT >= 27) {
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED) {
                menu.findItem(R.id.menu_top_request_grant_coarse_location).setVisible(false);
            } else {
                menu.findItem(R.id.menu_top_request_grant_coarse_location).setVisible(true);
            }
        } else {
            menu.findItem(R.id.menu_top_request_grant_coarse_location).setVisible(false);
        }

        if (isUiEnabled()) {
            menu.findItem(R.id.menu_top_housekeep).setEnabled(true);
            if (mGp.syncThreadActive) menu.findItem(R.id.menu_top_housekeep).setVisible(false);
            else menu.findItem(R.id.menu_top_housekeep).setVisible(true);
            menu.findItem(R.id.menu_top_sync).setVisible(true);
            menu.findItem(R.id.menu_top_settings).setEnabled(true);
            if (!mGp.externalStorageIsMounted) {
                menu.findItem(R.id.menu_top_browse_log).setEnabled(false);
                menu.findItem(R.id.menu_top_export).setEnabled(false);
                menu.findItem(R.id.menu_top_import).setEnabled(false);
                menu.findItem(R.id.menu_top_log_management).setEnabled(false);
            } else {
                if (!mGp.settingLogOption)
                    menu.findItem(R.id.menu_top_browse_log).setVisible(false);
                else menu.findItem(R.id.menu_top_browse_log).setVisible(true);
                menu.findItem(R.id.menu_top_export).setEnabled(true);
                menu.findItem(R.id.menu_top_import).setEnabled(true);
                menu.findItem(R.id.menu_top_log_management).setEnabled(true);
            }
            menu.findItem(R.id.menu_top_add_shortcut).setEnabled(true);

            menu.findItem(R.id.menu_top_select_storage).setVisible(true);
//            if (mGp.safMgr.hasExternalSdcardPath()) menu.findItem(R.id.menu_top_select_storage).setVisible(true);
//            else menu.findItem(R.id.menu_top_select_storage).setVisible(false);

        } else {
            menu.findItem(R.id.menu_top_sync).setVisible(false);
            if (!mGp.settingLogOption) menu.findItem(R.id.menu_top_browse_log).setVisible(false);
            else menu.findItem(R.id.menu_top_browse_log).setVisible(true);
            menu.findItem(R.id.menu_top_browse_log).setEnabled(true);
            if (!mGp.externalStorageIsMounted) {
                menu.findItem(R.id.menu_top_browse_log).setEnabled(false);
            }
            if (!mGp.settingLogOption) {
                menu.findItem(R.id.menu_top_browse_log).setEnabled(false);
            }

            menu.findItem(R.id.menu_top_export).setEnabled(false);
            menu.findItem(R.id.menu_top_import).setEnabled(false);
            menu.findItem(R.id.menu_top_settings).setEnabled(false);
            menu.findItem(R.id.menu_top_log_management).setEnabled(false);
            menu.findItem(R.id.menu_top_housekeep).setEnabled(false);
            menu.findItem(R.id.menu_top_add_shortcut).setEnabled(false);

            menu.findItem(R.id.menu_top_select_storage).setVisible(false);
        }
        menu.findItem(R.id.menu_top_add_shortcut).setVisible(false);

        return super.onPrepareOptionsMenu(menu);
    }

    private boolean mScheduleEditorAvailable = true;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                processHomeButtonPress();
                return true;
            case R.id.menu_top_sync:
                if (isUiEnabled()) {
                    if (SyncTaskUtil.getSyncTaskSelectedItemCount(mGp.syncTaskAdapter) > 0) {
                        syncSelectedSyncTask();
                    } else {
                        syncAutoSyncTask();
                    }
                    SyncTaskUtil.setAllSyncTaskToUnchecked(true, mGp.syncTaskAdapter);
                    setSyncTaskContextButtonNormalMode();
                }

                return true;
            case R.id.menu_top_browse_log:
                invokeLogFileBrowser();
                setContextButtonNormalMode();
                return true;
            case R.id.menu_top_export:
                mTaskUtil.exportSyncTaskListDlg();
                setContextButtonNormalMode();
                return true;
            case R.id.menu_top_import:
                importSyncTaskAndParms();
                setContextButtonNormalMode();
                return true;
            case R.id.menu_top_log_management:
                invokeLogManagement();
                setContextButtonNormalMode();
                return true;
            case R.id.menu_top_scheduler:
                if (mScheduleEditorAvailable && isUiEnabled()) {
                    mScheduleEditorAvailable = false;
//					ScheduleItemEditor sm=new ScheduleItemEditor(mUtil, this, this, commonDlg, ccMenu, mGp, mGp.scheduleInfoList.get(0));
//					sm.initDialog();
                    setContextButtonNormalMode();
                    ScheduleListEditor sm = new ScheduleListEditor(mUtil, this, this, commonDlg, ccMenu, mGp);
                    mUiHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mScheduleEditorAvailable = true;
                        }
                    }, 1000);
                }
                return true;
            case R.id.menu_top_about:
                aboutSMBSync();
                setContextButtonNormalMode();
                return true;
            case R.id.menu_top_settings:
                invokeSettingsActivity();
                setContextButtonNormalMode();
                return true;
            case R.id.menu_top_kill:
                killTerminateApplication();
                setContextButtonNormalMode();
                return true;
            case R.id.menu_top_housekeep:
                houseKeepManagementFile();
                return true;
            case R.id.menu_top_add_shortcut:
                addShortcut();
                return true;
//            case R.id.menu_top_show_battery_optimization:
//                showBatteryOptimization();
//                return true;
            case R.id.menu_top_list_storage:
                showSystemInfo();
                return true;
            case R.id.menu_top_select_storage:
                reselectSdcard("");
                return true;
            case R.id.menu_top_request_grant_coarse_location:
                mGp.setSettingGrantCoarseLocationRequired(true);
                checkLocationPermission();
                return true;
            case R.id.menu_top_start_logcat:
                LogCatUtil.startLogCat(mGp, mGp.getLogDirName(),"logcat.txt");
                return true;
            case R.id.menu_top_stop_logcat:
                LogCatUtil.stopLogCat(mGp, mUtil);
                return true;
            case R.id.menu_top_send_logcat:
                LogCatUtil.sendLogCat(mActivity, mGp, mUtil, mGp.getLogDirName(), "logcat.txt");
                return true;
        }
        if (isUiEnabled()) {
        }
        return false;
    }


    private void addShortcut() {

//        String shortcutName=getString(R.string.app_name_auto_sync);
//        Intent shortcutIntent=new Intent(Intent.ACTION_VIEW);
//        shortcutIntent.setClassName(this, ShortcutAutoSync.class.getName());
//
//        if (Build.VERSION.SDK_INT>=26) {
//            // Android 8 O API26 以降
//            Icon icon = Icon.createWithResource(getApplicationContext(), R.drawable.auto_sync);
//            ShortcutInfo shortcut = new ShortcutInfo.Builder(getApplicationContext(), shortcutName)
//                    .setShortLabel(shortcutName)
//                    .setLongLabel(shortcutName)
//                    .setIcon(icon)
//                    .setIntent(shortcutIntent)
//                    .build();
//
//            ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
//            shortcutManager.requestPinShortcut(shortcut, null); // フツーのショートカット
////            shortcutManager.addDynamicShortcuts(Arrays.asList(shortcut)); // ダイナミックショートカット
//        } else {
//            Intent intent = new Intent();
//            intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
//            intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, shortcutName);
//            Parcelable iconResource=Intent.ShortcutIconResource.fromContext(this, R.drawable.auto_sync);
//            intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);
//            intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
//            setResult(RESULT_OK, intent);
//            sendBroadcast(intent);
//        }
//
//        if (Build.VERSION.SDK_INT<26) {
//            mUiHandler.postDelayed(new Runnable(){
//                @Override
//                public void run() {
////		        Intent swhome=new Intent();
////		        swhome.setAction(Intent.ACTION_MAIN);
////		        swhome.addCategory(Intent.CATEGORY_HOME);
////		        swhome.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
////				startActivity(swhome);
//                    commonDlg.showCommonDialog(false, "I",
//                            mContext.getString(R.string.msgs_main_shortcut_shortcut_added), "", null);
//                }
//            }, 100);
//        }
    }

    private void houseKeepThreadOpenDialog() {
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
        setUiDisabled();
        mGp.progressSpinView.setVisibility(LinearLayout.VISIBLE);
        mGp.progressSpinView.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);
        mGp.progressSpinView.bringToFront();
        mGp.progressSpinSyncprof.setVisibility(TextView.GONE);
        mGp.progressSpinMsg.setText(getString(R.string.msgs_progress_spin_dlg_housekeep_running));
        mGp.progressSpinCancel.setText(getString(R.string.msgs_progress_spin_dlg_housekeep_cancel));
        mGp.progressSpinCancel.setEnabled(true);
        // CANCELボタンの指定
        mGp.progressSpinCancelListener = new View.OnClickListener() {
            public void onClick(View v) {
                NotifyEvent ntfy = new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context c, Object[] o) {
                        mTcHousekeep.setDisabled();
                        mGp.progressSpinCancel.setText(getString(R.string.msgs_progress_dlg_canceling));
                        mGp.progressSpinCancel.setEnabled(false);
                    }

                    @Override
                    public void negativeResponse(Context c, Object[] o) {
                    }
                });
                commonDlg.showCommonDialog(true, "W",
                        getString(R.string.msgs_progress_spin_dlg_housekeep_cancel_confirm),
                        "", ntfy);
            }
        };
        mGp.progressSpinCancel.setOnClickListener(mGp.progressSpinCancelListener);

        mGp.msgListView.setFastScrollEnabled(false);

        LogUtil.flushLog(mContext, mGp);
    }

    private void houseKeepThreadCloseDialog() {
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " ended");
        LogUtil.flushLog(mContext, mGp);

        mGp.progressBarCancelListener = null;
        mGp.progressBarImmedListener = null;
        mGp.progressSpinCancelListener = null;
        mGp.progressBarCancel.setOnClickListener(null);
        mGp.progressSpinCancel.setOnClickListener(null);
        mGp.progressBarImmed.setOnClickListener(null);

        mGp.progressSpinView.setVisibility(LinearLayout.GONE);

        setUiEnabled();
    }

    private int mResultLogDeleteCount = 0;

    private void houseKeepResultLog() {
        final ArrayList<String> del_list = new ArrayList<String>();
        mResultLogDeleteCount = 0;
        File rlf = new File(mGp.internalRootDirectory + "/" + APPLICATION_TAG + "/result_log");
        File[] fl = rlf.listFiles();
//		Log.v("","list="+fl);
        if (fl != null && fl.length > 0) {
            String del_msg = "", sep = "";
            for (final File ll : fl) {
                boolean found = false;
                if (mGp.syncHistoryList.size() > 0) {
                    for (SyncHistoryItem shi : mGp.syncHistoryList) {
//						Log.v("","check list="+shi.sync_result_file_path);
//						Log.v("","check file="+ll.getPath());
                        if (shi.sync_result_file_path.equals(ll.getPath())) {
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) {
                    del_list.add(ll.getPath());
                    del_msg += sep + ll.getPath();
                }
            }
            if (del_list.size() > 0) {
                NotifyEvent ntfy = new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context c, Object[] o) {
                        for (String del_fp : del_list) {
                            if (!deleteResultLogFile(del_fp)) {
                                break;
                            }
//							Log.v("","del="+ll.getPath());
                        }
                        mUtil.addLogMsg("I",
                                String.format(mContext.getString(R.string.msgs_maintenance_result_log_list_del_count), mResultLogDeleteCount));
                        synchronized (mTcHousekeep) {
                            mTcHousekeep.notify();
                        }
                    }

                    @Override
                    public void negativeResponse(Context c, Object[] o) {
                        mUtil.addLogMsg("I",
                                String.format(mContext.getString(R.string.msgs_maintenance_result_log_list_del_count), mResultLogDeleteCount));
                        synchronized (mTcHousekeep) {
                            mTcHousekeep.notify();
                        }
                    }
                });
                commonDlg.showCommonDialog(true, "W",
                        mContext.getString(R.string.msgs_maintenance_result_log_list_del_title), del_msg, ntfy);
                synchronized (mTcHousekeep) {
                    try {
                        mTcHousekeep.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private boolean deleteResultLogFile(String fp) {
        boolean result = false;
        File lf = new File(fp);
        if (lf.isDirectory()) {
            File[] fl = lf.listFiles();
            for (File item : fl) {
                if (item.isDirectory()) {
                    if (!deleteResultLogFile(item.getPath())) {
                        mUtil.addLogMsg("I", "Delete failed, path=" + item.getPath());
                        return false;
                    }
                } else {
                    result = item.delete();
                    if (result) {
                        mResultLogDeleteCount++;
                        String msg = String.format(mContext.getString(R.string.msgs_maintenance_result_log_list_del_file), item.getPath());
                        mUtil.addLogMsg("I", msg);
                    } else {
                        mUtil.addLogMsg("I", "Delete file failed, path=" + item.getPath());
                    }
                }
            }
            result = lf.delete();
            if (result) {
                mResultLogDeleteCount++;
                String msg = String.format(mContext.getString(R.string.msgs_maintenance_result_log_list_del_file), lf.getPath());
                mUtil.addLogMsg("I", msg);
            } else {
                mUtil.addLogMsg("I", "Delete directory failed, path=" + lf.getPath());
            }
        } else {
            result = lf.delete();
            if (result) {
                mResultLogDeleteCount++;
                String msg = String.format(mContext.getString(R.string.msgs_maintenance_result_log_list_del_file), lf.getPath());
                mUtil.addLogMsg("I", msg);
            } else {
                mUtil.addLogMsg("I", "Delete file failed, path=" + lf.getPath());
            }
        }
        return result;
    }

    private void houseKeepLocalFileLastModList() {
        ArrayList<FileLastModifiedTimeEntry> mCurrLastModifiedList = new ArrayList<FileLastModifiedTimeEntry>();
        ArrayList<FileLastModifiedTimeEntry> mNewLastModifiedList = new ArrayList<FileLastModifiedTimeEntry>();
        ArrayList<FileLastModifiedTimeEntry> del_list = new ArrayList<FileLastModifiedTimeEntry>();
        NotifyEvent ntfy = new NotifyEvent(mGp.appContext);
        ntfy.setListener(new NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
                String en = (String) o[0];
                mUtil.addLogMsg("W", "Duplicate local file last modified entry was ignored, name=" + en);
            }
        });
        FileLastModifiedTime.loadLastModifiedList(mGp.settingMgtFileDir, mCurrLastModifiedList, mNewLastModifiedList, ntfy);
        if (mCurrLastModifiedList.size() > 0) {
            for (FileLastModifiedTimeEntry li : mCurrLastModifiedList) {
                if (!mTcHousekeep.isEnabled()) break;
                if (li.getFilePath().startsWith(mGp.internalRootDirectory)) {
                    File lf = new File(li.getFilePath());
                    if (!lf.exists()) {
                        del_list.add(li);
                        mUtil.addDebugMsg(1, "I", "Entery was deleted, fp=" + li.getFilePath());
                    }
                }
            }
            for (FileLastModifiedTimeEntry li : del_list) {
                if (!mTcHousekeep.isEnabled()) break;
                mCurrLastModifiedList.remove(li);
            }
        }
        if (mTcHousekeep.isEnabled()) {
            mUtil.addLogMsg("I",
                    String.format(mContext.getString(R.string.msgs_maintenance_last_mod_list_del_count), del_list.size()));
            if (del_list.size() > 0)
                FileLastModifiedTime.saveLastModifiedList(mGp.settingMgtFileDir, mCurrLastModifiedList, mNewLastModifiedList);
        }
    }

    private ThreadCtrl mTcHousekeep = null;

    private void houseKeepManagementFile() {
        NotifyEvent ntfy = new NotifyEvent(mContext);
        ntfy.setListener(new NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                mTcHousekeep = new ThreadCtrl();
                Thread th2 = new Thread() {
                    @Override
                    public void run() {
                        mUtil.addLogMsg("I", mContext.getString(R.string.msgs_maintenance_last_mod_list_start_msg));
                        if (!mGp.syncThreadActive) {
                            mGp.syncThreadEnabled = false;
                            mUiHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    houseKeepThreadOpenDialog();
                                }
                            });

                            houseKeepResultLog();

                            houseKeepLocalFileLastModList();

                            String msg_txt = "";
                            if (mTcHousekeep.isEnabled()) {
                                msg_txt = mContext.getString(R.string.msgs_maintenance_last_mod_list_end_msg);
                            } else
                                msg_txt = mContext.getString(R.string.msgs_maintenance_last_mod_list_cancel_msg);
                            mUtil.addLogMsg("I", msg_txt);
                            commonDlg.showCommonDialog(false, "W", msg_txt, "", null);
                            mGp.uiHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    houseKeepThreadCloseDialog();
                                    mGp.syncThreadEnabled = true;
                                }
                            });

                        } else {
                            mUtil.addLogMsg("I", mContext.getString(R.string.msgs_maintenance_last_mod_list_can_not_start_msg));
                            commonDlg.showCommonDialog(false, "W",
                                    mContext.getString(R.string.msgs_maintenance_last_mod_list_can_not_start_msg), "", null);
                        }
                    }
                };
                th2.setPriority(Thread.MAX_PRIORITY);
                th2.start();
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
            }
        });
        if (!mGp.syncThreadActive) {
            commonDlg.showCommonDialog(true, "W",
                    mContext.getString(R.string.msgs_maintenance_last_mod_list_confirm_start_msg), "", ntfy);
        } else {
            mUtil.addLogMsg("I", mContext.getString(R.string.msgs_maintenance_last_mod_list_can_not_start_msg));
            commonDlg.showCommonDialog(false, "W",
                    mContext.getString(R.string.msgs_maintenance_last_mod_list_can_not_start_msg), "", null);
        }
    }

    private void setContextButtonNormalMode() {
        mActionBar.setIcon(R.drawable.smbsync);
        mActionBar.setHomeButtonEnabled(false);
        mActionBar.setTitle(R.string.app_name);

        mGp.syncTaskAdapter.setShowCheckBox(false);
        mGp.syncTaskAdapter.setAllItemChecked(false);
        mGp.syncTaskAdapter.notifyDataSetChanged();
        setSyncTaskContextButtonNormalMode();

        mGp.syncHistoryAdapter.setShowCheckBox(false);
        mGp.syncHistoryAdapter.setAllItemChecked(false);
        mGp.syncHistoryAdapter.notifyDataSetChanged();
        setHistoryContextButtonNormalMode();
    }

    private void processHomeButtonPress() {
        if (mCurrentTab.equals(SMBSYNC2_TAB_NAME_TASK)) {
            if (mGp.syncTaskAdapter.isShowCheckBox()) {
                mGp.syncTaskAdapter.setShowCheckBox(false);
                mGp.syncTaskAdapter.notifyDataSetChanged();

                setSyncTaskContextButtonNormalMode();
            }
        } else if (mCurrentTab.equals(SMBSYNC2_TAB_NAME_MESSAGE)) {
        } else if (mCurrentTab.equals(SMBSYNC2_TAB_NAME_HIST)) {
            if (mGp.syncHistoryAdapter.isShowCheckBox()) {
                mGp.syncHistoryAdapter.setShowCheckBox(false);
                mGp.syncHistoryAdapter.notifyDataSetChanged();
                setHistoryItemUnselectAll();

                setHistoryContextButtonNormalMode();
            }
        }
    }

    private void invokeLogManagement() {
        NotifyEvent ntfy = new NotifyEvent(mContext);
        ntfy.setListener(new NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                mGp.setSettingOptionLogEnabled((boolean) o[0]);
                reloadSettingParms();
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
            }
        });
        LogUtil.flushLog(mContext, mGp);
        LogFileListDialogFragment lfm =
                LogFileListDialogFragment.newInstance(false, getString(R.string.msgs_log_management_title));
        lfm.showDialog(getSupportFragmentManager(), lfm, mGp, ntfy);
    }

    private void importSyncTaskAndParms() {
        NotifyEvent ntfy = new NotifyEvent(this);
        ntfy.setListener(new NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                boolean[] parm = (boolean[]) o[0];
                if (parm[0]) {
                    reloadSettingParms();
                    ScheduleUtil.sendTimerRequest(mContext, SCHEDULER_INTENT_SET_TIMER);
                }
                if (parm[1]) {
                    ScheduleUtil.setSchedulerInfo(mGp);
                }
                setSyncTaskContextButtonNormalMode();
                mGp.syncTaskAdapter.setShowCheckBox(false);
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
            }
        });
        mTaskUtil.importSyncTaskListDlg(ntfy);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        mUtil.addDebugMsg(9, "i", "main onKeyDown enterd, kc=" + keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (isUiEnabled()) {
                    terminateApplication();
                } else {
                    Intent in = new Intent();
                    in.setAction(Intent.ACTION_MAIN);
                    in.addCategory(Intent.CATEGORY_HOME);
                    startActivity(in);
                }
                return true;
            // break;
            default:
                return super.onKeyDown(keyCode, event);
            // break;
        }
    }

    private void checkStorageStatus() {
        if (mGp.externalStorageAccessIsPermitted) {
            if (!mGp.externalStorageIsMounted) {
                mUtil.addLogMsg("W", getString(R.string.msgs_smbsync_main_no_external_storage));
                commonDlg.showCommonDialog(false, "W",
                        getString(R.string.msgs_smbsync_main_no_external_storage), "", null);
            }
        }
    }

    private void aboutSMBSync() {
        final Dialog dialog = new Dialog(mActivity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.about_dialog);

        final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.about_dialog_title_view);
        final TextView title = (TextView) dialog.findViewById(R.id.about_dialog_title);
        title_view.setBackgroundColor(mGp.themeColorList.dialog_title_background_color);
        title.setTextColor(mGp.themeColorList.text_color_dialog_title);
        title.setText(getString(R.string.msgs_dlg_title_about) + "(Ver " + CommonUtilities.getApplVersionName(mContext) + ")");

        // get our tabHost from the xml
        final TabHost tab_host = (TabHost) dialog.findViewById(R.id.about_tab_host);
        tab_host.setup();

        final TabWidget tab_widget = (TabWidget) dialog.findViewById(android.R.id.tabs);

        tab_widget.setStripEnabled(false);
        tab_widget.setShowDividers(LinearLayout.SHOW_DIVIDER_NONE);

        CustomTabContentView tabViewProf = new CustomTabContentView(this, getString(R.string.msgs_about_dlg_func_btn));
        tab_host.addTab(tab_host.newTabSpec("func").setIndicator(tabViewProf).setContent(android.R.id.tabcontent));

        CustomTabContentView tabViewHist = new CustomTabContentView(this, getString(R.string.msgs_about_dlg_change_btn));
        tab_host.addTab(tab_host.newTabSpec("change").setIndicator(tabViewHist).setContent(android.R.id.tabcontent));

        LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout ll_func = (LinearLayout) vi.inflate(R.layout.about_dialog_func, null);
        LinearLayout ll_change = (LinearLayout) vi.inflate(R.layout.about_dialog_change, null);

        final WebView func_view = (WebView) ll_func.findViewById(R.id.about_dialog_function);
        func_view.loadUrl("file:///android_asset/" + getString(R.string.msgs_dlg_title_about_func_desc));
        func_view.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        func_view.getSettings().setBuiltInZoomControls(true);

        final WebView change_view =
                (WebView) ll_change.findViewById(R.id.about_dialog_change_history);
        change_view.loadUrl("file:///android_asset/" + getString(R.string.msgs_dlg_title_about_change_desc));
        change_view.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        change_view.getSettings().setBuiltInZoomControls(true);

        final CustomViewPagerAdapter mAboutViewPagerAdapter = new CustomViewPagerAdapter(this,
                new WebView[]{func_view, change_view});
        final CustomViewPager mAboutViewPager = (CustomViewPager) dialog.findViewById(R.id.about_view_pager);
//	    mMainViewPager.setBackgroundColor(mThemeColorList.window_color_background);
        mAboutViewPager.setAdapter(mAboutViewPagerAdapter);
        mAboutViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
//		    	mUtil.addDebugMsg(2,"I","onPageSelected entered, pos="+position);
                tab_widget.setCurrentTab(position);
                tab_host.setCurrentTab(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
//		    	mUtil.addDebugMsg(2,"I","onPageScrollStateChanged entered, state="+state);
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
//		    	mUtil.addDebugMsg(2,"I","onPageScrolled entered, pos="+position);
            }
        });

        tab_host.setOnTabChangedListener(new OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                mUtil.addDebugMsg(2, "I", "onTabchanged entered. tab=" + tabId);
                mAboutViewPager.setCurrentItem(tab_host.getCurrentTab());
            }
        });

        final Button btnOk = (Button) dialog.findViewById(R.id.about_dialog_btn_ok);

        CommonDialog.setDlgBoxSizeLimit(dialog, true);

        // OKボタンの指定
        btnOk.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        // Cancelリスナーの指定
        dialog.setOnCancelListener(new Dialog.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface arg0) {
                btnOk.performClick();
            }
        });

        dialog.show();
    }

    private void terminateApplication() {
        if (mMainTabHost.getCurrentTabTag().equals(SMBSYNC2_TAB_NAME_TASK)) {//
            if (mGp.syncTaskAdapter.isShowCheckBox()) {
                mGp.syncTaskAdapter.setShowCheckBox(false);
                mGp.syncTaskAdapter.notifyDataSetChanged();
                setSyncTaskContextButtonNormalMode();
                return;
            }
        } else if (mMainTabHost.getCurrentTabTag().equals(SMBSYNC2_TAB_NAME_MESSAGE)) {
        } else if (mMainTabHost.getCurrentTabTag().equals(SMBSYNC2_TAB_NAME_HIST)) {
            if (mGp.syncHistoryAdapter.isShowCheckBox()) {
                mGp.syncHistoryAdapter.setShowCheckBox(false);
                mGp.syncHistoryAdapter.notifyDataSetChanged();
                setHistoryItemUnselectAll();
                setHistoryContextButtonNormalMode();
                return;
            }
        }
//		mUtil.addLogMsg("I",mContext.getString(R.string.msgs_smbsync_main_end));
        isTaskTermination = true; // exit cleanly
        finish();
    }

    private void killTerminateApplication() {

        final NotifyEvent ntfy = new NotifyEvent(mContext);
        ntfy.setListener(new NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
//				terminateApplication();
                deleteTaskData();
                LogUtil.flushLog(mContext, mGp);
                android.os.Process.killProcess(android.os.Process.myPid());
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
            }
        });
        Handler hndl = new Handler();
        hndl.post(new Runnable() {
            @Override
            public void run() {
                LogUtil.flushLog(mContext, mGp);
                commonDlg.showCommonDialog(true, "W",
                        mContext.getString(R.string.msgs_smnsync_main_kill_application), "", ntfy);
            }
        });
    }

    private void reloadSettingParms() {

        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");

        String p_dir = mGp.settingMgtFileDir;
        boolean p_light_theme = mGp.themeIsLight;
        boolean p_log_option = mGp.settingLogOption;

        mGp.loadSettingsParms();
        mGp.setLogParms(mGp);
        if ((p_log_option && !mGp.settingLogOption) || (!p_log_option && mGp.settingLogOption))
            mUtil.resetLogReceiver();

        if (!mGp.settingMgtFileDir.equals(p_dir) && mGp.settingLogOption) {// option was changed
            LogUtil.closeLog(mContext, mGp);
        }

        if ((p_light_theme && !mGp.themeIsLight) || (!p_light_theme && mGp.themeIsLight)) {
            setTheme(mGp.applicationTheme);
            mGp.themeColorList = ThemeUtil.getThemeColorList(mActivity);
            screenReload(false);
        }

        if (mGp.settingFixDeviceOrientationToPortrait)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        else setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        checkJcifsOptionChanged();

    }

    private void listSettingsOption() {
        mUtil.addDebugMsg(1, "I", "Option: " +
                "debugLevel=" + mGp.settingDebugLevel +
                ", settingErrorOption=" + mGp.settingErrorOption +
                ", settingWifiLockRequired=" + mGp.settingWifiLockRequired +
                ", settingVibrateWhenSyncEnded=" + mGp.settingVibrateWhenSyncEnded +
                ", settingRingtoneWhenSyncEnded=" + mGp.settingRingtoneWhenSyncEnded +

                ", settingSupressAppSpecifiDirWarning=" + mGp.settingSupressAppSpecifiDirWarning +
//				", settingSuppressShortcutWarning="+mGp.settingSuppressShortcutWarning+
                ", settingFixDeviceOrientationToPortrait=" + mGp.settingFixDeviceOrientationToPortrait +
                ", settingExportedProfileEncryptRequired=" + mGp.settingExportedProfileEncryptRequired +

                ", settingLogOption=" + mGp.settingLogOption +
                ", settingMgtFileDir=" + mGp.settingMgtFileDir +
                ", settingLogMsgFilename=" + mGp.settingLogMsgFilename +
                ", settiingLogGeneration=" + mGp.settingLogMaxFileCount +

                ", settingExitClean=" + mGp.settingExitClean +
                "");
    }

    private void invokeLogFileBrowser() {
        mUtil.addDebugMsg(1, "I", "Invoke log file browser.");
        LogUtil.flushLog(mContext, mGp);
        if (mGp.settingLogOption) {
            Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
            intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
//                startActivity(intent);
                if (Build.VERSION.SDK_INT>=26) {
                    Uri uri= FileProvider.getUriForFile(mContext, BuildConfig.APPLICATION_ID + ".provider", new File(LogUtil.getLogFilePath(mGp)));
                    intent.setDataAndType(uri, "text/plain");
//                    startActivity(Intent.createChooser(intent, LogUtil.getLogFilePath(mGp)));
                } else {
                    intent.setDataAndType(Uri.parse("file://"+LogUtil.getLogFilePath(mGp)), "text/plain");
                }
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                commonDlg.showCommonDialog(false, "E",
                        mContext.getString(R.string.msgs_log_file_browse_app_can_not_found), e.getMessage(), null);
            }
        }
    }

    private void invokeSettingsActivity() {
        mUtil.addDebugMsg(1, "I", "Invoke Settings.");
        Intent intent = null;
        intent = new Intent(this, ActivitySettings.class);
        startActivityForResult(intent, 0);
    }

    private boolean mIsStorageSelectorActivityNotFound = false;

    public void invokeSdcardSelector(final NotifyEvent p_ntfy) {
        mSafSelectActivityNotify = p_ntfy;
        NotifyEvent ntfy = new NotifyEvent(mContext);
        ntfy.setListener(new NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                startSdcardSelectorActivity();
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
            }
        });
        mTaskUtil.showSelectSdcardMsg(ntfy);

    }

    private final int REQUEST_PERMISSIONS_WRITE_EXTERNAL_STORAGE = 1;
    private final int REQUEST_PERMISSIONS_ACCESS_COARSE_LOCATION = 2;

    private void checkRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= 23) {
            mUtil.addDebugMsg(1, "I", "Prermission WriteExternalStorage=" + checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) +
                    ", WakeLock=" + checkSelfPermission(Manifest.permission.WAKE_LOCK)
            );
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                NotifyEvent ntfy = new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context c, Object[] o) {
                        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                REQUEST_PERMISSIONS_WRITE_EXTERNAL_STORAGE);
                    }

                    @Override
                    public void negativeResponse(Context c, Object[] o) {
                        NotifyEvent ntfy_term = new NotifyEvent(mContext);
                        ntfy_term.setListener(new NotifyEventListener() {
                            @Override
                            public void positiveResponse(Context c, Object[] o) {
                                isTaskTermination = true;
                                finish();
                            }

                            @Override
                            public void negativeResponse(Context c, Object[] o) {
                            }
                        });
                        commonDlg.showCommonDialog(false, "W",
                                mContext.getString(R.string.msgs_main_permission_external_storage_title),
                                mContext.getString(R.string.msgs_main_permission_external_storage_denied_msg), ntfy_term);
                    }
                });
                commonDlg.showCommonDialog(false, "W",
                        mContext.getString(R.string.msgs_main_permission_external_storage_title),
                        mContext.getString(R.string.msgs_main_permission_external_storage_request_msg), ntfy);
            }
        }
        checkLocationPermission();
    }

    private void checkLocationPermission() {
        if (Build.VERSION.SDK_INT >= 27) {
            mUtil.addDebugMsg(1, "I", "Prermission LocationCoarse=" + checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)+
                                    ", settingGrantCoarseLocationRequired="+mGp.settingGrantCoarseLocationRequired);
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED && mGp.settingGrantCoarseLocationRequired) {
                NotifyEvent ntfy = new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context c, Object[] o) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                REQUEST_PERMISSIONS_ACCESS_COARSE_LOCATION);
                    }

                    @Override
                    public void negativeResponse(Context c, Object[] o) {
//                        NotifyEvent ntfy_deny=new NotifyEvent(mContext);
//                        ntfy_deny.setListener(new NotifyEventListener() {
//                            @Override
//                            public void positiveResponse(Context context, Object[] objects) {
//                                mGp.setSettingGrantCoarseLocationRequired(false);
//                            }
//                            @Override
//                            public void negativeResponse(Context context, Object[] objects) {}
//                        });
//                        commonDlg.showCommonDialog(true, "W",
//                                mContext.getString(R.string.msgs_main_permission_coarse_location_title),
//                                mContext.getString(R.string.msgs_main_permission_coarse_location_denied_msg), ntfy_deny);
                        mGp.setSettingGrantCoarseLocationRequired(false);
                    }
                });
                commonDlg.showCommonDialog(true, "W",
                        mContext.getString(R.string.msgs_main_permission_coarse_location_title),
                        mContext.getString(R.string.msgs_main_permission_coarse_location_request_msg), ntfy);
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (REQUEST_PERMISSIONS_WRITE_EXTERNAL_STORAGE == requestCode) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mGp.initStorageStatus();
                mUiHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        checkStorageStatus();
                    }
                }, 500);
            } else {
                NotifyEvent ntfy_term = new NotifyEvent(mContext);
                ntfy_term.setListener(new NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context c, Object[] o) {
                        isTaskTermination = true;
                        finish();
                    }

                    @Override
                    public void negativeResponse(Context c, Object[] o) {
                    }
                });
                commonDlg.showCommonDialog(false, "W",
                        mContext.getString(R.string.msgs_main_permission_external_storage_title),
                        mContext.getString(R.string.msgs_main_permission_external_storage_denied_msg), ntfy_term);
            }
        }
        if (REQUEST_PERMISSIONS_ACCESS_COARSE_LOCATION == requestCode) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                NotifyEvent ntfy_deny=new NotifyEvent(mContext);
                ntfy_deny.setListener(new NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context context, Object[] objects) {
                        mGp.setSettingGrantCoarseLocationRequired(false);
                    }
                    @Override
                    public void negativeResponse(Context context, Object[] objects) {}
                });
                commonDlg.showCommonDialog(false, "W",
                        mContext.getString(R.string.msgs_main_permission_coarse_location_title),
                        mContext.getString(R.string.msgs_main_permission_coarse_location_denied_msg), ntfy_deny);
            }
        }
    }

    private NotifyEvent mSafSelectActivityNotify = null;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0) {
            mUtil.addDebugMsg(1, "I", "Return from Settings.");
            reloadSettingParms();
            if (mGp.syncTaskAdapter.isShowCheckBox()) setSyncTaskContextButtonSelectMode();
            else setSyncTaskContextButtonNormalMode();
        } else if (requestCode == (ACTIVITY_REQUEST_CODE_SDCARD_STORAGE_ACCESS + 1)) {
            mUtil.addDebugMsg(1, "I", "Return from Storage Picker. id=" + requestCode);
        } else if (requestCode == ACTIVITY_REQUEST_CODE_SDCARD_STORAGE_ACCESS) {
            mUtil.addDebugMsg(1, "I", "Return from Storage Picker. id=" + requestCode + ", result=" + resultCode);
            if (resultCode == Activity.RESULT_OK) {
                mUtil.addDebugMsg(1, "I", "Intent=" + data.getData().toString());
//                if (SafManager.getUuidFromUri(data.getData().toString()).equals("0000-0000")) {
//                    reselectSdcard(mContext.getString(R.string.msgs_main_external_sdcard_select_uuid_invalid_msg));
//                } else
                if (mGp.safMgr.isUsbUuid(SafManager.getUuidFromUri(data.getData().toString()))) {
                    mUtil.addDebugMsg(1, "I", "Selected UUID="+SafManager.getUuidFromUri(data.getData().toString()));
                    mUtil.addDebugMsg(1, "I", "SafMessage="+mGp.safMgr.getMessages());
                    reselectSdcard(mContext.getString(R.string.msgs_main_external_sdcard_select_retry_select_msg));
                } else {
                    mUtil.addDebugMsg(1, "I", "Selected UUID="+SafManager.getUuidFromUri(data.getData().toString()));
                    mUtil.addDebugMsg(1, "I", "SafMessage="+mGp.safMgr.getMessages());
                    if (mGp.safMgr.isRootTreeUri(data.getData())) {
                        boolean rc=mGp.safMgr.addSdcardUuid(data.getData());
                        if (!rc) {
                            String saf_msg=mGp.safMgr.getMessages();
                            commonDlg.showCommonDialog(false, "W", "External SDCARD UUID registration failed, please reselect SDCARD", saf_msg, null);
                            mUtil.addLogMsg("E", "External SDCARD UUID registration failed, please reselect SDCARD\n", saf_msg);
                        }
                        mGp.syncTaskAdapter.notifyDataSetChanged();
                        if (mSafSelectActivityNotify != null)
                            mSafSelectActivityNotify.notifyToListener(true, new Object[]{data.getData()});
                    } else {
                        reselectSdcard(mContext.getString(R.string.msgs_main_external_sdcard_select_retry_select_msg));
                    }
                }
            } else {
                if (mGp.safMgr.getSdcardRootSafFile() == null && !mIsStorageSelectorActivityNotFound) {
                    SyncTaskItem pli = SyncTaskUtil.getExternalSdcardUsedSyncTask(mGp);
                    if (pli != null) {
                        String msg = String.format(mContext.getString(R.string.msgs_main_external_sdcard_select_required_cancel_msg),
                                pli.getSyncTaskName());
                        commonDlg.showCommonDialog(false, "W",
                                mContext.getString(R.string.msgs_main_external_sdcard_select_required_title),
                                msg,
                                null);
                    }
                }
            }
        }
    }

    private void startSdcardSelectorActivity() {
        try {
            mIsStorageSelectorActivityNotFound = false;
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            startActivityForResult(intent, ACTIVITY_REQUEST_CODE_SDCARD_STORAGE_ACCESS);
        } catch (Exception e) {
            mIsStorageSelectorActivityNotFound = true;
            commonDlg.showCommonDialog(false, "E",
                    mContext.getString(R.string.msgs_main_external_sdcard_select_required_title),
                    mContext.getString(R.string.msgs_main_external_sdcard_select_activity_not_found_msg),
                    null);
        }
    }

    private void reselectSdcard(String msg) {
        NotifyEvent ntfy_retry = new NotifyEvent(mContext);
        ntfy_retry.setListener(new NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                NotifyEvent ntfy = new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context c, Object[] o) {
                        startSdcardSelectorActivity();
                    }
                    @Override
                    public void negativeResponse(Context c, Object[] o) {
                        if (!mGp.safMgr.isSdcardMounted()) {
                            SyncTaskItem pli = SyncTaskUtil.getExternalSdcardUsedSyncTask(mGp);
                            if (pli != null) {
                                String msg = String.format(mContext.getString(R.string.msgs_main_external_sdcard_select_required_cancel_msg),
                                        pli.getSyncTaskName());
                                commonDlg.showCommonDialog(false, "W",
                                        mContext.getString(R.string.msgs_main_external_sdcard_select_required_title),
                                        msg,
                                        null);
                            }
                        }
                    }
                });
                mTaskUtil.showSelectSdcardMsg(ntfy);
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
                if (!mGp.safMgr.isSdcardMounted()) {
                    SyncTaskItem pli = SyncTaskUtil.getExternalSdcardUsedSyncTask(mGp);
                    if (pli != null) {
                        String msg = String.format(mContext.getString(R.string.msgs_main_external_sdcard_select_required_cancel_msg),
                                pli.getSyncTaskName());
                        commonDlg.showCommonDialog(false, "W",
                                mContext.getString(R.string.msgs_main_external_sdcard_select_required_title),
                                msg,
                                null);
                    }
                }
            }
        });
        if (msg.equals("")) ntfy_retry.notifyToListener(true, null);
        else commonDlg.showCommonDialog(true, "W", msg, "", ntfy_retry);

    }

    private void setHistoryViewItemClickListener() {
        mGp.syncHistoryListView.setEnabled(true);
        mGp.syncHistoryListView
                .setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view,
                                            int position, long id) {
                        mGp.syncHistoryListView.setEnabled(false);
                        SyncHistoryItem item = mGp.syncHistoryAdapter.getItem(position);
                        if (mGp.syncHistoryAdapter.isShowCheckBox()) {
                            item.isChecked = !item.isChecked;
                            setHistoryContextButtonSelectMode();
                            mGp.syncHistoryListView.setEnabled(true);
                        } else {
                            if (item.sync_result_file_path!=null && !item.sync_result_file_path.equals("")) {
                                File lf=new File(item.sync_result_file_path);
                                if (lf.exists()) {
                                    Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
                                    if (Build.VERSION.SDK_INT>=26) {
                                        Uri uri=FileProvider.getUriForFile(mContext, BuildConfig.APPLICATION_ID + ".provider", new File(item.sync_result_file_path));
                                        intent.setDataAndType(uri, "text/plain");
                                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    } else {
                                        intent.setDataAndType(Uri.parse("file://"+item.sync_result_file_path),"text/plain");
                                    }
                                    mActivity.startActivity(intent);
                                }
                            }
                            mUiHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    mGp.syncHistoryListView.setEnabled(true);
                                }
                            }, 1000);
                        }
                        mGp.syncHistoryAdapter.notifyDataSetChanged();
                    }
                });

        NotifyEvent ntfy = new NotifyEvent(mContext);
        ntfy.setListener(new NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                setHistoryContextButtonSelectMode();
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
            }
        });
        mGp.syncHistoryAdapter.setNotifyCheckBoxEventHandler(ntfy);
    }

    private void setHistoryViewLongClickListener() {
        mGp.syncHistoryListView
                .setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                                                   int pos, long arg3) {
                        if (mGp.syncHistoryAdapter.isEmptyAdapter()) return true;
                        if (!isUiEnabled()) return true;

                        if (!mGp.syncHistoryAdapter.getItem(pos).isChecked) {
                            if (mGp.syncHistoryAdapter.isAnyItemSelected()) {
                                int down_sel_pos = -1, up_sel_pos = -1;
                                int tot_cnt = mGp.syncHistoryAdapter.getCount();
                                if (pos + 1 <= tot_cnt) {
                                    for (int i = pos + 1; i < tot_cnt; i++) {
                                        if (mGp.syncHistoryAdapter.getItem(i).isChecked) {
                                            up_sel_pos = i;
                                            break;
                                        }
                                    }
                                }
                                if (pos > 0) {
                                    for (int i = pos; i >= 0; i--) {
                                        if (mGp.syncHistoryAdapter.getItem(i).isChecked) {
                                            down_sel_pos = i;
                                            break;
                                        }
                                    }
                                }
//						Log.v("","up="+up_sel_pos+", down="+down_sel_pos);
                                if (up_sel_pos != -1 && down_sel_pos == -1) {
                                    for (int i = pos; i < up_sel_pos; i++)
                                        mGp.syncHistoryAdapter.getItem(i).isChecked = true;
                                } else if (up_sel_pos != -1 && down_sel_pos != -1) {
                                    for (int i = down_sel_pos + 1; i < up_sel_pos; i++)
                                        mGp.syncHistoryAdapter.getItem(i).isChecked = true;
                                } else if (up_sel_pos == -1 && down_sel_pos != -1) {
                                    for (int i = down_sel_pos + 1; i <= pos; i++)
                                        mGp.syncHistoryAdapter.getItem(i).isChecked = true;
                                }
                                mGp.syncHistoryAdapter.notifyDataSetChanged();
                            } else {
                                mGp.syncHistoryAdapter.setShowCheckBox(true);
                                mGp.syncHistoryAdapter.getItem(pos).isChecked = true;
                                mGp.syncHistoryAdapter.notifyDataSetChanged();
                            }
                            setHistoryContextButtonSelectMode();
                        }
                        return true;
                    }
                });
    }

    private void sendHistoryFile() {
        final String zip_file_name = mGp.getLogDirName() + "log.zip";

        int no_of_files = 0;
        for (int i = 0; i < mGp.syncHistoryAdapter.getCount(); i++) {
//			Log.v("","name="+mGp.syncHistoryAdapter.getItem(i).sync_result_file_path);
            if (mGp.syncHistoryAdapter.getItem(i).isChecked && !mGp.syncHistoryAdapter.getItem(i).sync_result_file_path.equals("")) {
                no_of_files++;
            }
        }

        if (no_of_files == 0) {
            MessageDialogFragment mdf = MessageDialogFragment.newInstance(false, "E",
                    mContext.getString(R.string.msgs_main_sync_history_result_log_not_found),
                    "");
            mdf.showDialog(getSupportFragmentManager(), mdf, null);
            return;
        }


//		Log.v("","file="+no_of_files);
        final String[] file_name = new String[no_of_files];
        int files_pos = 0;
        for (int i = 0; i < mGp.syncHistoryAdapter.getCount(); i++) {
            if (mGp.syncHistoryAdapter.getItem(i).isChecked && !mGp.syncHistoryAdapter.getItem(i).sync_result_file_path.equals("")) {
                file_name[files_pos] = mGp.syncHistoryAdapter.getItem(i).sync_result_file_path;
                files_pos++;
            }
        }

        final ThreadCtrl tc = new ThreadCtrl();
        NotifyEvent ntfy = new NotifyEvent(mContext);
        ntfy.setListener(new NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
                tc.setDisabled();
            }
        });

        final ProgressBarDialogFragment pbdf = ProgressBarDialogFragment.newInstance(
                mContext.getString(R.string.msgs_log_file_list_dlg_send_zip_file_creating),
                "",
                mContext.getString(R.string.msgs_common_dialog_cancel),
                mContext.getString(R.string.msgs_common_dialog_cancel));
        pbdf.showDialog(getSupportFragmentManager(), pbdf, ntfy, true);
        Thread th = new Thread() {
            @Override
            public void run() {
                File lf = new File(zip_file_name);
                lf.delete();
                String[] lmp = LocalMountPoint.convertFilePathToMountpointFormat(mContext, file_name[0]);
                ZipUtil.createZipFile(mContext, tc, pbdf, zip_file_name, lmp[0], file_name);
                if (tc.isEnabled()) {
                    Intent intent = new Intent();
                    intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.setAction(Intent.ACTION_SEND);
//				    intent.setType("message/rfc822");
//				    intent.setType("text/plain");
                    intent.setType("application/zip");

                    if (Build.VERSION.SDK_INT>=26) {
                        Uri uri=FileProvider.getUriForFile(mContext, BuildConfig.APPLICATION_ID + ".provider", lf);
                        intent.putExtra(Intent.EXTRA_STREAM, uri);
                    } else {
                        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(lf));
                    }
                    mContext.startActivity(intent);

                } else {
                    lf.delete();
                    MessageDialogFragment mdf = MessageDialogFragment.newInstance(false, "W",
                            mContext.getString(R.string.msgs_log_file_list_dlg_send_zip_file_cancelled),
                            "");
                    mdf.showDialog(getSupportFragmentManager(), mdf, null);
                }
                pbdf.dismiss();
            }

            ;
        };
        th.start();
    }

    private void setHistoryContextButtonListener() {
        mContextHistoryButtonSendTo.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setContextButtonEnabled(mContextHistoryButtonSendTo, false);
                if (isUiEnabled()) {
                    sendHistoryFile();
                    mGp.syncHistoryAdapter.setAllItemChecked(false);
                    mGp.syncHistoryAdapter.setShowCheckBox(false);
                    mGp.syncHistoryAdapter.notifyDataSetChanged();
                    setHistoryContextButtonNormalMode();
                }
                setContextButtonEnabled(mContextHistoryButtonSendTo, true);
            }
        });
        ContextButtonUtil.setButtonLabelListener(mContext, mContextHistoryButtonSendTo, mContext.getString(R.string.msgs_hist_cont_label_share));

        mContextHistoryButtonMoveTop.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setContextButtonEnabled(mContextHistoryButtonMoveTop, false);
                mGp.syncHistoryListView.setSelection(0);
                setContextButtonEnabled(mContextHistoryButtonMoveTop, true);
            }
        });
        ContextButtonUtil.setButtonLabelListener(mContext, mContextHistoryButtonMoveTop, mContext.getString(R.string.msgs_hist_cont_label_move_top));

        mContextHistoryButtonMoveBottom.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setContextButtonEnabled(mContextHistoryButtonMoveBottom, false);
                mGp.syncHistoryListView.setSelection(mGp.syncHistoryAdapter.getCount() - 1);
                setContextButtonEnabled(mContextHistoryButtonMoveBottom, true);
            }
        });
        ContextButtonUtil.setButtonLabelListener(mContext, mContextHistoryButtonMoveBottom, mContext.getString(R.string.msgs_hist_cont_label_move_bottom));
        mContextHistoryButtonDeleteHistory.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isUiEnabled()) {
                    confirmDeleteHistory();
                }
            }
        });
        ContextButtonUtil.setButtonLabelListener(mContext, mContextHistoryButtonDeleteHistory, mContext.getString(R.string.msgs_hist_cont_label_delete));

        final Toast toast = Toast.makeText(mContext, mContext.getString(R.string.msgs_main_sync_history_copy_completed),
                Toast.LENGTH_SHORT);
        toast.setDuration(1500);
        mContextHistoryButtonHistiryCopyClipboard.setOnClickListener(new OnClickListener() {
            private long last_show_time = 0;

            @Override
            public void onClick(View v) {
                setContextButtonEnabled(mContextHistoryButtonHistiryCopyClipboard, false);
                if (isUiEnabled()) {
                    ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    StringBuilder out = new StringBuilder(256);
                    for (int i = 0; i < mGp.syncHistoryAdapter.getCount(); i++) {
                        if (mGp.syncHistoryAdapter.getItem(i).isChecked) {
                            SyncHistoryItem hli = mGp.syncHistoryAdapter.getItem(i);
                            out.append(hli.sync_date).append(" ");
                            out.append(hli.sync_time).append(" ");
                            out.append(hli.sync_prof).append("\n");
                            if (hli.sync_status == SyncHistoryItem.SYNC_STATUS_SUCCESS) {
                                out.append(mContext.getString(R.string.msgs_main_sync_history_status_success)).append("\n");
                            } else if (hli.sync_status == SyncHistoryItem.SYNC_STATUS_ERROR) {
                                out.append(mContext.getString(R.string.msgs_main_sync_history_status_error)).append("\n");
                            } else if (hli.sync_status == SyncHistoryItem.SYNC_STATUS_CANCEL) {
                                out.append(mContext.getString(R.string.msgs_main_sync_history_status_cancel)).append("\n");
                            }
                            out.append(mContext.getString(R.string.msgs_main_sync_history_count_copied))
                                    .append(Integer.toString(hli.sync_result_no_of_copied)).append(" ");
                            out.append(mContext.getString(R.string.msgs_main_sync_history_count_deleted))
                                    .append(Integer.toString(hli.sync_result_no_of_deleted)).append(" ");
                            out.append(mContext.getString(R.string.msgs_main_sync_history_count_ignored))
                                    .append(Integer.toString(hli.sync_result_no_of_ignored)).append(" ");
                            out.append("\n").append(hli.sync_error_text);
                        }
                    }
                    if (out.length() > 0) cm.setText(out);
                    if ((last_show_time + 1500) < System.currentTimeMillis()) {
                        toast.show();
                        last_show_time = System.currentTimeMillis();
                    }
                    mGp.syncHistoryAdapter.setAllItemChecked(false);
                    mGp.syncHistoryAdapter.setShowCheckBox(false);
                    mGp.syncHistoryAdapter.notifyDataSetChanged();
                    setHistoryContextButtonNormalMode();
                }
                setContextButtonEnabled(mContextHistoryButtonHistiryCopyClipboard, true);
            }
        });
        ContextButtonUtil.setButtonLabelListener(mContext, mContextHistoryButtonHistiryCopyClipboard, mContext.getString(R.string.msgs_hist_cont_label_copy));

        mContextHistiryButtonSelectAll.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isUiEnabled()) {
                    setContextButtonEnabled(mContextHistiryButtonSelectAll, false);
                    setHistoryItemSelectAll();
                    mGp.syncHistoryAdapter.setShowCheckBox(true);
                    setHistoryContextButtonSelectMode();
                    setContextButtonEnabled(mContextHistiryButtonSelectAll, true);
                }
            }
        });
        ContextButtonUtil.setButtonLabelListener(mContext, mContextHistiryButtonSelectAll, mContext.getString(R.string.msgs_hist_cont_label_select_all));

        mContextHistiryButtonUnselectAll.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isUiEnabled()) {
                    setContextButtonEnabled(mContextHistiryButtonUnselectAll, false);
                    setHistoryItemUnselectAll();
                    //				mGp.syncHistoryAdapter.setShowCheckBox(false);
                    //				setHistoryContextButtonNotselected();
                    setContextButtonEnabled(mContextHistiryButtonUnselectAll, true);
                }
            }
        });
        ContextButtonUtil.setButtonLabelListener(mContext, mContextHistiryButtonUnselectAll, mContext.getString(R.string.msgs_hist_cont_label_unselect_all));
    }

    private void setHistoryContextButtonSelectMode() {
        int sel_cnt = mGp.syncHistoryAdapter.getItemSelectedCount();
        int tot_cnt = mGp.syncHistoryAdapter.getCount();
        setActionBarSelectMode(sel_cnt, tot_cnt);

        mContextHistiryViewMoveTop.setVisibility(ImageButton.VISIBLE);
        mContextHistiryViewMoveBottom.setVisibility(ImageButton.VISIBLE);

//		if (sel_cnt==1) ll_show_log.setVisibility(ImageButton.VISIBLE);
//		else ll_show_log.setVisibility(ImageButton.INVISIBLE);
        if (sel_cnt > 0) {
            mContextHistiryViewShare.setVisibility(ImageButton.VISIBLE);
            mContextHistiryViewDeleteHistory.setVisibility(ImageButton.VISIBLE);
            mContextHistiryViewHistoryCopyClipboard.setVisibility(ImageButton.VISIBLE);
            mContextHistiryViewUnselectAll.setVisibility(ImageButton.VISIBLE);
        } else {
            mContextHistiryViewShare.setVisibility(ImageButton.INVISIBLE);
            mContextHistiryViewDeleteHistory.setVisibility(ImageButton.INVISIBLE);
            mContextHistiryViewHistoryCopyClipboard.setVisibility(ImageButton.INVISIBLE);
            mContextHistiryViewUnselectAll.setVisibility(ImageButton.INVISIBLE);
        }

        if (tot_cnt != sel_cnt) mContextHistiryViewSelectAll.setVisibility(ImageButton.VISIBLE);
        else mContextHistiryViewSelectAll.setVisibility(ImageButton.INVISIBLE);

    }

    private void setHistoryContextButtonNormalMode() {
        setActionBarNormalMode();

        if (!mGp.syncHistoryAdapter.isEmptyAdapter()) {
            mContextHistiryViewShare.setVisibility(ImageButton.INVISIBLE);
            mContextHistiryViewMoveTop.setVisibility(ImageButton.INVISIBLE);
            mContextHistiryViewMoveTop.setVisibility(ImageButton.VISIBLE);
            mContextHistiryViewMoveBottom.setVisibility(ImageButton.VISIBLE);
            mContextHistiryViewDeleteHistory.setVisibility(ImageButton.INVISIBLE);
            mContextHistiryViewHistoryCopyClipboard.setVisibility(ImageButton.INVISIBLE);
            if (isUiEnabled()) mContextHistiryViewSelectAll.setVisibility(ImageButton.VISIBLE);
            else mContextHistiryViewSelectAll.setVisibility(ImageButton.INVISIBLE);
            mContextHistiryViewUnselectAll.setVisibility(ImageButton.INVISIBLE);
        } else {
            mContextHistiryViewShare.setVisibility(ImageButton.INVISIBLE);
            mContextHistiryViewShare.setVisibility(ImageButton.INVISIBLE);
            mContextHistiryViewMoveTop.setVisibility(ImageButton.INVISIBLE);
            mContextHistiryViewMoveBottom.setVisibility(ImageButton.INVISIBLE);
            mContextHistiryViewDeleteHistory.setVisibility(ImageButton.INVISIBLE);
            mContextHistiryViewHistoryCopyClipboard.setVisibility(ImageButton.INVISIBLE);
            mContextHistiryViewSelectAll.setVisibility(ImageButton.INVISIBLE);
            mContextHistiryViewUnselectAll.setVisibility(ImageButton.INVISIBLE);
        }
    }

    private void setHistoryItemUnselectAll() {
        mGp.syncHistoryAdapter.setAllItemChecked(false);
//		for (int i=0;i<mGp.syncHistoryAdapter.getCount();i++) mGp.syncHistoryAdapter.getItem(i).isChecked=false;
//		mGp.syncHistoryAdapter.setShowCheckBox(false);
        mGp.syncHistoryAdapter.notifyDataSetChanged();
        setHistoryContextButtonSelectMode();
    }

    private void setHistoryItemSelectAll() {
        mGp.syncHistoryAdapter.setAllItemChecked(true);
//		for (int i=0;i<mGp.syncHistoryAdapter.getCount();i++) mGp.syncHistoryAdapter.getItem(i).isChecked=true;
        mGp.syncHistoryAdapter.setShowCheckBox(true);
        mGp.syncHistoryAdapter.notifyDataSetChanged();
        setHistoryContextButtonSelectMode();
    }

    @SuppressWarnings("unused")
    private void setHistoryItemChecked(int pos, boolean p) {
        mGp.syncHistoryAdapter.getItem(pos).isChecked = p;
    }

    private void confirmDeleteHistory() {
        String conf_list = "";
        boolean del_all_history = false;
        int del_cnt = 0;
        String sep = "";
        for (int i = 0; i < mGp.syncHistoryAdapter.getCount(); i++) {
            if (mGp.syncHistoryAdapter.getItem(i).isChecked) {
                del_cnt++;
                conf_list += sep + mGp.syncHistoryAdapter.getItem(i).sync_date + " " +
                        mGp.syncHistoryAdapter.getItem(i).sync_time + " " +
                        mGp.syncHistoryAdapter.getItem(i).sync_prof + " ";
                sep = "\n";
            }
        }
        if (del_cnt == mGp.syncHistoryAdapter.getCount()) del_all_history = true;
        NotifyEvent ntfy = new NotifyEvent(this);
        ntfy.setListener(new NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                for (int i = mGp.syncHistoryAdapter.getCount() - 1; i >= 0; i--) {
                    if (mGp.syncHistoryAdapter.getItem(i).isChecked) {
                        String result_fp = mGp.syncHistoryAdapter.getItem(i).sync_result_file_path;
                        if (!result_fp.equals("")) {
                            File lf = new File(result_fp);
                            if (lf.exists()) {
                                lf.delete();
                                mUtil.addDebugMsg(1, "I", "Sync history log file deleted, fp=" + result_fp);
                            }
                        }
                        mUtil.addDebugMsg(1, "I", "Sync history item deleted, item=" + mGp.syncHistoryAdapter.getItem(i).sync_prof);
                        mGp.syncHistoryAdapter.remove(mGp.syncHistoryAdapter.getItem(i));
                    }
                }
                mUtil.saveHistoryList(mGp.syncHistoryList);
//				mGp.syncHistoryAdapter.setSyncHistoryList(mUtil.loadHistoryList());
                mGp.syncHistoryAdapter.setShowCheckBox(false);
                mGp.syncHistoryAdapter.notifyDataSetChanged();
                setHistoryContextButtonNormalMode();
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
            }
        });

        if (del_all_history) {
//			subtitle=getString(R.string.msgs_main_sync_history_del_conf_subtitle);
            commonDlg.showCommonDialog(true, "W", getString(R.string.msgs_main_sync_history_del_conf_all_history),
                    "", ntfy);
        } else {
//			subtitle=getString(R.string.msgs_main_sync_history_del_conf_subtitle);
            commonDlg.showCommonDialog(true, "W", getString(R.string.msgs_main_sync_history_del_conf_selected_history),
                    conf_list, ntfy);
        }
    }

    private void setSyncTaskListItemClickListener() {
        mGp.syncTaskListView.setEnabled(true);
        mGp.syncTaskListView
                .setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view,
                                            int position, long id) {
                        if (isUiEnabled()) {
                            mGp.syncTaskListView.setEnabled(false);
                            SyncTaskItem item = mGp.syncTaskAdapter.getItem(position);
                            if (!mGp.syncTaskAdapter.isShowCheckBox()) {
                                editSyncTask(item.getSyncTaskName(), item.isSyncTaskAuto(), position);
                                mUiHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        mGp.syncTaskListView.setEnabled(true);
                                    }
                                }, 1000);
                            } else {
                                item.setChecked(!item.isChecked());
                                setSyncTaskContextButtonSelectMode();
                                mGp.syncTaskListView.setEnabled(true);
                            }
                            mGp.syncTaskAdapter.notifyDataSetChanged();
                        }
                    }
                });

        NotifyEvent ntfy = new NotifyEvent(mContext);
        ntfy.setListener(new NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                if (!mGp.syncTaskAdapter.isShowCheckBox()) {
//					syncTaskListAdapter.setShowCheckBox(false);
                    mGp.syncTaskAdapter.notifyDataSetChanged();
                    setSyncTaskContextButtonNormalMode();
                } else {
                    setSyncTaskContextButtonSelectMode();
                }
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
            }
        });
        mGp.syncTaskAdapter.setNotifyCheckBoxEventHandler(ntfy);
    }

    private void setSyncTaskListLongClickListener() {
        mGp.syncTaskListView
                .setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(final AdapterView<?> list_view, final View item_view,
                                                   int pos, long arg3) {
                        if (mGp.syncTaskAdapter.isEmptyAdapter()) return true;
                        if (!isUiEnabled()) return true;

                        if (!mGp.syncTaskAdapter.getItem(pos).isChecked()) {
                            if (SyncTaskUtil.isSyncTaskSelected(mGp.syncTaskAdapter)) {

                                int down_sel_pos = -1, up_sel_pos = -1;
                                int tot_cnt = mGp.syncTaskAdapter.getCount();
                                if (pos + 1 <= tot_cnt) {
                                    for (int i = pos + 1; i < tot_cnt; i++) {
                                        if (mGp.syncTaskAdapter.getItem(i).isChecked()) {
                                            up_sel_pos = i;
                                            break;
                                        }
                                    }
                                }
                                if (pos > 0) {
                                    for (int i = pos; i >= 0; i--) {
                                        if (mGp.syncTaskAdapter.getItem(i).isChecked()) {
                                            down_sel_pos = i;
                                            break;
                                        }
                                    }
                                }
//						Log.v("","up="+up_sel_pos+", down="+down_sel_pos);
                                if (up_sel_pos != -1 && down_sel_pos == -1) {
                                    for (int i = pos; i < up_sel_pos; i++)
                                        mGp.syncTaskAdapter.getItem(i).setChecked(true);
                                } else if (up_sel_pos != -1 && down_sel_pos != -1) {
                                    for (int i = down_sel_pos + 1; i < up_sel_pos; i++)
                                        mGp.syncTaskAdapter.getItem(i).setChecked(true);
                                } else if (up_sel_pos == -1 && down_sel_pos != -1) {
                                    for (int i = down_sel_pos + 1; i <= pos; i++)
                                        mGp.syncTaskAdapter.getItem(i).setChecked(true);
                                }
                                mGp.syncTaskAdapter.notifyDataSetChanged();
                            } else {
                                mGp.syncTaskAdapter.setShowCheckBox(true);
                                mGp.syncTaskAdapter.getItem(pos).setChecked(true);
                                mGp.syncTaskAdapter.notifyDataSetChanged();
                            }
                            setSyncTaskContextButtonSelectMode();
                        }
                        return true;
                    }
                });
    }

    private ImageButton mContextSyncTaskButtonActivete = null;
    private ImageButton mContextSyncTaskButtonInactivete = null;
    private ImageButton mContextSyncTaskButtonAddSync = null;
    private ImageButton mContextSyncTaskButtonCopySyncTask = null;
    private ImageButton mContextSyncTaskButtonDeleteSyncTask = null;
    private ImageButton mContextSyncTaskButtonRenameSyncTask = null;
    private ImageButton mContextSyncTaskButtonMoveToUp = null;
    private ImageButton mContextSyncTaskButtonMoveToDown = null;
    private ImageButton mContextSyncTaskButtonSelectAll = null;
    private ImageButton mContextSyncTaskButtonUnselectAll = null;

//    private Bitmap mContextSyncTaskBitmapActive=null;
//    private Bitmap mContextSyncTaskBitmapInactive=null;
//    private Bitmap mContextSyncTaskBitmapAddLocal=null;
//    private Bitmap mContextSyncTaskBitmapAddRemote=null;
//    private Bitmap mContextSyncTaskBitmapAddSync=null;
//    private Bitmap mContextSyncTaskBitmapStartWizard=null;
//    private Bitmap mContextSyncTaskBitmapCopyProfile=null;
//    private Bitmap mContextSyncTaskBitmapDeleteProfile=null;
//    private Bitmap mContextSyncTaskBitmapRenameProfile=null;
//    private Bitmap mContextSyncTaskBitmapSync=null;
//    private Bitmap mContextSyncTaskBitmapSelectAll=null;
//    private Bitmap mContextSyncTaskBitmapUnselectAll=null;

    private LinearLayout mContextSyncTaskViewActivete = null;
    private LinearLayout mContextSyncTaskViewInactivete = null;
    private LinearLayout mContextSyncTaskViewAddSync = null;
    private LinearLayout mContextSyncTaskViewCopySyncTask = null;
    private LinearLayout mContextSyncTaskViewDeleteSyncTask = null;
    private LinearLayout mContextSyncTaskViewRenameSyncTask = null;
    private LinearLayout mContextSyncTaskViewMoveToUp = null;
    private LinearLayout mContextSyncTaskViewMoveToDown = null;
    private LinearLayout mContextSyncTaskViewSelectAll = null;
    private LinearLayout mContextSyncTaskViewUnselectAll = null;

    private ImageButton mContextHistoryButtonSendTo = null;
    private ImageButton mContextHistoryButtonMoveTop = null;
    private ImageButton mContextHistoryButtonMoveBottom = null;
    private ImageButton mContextHistoryButtonDeleteHistory = null;
    private ImageButton mContextHistoryButtonHistiryCopyClipboard = null;
    private ImageButton mContextHistiryButtonSelectAll = null;
    private ImageButton mContextHistiryButtonUnselectAll = null;

    private LinearLayout mContextHistiryViewShare = null;
    private LinearLayout mContextHistiryViewMoveTop = null;
    private LinearLayout mContextHistiryViewMoveBottom = null;
    private LinearLayout mContextHistiryViewDeleteHistory = null;
    private LinearLayout mContextHistiryViewHistoryCopyClipboard = null;
    private LinearLayout mContextHistiryViewSelectAll = null;
    private LinearLayout mContextHistiryViewUnselectAll = null;

    private ImageButton mContextMessageButtonMoveTop = null;
    private ImageButton mContextMessageButtonPinned = null;
    private ImageButton mContextMessageButtonMoveBottom = null;
    private ImageButton mContextMessageButtonClear = null;

    private LinearLayout mContextMessageViewMoveTop = null;
    private LinearLayout mContextMessageViewPinned = null;
    private LinearLayout mContextMessageViewMoveBottom = null;
    private LinearLayout mContextMessageViewClear = null;

    private void releaseImageResource() {
        releaseImageBtnRes(mContextSyncTaskButtonActivete);
        releaseImageBtnRes(mContextSyncTaskButtonInactivete);
        releaseImageBtnRes(mContextSyncTaskButtonAddSync);
        releaseImageBtnRes(mContextSyncTaskButtonCopySyncTask);
        releaseImageBtnRes(mContextSyncTaskButtonDeleteSyncTask);
        releaseImageBtnRes(mContextSyncTaskButtonRenameSyncTask);
        releaseImageBtnRes(mContextSyncTaskButtonMoveToUp);
        releaseImageBtnRes(mContextSyncTaskButtonMoveToDown);
        releaseImageBtnRes(mContextSyncTaskButtonSelectAll);
        releaseImageBtnRes(mContextSyncTaskButtonUnselectAll);

        releaseImageBtnRes(mContextHistoryButtonSendTo);
        releaseImageBtnRes(mContextHistoryButtonMoveTop);
        releaseImageBtnRes(mContextHistoryButtonMoveBottom);
        releaseImageBtnRes(mContextHistoryButtonDeleteHistory);
        releaseImageBtnRes(mContextHistoryButtonHistiryCopyClipboard);
        releaseImageBtnRes(mContextHistiryButtonSelectAll);
        releaseImageBtnRes(mContextHistiryButtonUnselectAll);

        releaseImageBtnRes(mContextMessageButtonMoveTop);
        releaseImageBtnRes(mContextMessageButtonPinned);
        releaseImageBtnRes(mContextMessageButtonMoveBottom);
        releaseImageBtnRes(mContextMessageButtonClear);

        mGp.syncTaskListView.setAdapter(null);
        mGp.syncHistoryListView.setAdapter(null);
    }

    private void releaseImageBtnRes(ImageButton ib) {
//    	((BitmapDrawable) ib.getDrawable()).getBitmap().recycle();
        ib.setImageDrawable(null);
//    	ib.setBackground(null);
        ib.setBackgroundDrawable(null);
        ib.setImageBitmap(null);
    }

    private void createContextView() {
        mContextSyncTaskButtonActivete = (ImageButton) mSyncTaskView.findViewById(R.id.context_button_activate);
        mContextSyncTaskButtonInactivete = (ImageButton) mSyncTaskView.findViewById(R.id.context_button_inactivate);
        mContextSyncTaskButtonAddSync = (ImageButton) mSyncTaskView.findViewById(R.id.context_button_add_sync);
        mContextSyncTaskButtonCopySyncTask = (ImageButton) mSyncTaskView.findViewById(R.id.context_button_copy);
        mContextSyncTaskButtonDeleteSyncTask = (ImageButton) mSyncTaskView.findViewById(R.id.context_button_delete);
        mContextSyncTaskButtonRenameSyncTask = (ImageButton) mSyncTaskView.findViewById(R.id.context_button_rename);
        mContextSyncTaskButtonMoveToUp = (ImageButton) mSyncTaskView.findViewById(R.id.context_button_up_arrow);
        mContextSyncTaskButtonMoveToDown = (ImageButton) mSyncTaskView.findViewById(R.id.context_button_down_arrow);
        mContextSyncTaskButtonSelectAll = (ImageButton) mSyncTaskView.findViewById(R.id.context_button_select_all);
        mContextSyncTaskButtonUnselectAll = (ImageButton) mSyncTaskView.findViewById(R.id.context_button_unselect_all);

        mContextSyncTaskViewActivete = (LinearLayout) mSyncTaskView.findViewById(R.id.context_button_activate_view);
        mContextSyncTaskViewInactivete = (LinearLayout) mSyncTaskView.findViewById(R.id.context_button_inactivate_view);
        mContextSyncTaskViewAddSync = (LinearLayout) mSyncTaskView.findViewById(R.id.context_button_add_sync_view);
        mContextSyncTaskViewCopySyncTask = (LinearLayout) mSyncTaskView.findViewById(R.id.context_button_copy_view);
        mContextSyncTaskViewDeleteSyncTask = (LinearLayout) mSyncTaskView.findViewById(R.id.context_button_delete_view);
        mContextSyncTaskViewRenameSyncTask = (LinearLayout) mSyncTaskView.findViewById(R.id.context_button_rename_view);
        mContextSyncTaskViewMoveToUp = (LinearLayout) mSyncTaskView.findViewById(R.id.context_button_up_arrow_view);
        mContextSyncTaskViewMoveToDown = (LinearLayout) mSyncTaskView.findViewById(R.id.context_button_down_arrow_view);

        mContextSyncTaskViewSelectAll = (LinearLayout) mSyncTaskView.findViewById(R.id.context_button_select_all_view);
        mContextSyncTaskViewUnselectAll = (LinearLayout) mSyncTaskView.findViewById(R.id.context_button_unselect_all_view);

        mContextHistoryButtonSendTo = (ImageButton) mHistoryView.findViewById(R.id.context_button_share);
        mContextHistoryButtonMoveTop = (ImageButton) mHistoryView.findViewById(R.id.context_button_move_to_top);
        mContextHistoryButtonMoveBottom = (ImageButton) mHistoryView.findViewById(R.id.context_button_move_to_bottom);
        mContextHistoryButtonDeleteHistory = (ImageButton) mHistoryView.findViewById(R.id.context_button_delete);
        mContextHistoryButtonHistiryCopyClipboard = (ImageButton) mHistoryView.findViewById(R.id.context_button_copy_to_clipboard);
        mContextHistiryButtonSelectAll = (ImageButton) mHistoryView.findViewById(R.id.context_button_select_all);
        mContextHistiryButtonUnselectAll = (ImageButton) mHistoryView.findViewById(R.id.context_button_unselect_all);

        mContextHistiryViewShare = (LinearLayout) mHistoryView.findViewById(R.id.context_button_share_view);
        mContextHistiryViewMoveTop = (LinearLayout) mHistoryView.findViewById(R.id.context_button_move_to_top_view);
        mContextHistiryViewMoveBottom = (LinearLayout) mHistoryView.findViewById(R.id.context_button_move_to_bottom_view);
        mContextHistiryViewDeleteHistory = (LinearLayout) mHistoryView.findViewById(R.id.context_button_delete_view);
        mContextHistiryViewHistoryCopyClipboard = (LinearLayout) mHistoryView.findViewById(R.id.context_button_copy_to_clipboard_view);
        mContextHistiryViewSelectAll = (LinearLayout) mHistoryView.findViewById(R.id.context_button_select_all_view);
        mContextHistiryViewUnselectAll = (LinearLayout) mHistoryView.findViewById(R.id.context_button_unselect_all_view);

        mContextMessageButtonPinned = (ImageButton) mMessageView.findViewById(R.id.context_button_pinned);
        mContextMessageButtonMoveTop = (ImageButton) mMessageView.findViewById(R.id.context_button_move_to_top);
        mContextMessageButtonMoveBottom = (ImageButton) mMessageView.findViewById(R.id.context_button_move_to_bottom);
        mContextMessageButtonClear = (ImageButton) mMessageView.findViewById(R.id.context_button_clear);

        mContextMessageViewPinned = (LinearLayout) mMessageView.findViewById(R.id.context_button_pinned_view);
        mContextMessageViewMoveTop = (LinearLayout) mMessageView.findViewById(R.id.context_button_move_to_top_view);
        mContextMessageViewMoveBottom = (LinearLayout) mMessageView.findViewById(R.id.context_button_move_to_bottom_view);
        mContextMessageViewClear = (LinearLayout) mMessageView.findViewById(R.id.context_button_clear_view);
    }

    private void setContextButtonEnabled(final ImageButton btn, boolean enabled) {
        if (enabled) {
            btn.postDelayed(new Runnable() {
                @Override
                public void run() {
                    btn.setEnabled(true);
                }
            }, 1000);
        } else {
            btn.setEnabled(false);
        }
    }

    private void setSyncTaskContextButtonListener() {
        final NotifyEvent ntfy = new NotifyEvent(mContext);
        ntfy.setListener(new NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                if (mGp.syncTaskAdapter.isShowCheckBox()) setSyncTaskContextButtonSelectMode();
                else setSyncTaskContextButtonNormalMode();
//				checkSafExternalSdcardTreeUri(null);
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
//				checkSafExternalSdcardTreeUri(null);
            }
        });

        mContextSyncTaskButtonActivete.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isUiEnabled()) confirmActivate(mGp.syncTaskAdapter, ntfy);
            }
        });
        ContextButtonUtil.setButtonLabelListener(mContext, mContextSyncTaskButtonActivete, mContext.getString(R.string.msgs_prof_cont_label_activate));

        mContextSyncTaskButtonInactivete.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isUiEnabled()) confirmInactivate(mGp.syncTaskAdapter, ntfy);
            }
        });
        ContextButtonUtil.setButtonLabelListener(mContext, mContextSyncTaskButtonInactivete, mContext.getString(R.string.msgs_prof_cont_label_inactivate));

        mContextSyncTaskButtonAddSync.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isUiEnabled()) {
                    setContextButtonEnabled(mContextSyncTaskButtonAddSync, false);
                    SyncTaskItem pfli = new SyncTaskItem();
//					pfli.setMasterDirectoryName("from");
//					pfli.setTargetDirectoryName("to");
                    pfli.setSyncTaskAuto(true);
                    pfli.setMasterLocalMountPoint(mGp.internalRootDirectory);
                    pfli.setTargetLocalMountPoint(mGp.internalRootDirectory);
                    pfli.setSyncUseExtendedDirectoryFilter1(true);
                    SyncTaskEditor pmsp = SyncTaskEditor.newInstance();
                    pmsp.showDialog(getSupportFragmentManager(), pmsp, "ADD", pfli,
                            mTaskUtil, mUtil, commonDlg, mGp, ntfy);
                    setContextButtonEnabled(mContextSyncTaskButtonAddSync, true);
                }
            }
        });
        ContextButtonUtil.setButtonLabelListener(mContext, mContextSyncTaskButtonAddSync, mContext.getString(R.string.msgs_prof_cont_label_add_sync));

        mContextSyncTaskButtonCopySyncTask.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isUiEnabled()) {
                    setContextButtonEnabled(mContextSyncTaskButtonCopySyncTask, false);
                    for (int i = 0; i < mGp.syncTaskAdapter.getCount(); i++) {
                        SyncTaskItem item = mGp.syncTaskAdapter.getItem(i);
                        if (item.isChecked()) {
                            mTaskUtil.copySyncTask(item, ntfy);
                            break;
                        }
                    }
                    setContextButtonEnabled(mContextSyncTaskButtonCopySyncTask, true);
                }
            }
        });
        ContextButtonUtil.setButtonLabelListener(mContext, mContextSyncTaskButtonCopySyncTask, mContext.getString(R.string.msgs_prof_cont_label_copy));

        mContextSyncTaskButtonDeleteSyncTask.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isUiEnabled()) {
                    setContextButtonEnabled(mContextSyncTaskButtonDeleteSyncTask, false);
                    mTaskUtil.deleteSyncTask(ntfy);
                    setContextButtonEnabled(mContextSyncTaskButtonDeleteSyncTask, true);
                }
            }
        });
        ContextButtonUtil.setButtonLabelListener(mContext, mContextSyncTaskButtonDeleteSyncTask, mContext.getString(R.string.msgs_prof_cont_label_delete));

        mContextSyncTaskButtonRenameSyncTask.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isUiEnabled()) {
                    setContextButtonEnabled(mContextSyncTaskButtonRenameSyncTask, false);
                    for (int i = 0; i < mGp.syncTaskAdapter.getCount(); i++) {
                        SyncTaskItem item = mGp.syncTaskAdapter.getItem(i);
                        if (item.isChecked()) {
                            mTaskUtil.renameSyncTask(item, ntfy);
                            break;
                        }
                    }
                    setContextButtonEnabled(mContextSyncTaskButtonRenameSyncTask, true);
                }
            }
        });
        ContextButtonUtil.setButtonLabelListener(mContext, mContextSyncTaskButtonRenameSyncTask, mContext.getString(R.string.msgs_prof_cont_label_rename));

        mContextSyncTaskButtonMoveToUp.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isUiEnabled()) {
                    for (int i = 0; i < mGp.syncTaskAdapter.getCount(); i++) {
                        SyncTaskItem item = mGp.syncTaskAdapter.getItem(i);
                        if (item.isChecked()) {
                            int c_pos = item.getSyncTaskPosition();
                            if (c_pos > 0) {
                                for (int j = 0; j < mGp.syncTaskAdapter.getCount(); j++) {
                                    if (mGp.syncTaskAdapter.getItem(j).getSyncTaskPosition() == (c_pos - 1)) {
                                        mGp.syncTaskAdapter.getItem(j).setSyncTaskPosition(c_pos);
                                    }
                                }
                                item.setSyncTaskPosition(c_pos - 1);
                                mGp.syncTaskAdapter.sort();
                                SyncTaskUtil.saveSyncTaskListToFile(mGp, mContext, mUtil, false, "", "", mGp.syncTaskList, false);
                                mGp.syncTaskAdapter.notifyDataSetChanged();

                                if (item.getSyncTaskPosition() == 0) {
                                    mContextSyncTaskViewMoveToUp.setVisibility(ImageButton.INVISIBLE);
                                    mContextSyncTaskViewMoveToDown.setVisibility(ImageButton.VISIBLE);
                                }
                                if (item.getSyncTaskPosition() == (mGp.syncTaskAdapter.getCount() - 1)) {
                                    mContextSyncTaskViewMoveToUp.setVisibility(ImageButton.VISIBLE);
                                    mContextSyncTaskViewMoveToDown.setVisibility(ImageButton.INVISIBLE);
                                }
                            }
                            break;
                        }
                    }
                }
            }
        });
        ContextButtonUtil.setButtonLabelListener(mContext, mContextSyncTaskButtonMoveToUp, mContext.getString(R.string.msgs_prof_cont_label_up));

        mContextSyncTaskButtonMoveToDown.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isUiEnabled()) {
                    for (int i = 0; i < mGp.syncTaskAdapter.getCount(); i++) {
                        SyncTaskItem item = mGp.syncTaskAdapter.getItem(i);
                        if (item.isChecked()) {
                            int c_pos = item.getSyncTaskPosition();
                            if (item.getSyncTaskPosition() < (mGp.syncTaskAdapter.getCount() - 1)) {
                                for (int j = 0; j < mGp.syncTaskAdapter.getCount(); j++) {
                                    if (mGp.syncTaskAdapter.getItem(j).getSyncTaskPosition() == (c_pos + 1)) {
                                        mGp.syncTaskAdapter.getItem(j).setSyncTaskPosition(c_pos);
                                    }
                                }
                                item.setSyncTaskPosition(c_pos + 1);
                                mGp.syncTaskAdapter.sort();
                                SyncTaskUtil.saveSyncTaskListToFile(mGp, mContext, mUtil, false, "", "", mGp.syncTaskList, false);
                                mGp.syncTaskAdapter.notifyDataSetChanged();

                                if (item.getSyncTaskPosition() == 0) {
                                    mContextSyncTaskViewMoveToUp.setVisibility(ImageButton.INVISIBLE);
                                    mContextSyncTaskViewMoveToDown.setVisibility(ImageButton.VISIBLE);
                                }
                                if (item.getSyncTaskPosition() == (mGp.syncTaskAdapter.getCount() - 1)) {
                                    mContextSyncTaskViewMoveToUp.setVisibility(ImageButton.VISIBLE);
                                    mContextSyncTaskViewMoveToDown.setVisibility(ImageButton.INVISIBLE);
                                }

                            }
                            break;
                        }
                    }
                }
            }
        });
        ContextButtonUtil.setButtonLabelListener(mContext, mContextSyncTaskButtonMoveToDown, mContext.getString(R.string.msgs_prof_cont_label_down));

        mContextSyncTaskButtonSelectAll.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isUiEnabled()) {
                    setContextButtonEnabled(mContextSyncTaskButtonSelectAll, false);
                    for (int i = 0; i < mGp.syncTaskAdapter.getCount(); i++) {
                        mGp.syncTaskAdapter.getItem(i).setChecked(true);
                    }
                    mGp.syncTaskAdapter.notifyDataSetChanged();
                    mGp.syncTaskAdapter.setShowCheckBox(true);
                    setSyncTaskContextButtonSelectMode();
                    setContextButtonEnabled(mContextSyncTaskButtonSelectAll, true);
                }
            }
        });
        ContextButtonUtil.setButtonLabelListener(mContext, mContextSyncTaskButtonSelectAll, mContext.getString(R.string.msgs_prof_cont_label_select_all));

        mContextSyncTaskButtonUnselectAll.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isUiEnabled()) {
                    setContextButtonEnabled(mContextSyncTaskButtonUnselectAll, false);
                    SyncTaskUtil.setAllSyncTaskToUnchecked(false, mGp.syncTaskAdapter);
                    //				for (int i=0;i<syncTaskListAdapter.getCount();i++) {
                    //					ProfileUtility.setProfileToChecked(false, syncTaskListAdapter, i);
                    //				}
                    mGp.syncTaskAdapter.notifyDataSetChanged();
                    setSyncTaskContextButtonSelectMode();
                    setContextButtonEnabled(mContextSyncTaskButtonUnselectAll, true);
                }
            }
        });
        ContextButtonUtil.setButtonLabelListener(mContext, mContextSyncTaskButtonUnselectAll, mContext.getString(R.string.msgs_prof_cont_label_unselect_all));

    }

    private void confirmActivate(AdapterSyncTask pa, final NotifyEvent p_ntfy) {
        NotifyEvent ntfy = new NotifyEvent(mContext);
        ntfy.setListener(new NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                mTaskUtil.setSyncTaskToAuto(mGp);
                SyncTaskUtil.setAllSyncTaskToUnchecked(true, mGp.syncTaskAdapter);
                p_ntfy.notifyToListener(true, null);
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
            }
        });
        String msg = "";
        String sep = "";
        for (int i = 0; i < pa.getCount(); i++) {
            if (pa.getItem(i).isChecked() && !pa.getItem(i).isSyncTaskAuto()) {
                msg += sep + pa.getItem(i).getSyncTaskName();
                sep = "\n";
            }
        }
//		msg+="\n";
        commonDlg.showCommonDialog(true, "W",
                mContext.getString(R.string.msgs_prof_cont_to_activate_profile),
                msg, ntfy);
    }

    private void confirmInactivate(AdapterSyncTask pa, final NotifyEvent p_ntfy) {
        NotifyEvent ntfy = new NotifyEvent(mContext);
        ntfy.setListener(new NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                mTaskUtil.setSyncTaskToManual();
                SyncTaskUtil.setAllSyncTaskToUnchecked(true, mGp.syncTaskAdapter);
                p_ntfy.notifyToListener(true, null);
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
            }
        });
        String msg = "";
        String sep = "";
        for (int i = 0; i < pa.getCount(); i++) {
            if (pa.getItem(i).isChecked() && pa.getItem(i).isSyncTaskAuto()) {
                msg += sep + pa.getItem(i).getSyncTaskName();
                sep = "\n";
            }
        }
//		msg+="\n";
        commonDlg.showCommonDialog(true, "W",
                mContext.getString(R.string.msgs_prof_cont_to_inactivate_profile),
                msg, ntfy);
    }

    private void setSyncTaskContextButtonSelectMode() {
        int sel_cnt = SyncTaskUtil.getSyncTaskSelectedItemCount(mGp.syncTaskAdapter);
        int tot_cnt = mGp.syncTaskAdapter.getCount();
        setActionBarSelectMode(sel_cnt, tot_cnt);

        boolean any_selected = SyncTaskUtil.isSyncTaskSelected(mGp.syncTaskAdapter);

        boolean act_prof_selected = false, inact_prof_selected = false;
        if (any_selected) {
            for (int i = 0; i < tot_cnt; i++) {
                if (mGp.syncTaskAdapter.getItem(i).isChecked()) {
                    if (mGp.syncTaskAdapter.getItem(i).isSyncTaskAuto()) act_prof_selected = true;
                    else inact_prof_selected = true;
                    if (act_prof_selected && inact_prof_selected) break;
                }
            }
        }

        if (inact_prof_selected) {
            if (any_selected) mContextSyncTaskViewActivete.setVisibility(ImageButton.VISIBLE);
            else mContextSyncTaskViewActivete.setVisibility(ImageButton.INVISIBLE);
        } else mContextSyncTaskViewActivete.setVisibility(ImageButton.INVISIBLE);

        if (act_prof_selected) {
            if (any_selected) mContextSyncTaskViewInactivete.setVisibility(ImageButton.VISIBLE);
            else mContextSyncTaskViewInactivete.setVisibility(ImageButton.INVISIBLE);
        } else mContextSyncTaskViewInactivete.setVisibility(ImageButton.INVISIBLE);

        mContextSyncTaskViewAddSync.setVisibility(ImageButton.INVISIBLE);

        if (sel_cnt == 1) mContextSyncTaskViewCopySyncTask.setVisibility(ImageButton.VISIBLE);
        else mContextSyncTaskViewCopySyncTask.setVisibility(ImageButton.INVISIBLE);

        if (any_selected) mContextSyncTaskViewDeleteSyncTask.setVisibility(ImageButton.VISIBLE);
        else mContextSyncTaskViewDeleteSyncTask.setVisibility(ImageButton.INVISIBLE);

        if (sel_cnt == 1) mContextSyncTaskViewRenameSyncTask.setVisibility(ImageButton.VISIBLE);
        else mContextSyncTaskViewRenameSyncTask.setVisibility(ImageButton.INVISIBLE);

        if (sel_cnt == 1) {
            for (int i = 0; i < tot_cnt; i++) {
                if (mGp.syncTaskAdapter.getItem(i).isChecked()) {
                    if (i == 0) mContextSyncTaskViewMoveToUp.setVisibility(ImageButton.INVISIBLE);
                    else mContextSyncTaskViewMoveToUp.setVisibility(ImageButton.VISIBLE);
                    if (i == (tot_cnt - 1))
                        mContextSyncTaskViewMoveToDown.setVisibility(ImageButton.INVISIBLE);
                    else mContextSyncTaskViewMoveToDown.setVisibility(ImageButton.VISIBLE);
                    break;
                }
            }
        } else {
            mContextSyncTaskViewMoveToUp.setVisibility(ImageButton.INVISIBLE);
            mContextSyncTaskViewMoveToDown.setVisibility(ImageButton.INVISIBLE);
        }

        if (tot_cnt != sel_cnt) mContextSyncTaskViewSelectAll.setVisibility(ImageButton.VISIBLE);
        else mContextSyncTaskViewSelectAll.setVisibility(ImageButton.INVISIBLE);

        if (any_selected) mContextSyncTaskViewUnselectAll.setVisibility(ImageButton.VISIBLE);
        else mContextSyncTaskViewUnselectAll.setVisibility(ImageButton.INVISIBLE);

        refreshOptionMenu();
    }

    private void setSyncTaskContextButtonHide() {
        mActionBar.setIcon(R.drawable.smbsync);
        mActionBar.setHomeButtonEnabled(false);
        mActionBar.setTitle(R.string.app_name);

        mGp.syncTaskAdapter.setAllItemChecked(false);
        mGp.syncTaskAdapter.setShowCheckBox(false);
        mGp.syncTaskAdapter.notifyDataSetChanged();

        mContextSyncTaskViewActivete.setVisibility(ImageButton.INVISIBLE);
        mContextSyncTaskViewInactivete.setVisibility(ImageButton.INVISIBLE);
        mContextSyncTaskViewAddSync.setVisibility(ImageButton.INVISIBLE);
        mContextSyncTaskViewCopySyncTask.setVisibility(ImageButton.INVISIBLE);
        mContextSyncTaskViewDeleteSyncTask.setVisibility(ImageButton.INVISIBLE);
        mContextSyncTaskViewRenameSyncTask.setVisibility(ImageButton.INVISIBLE);
        mContextSyncTaskViewMoveToUp.setVisibility(ImageButton.INVISIBLE);
        mContextSyncTaskViewMoveToDown.setVisibility(ImageButton.INVISIBLE);
        mContextSyncTaskViewSelectAll.setVisibility(ImageButton.INVISIBLE);
        mContextSyncTaskViewUnselectAll.setVisibility(ImageButton.INVISIBLE);

    }

    private void setActionBarSelectMode(int sel_cnt, int tot_cnt) {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        String sel_txt = "" + sel_cnt + "/" + tot_cnt;
        actionBar.setTitle(sel_txt);
    }

    private void setActionBarNormalMode() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.app_name);
        actionBar.setHomeButtonEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(false);
    }

    private void setSyncTaskContextButtonNormalMode() {
        setActionBarNormalMode();

        mGp.syncTaskAdapter.setAllItemChecked(false);
        mGp.syncTaskAdapter.setShowCheckBox(false);
        mGp.syncTaskAdapter.notifyDataSetChanged();

        mContextSyncTaskViewActivete.setVisibility(ImageButton.INVISIBLE);
        mContextSyncTaskViewInactivete.setVisibility(ImageButton.INVISIBLE);
        if (isUiEnabled()) mContextSyncTaskViewAddSync.setVisibility(ImageButton.VISIBLE);
        else mContextSyncTaskViewAddSync.setVisibility(ImageButton.INVISIBLE);
        mContextSyncTaskViewCopySyncTask.setVisibility(ImageButton.INVISIBLE);
        mContextSyncTaskViewDeleteSyncTask.setVisibility(ImageButton.INVISIBLE);
        mContextSyncTaskViewRenameSyncTask.setVisibility(ImageButton.INVISIBLE);
        mContextSyncTaskViewMoveToUp.setVisibility(ImageButton.INVISIBLE);
        mContextSyncTaskViewMoveToDown.setVisibility(ImageButton.INVISIBLE);
        if (isUiEnabled()) {
            if (!mGp.syncTaskAdapter.isEmptyAdapter())
                mContextSyncTaskViewSelectAll.setVisibility(ImageButton.VISIBLE);
            else mContextSyncTaskViewSelectAll.setVisibility(ImageButton.INVISIBLE);
        } else {
            mContextSyncTaskViewSelectAll.setVisibility(ImageButton.INVISIBLE);
        }
        mContextSyncTaskViewUnselectAll.setVisibility(ImageButton.INVISIBLE);

        refreshOptionMenu();
    }

    private void setMessageContextButtonListener() {
        final Toast toast_active = Toast.makeText(mContext, mContext.getString(R.string.msgs_log_activate_pinned),
                Toast.LENGTH_SHORT);
        final Toast toast_inactive = Toast.makeText(mContext, mContext.getString(R.string.msgs_log_inactivate_pinned),
                Toast.LENGTH_SHORT);
        mContextMessageButtonPinned.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setContextButtonEnabled(mContextMessageButtonPinned, false);
                mGp.freezeMessageViewScroll = !mGp.freezeMessageViewScroll;
                if (mGp.freezeMessageViewScroll) {
                    mContextMessageButtonPinned.setImageResource(R.drawable.context_button_pinned_active);
                    toast_active.show();
                    ContextButtonUtil.setButtonLabelListener(mContext, mContextMessageButtonPinned,
                            mContext.getString(R.string.msgs_msg_cont_label_pinned_active));
                } else {
                    mContextMessageButtonPinned.setImageResource(R.drawable.context_button_pinned_inactive);
                    mGp.msgListView.setSelection(mGp.msgListView.getCount() - 1);
                    toast_inactive.show();
                    ContextButtonUtil.setButtonLabelListener(mContext, mContextMessageButtonPinned,
                            mContext.getString(R.string.msgs_msg_cont_label_pinned_inactive));
                }
                setContextButtonEnabled(mContextMessageButtonPinned, true);
            }
        });
        ContextButtonUtil.setButtonLabelListener(mContext, mContextMessageButtonPinned, mContext.getString(R.string.msgs_msg_cont_label_pinned_inactive));

        mContextMessageButtonMoveTop.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setContextButtonEnabled(mContextMessageButtonMoveTop, false);
                mGp.msgListView.setSelection(0);
                setContextButtonEnabled(mContextMessageButtonMoveTop, true);
            }
        });
        ContextButtonUtil.setButtonLabelListener(mContext, mContextMessageButtonMoveTop, mContext.getString(R.string.msgs_msg_cont_label_move_top));

        mContextMessageButtonMoveBottom.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setContextButtonEnabled(mContextMessageButtonMoveBottom, false);
                mGp.msgListView.setSelection(mGp.msgListView.getCount() - 1);
                setContextButtonEnabled(mContextMessageButtonMoveBottom, true);
            }
        });
        ContextButtonUtil.setButtonLabelListener(mContext, mContextMessageButtonMoveBottom, mContext.getString(R.string.msgs_msg_cont_label_move_bottom));

        mContextMessageButtonClear.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                NotifyEvent ntfy = new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context c, Object[] o) {
                        mGp.msgListView.setSelection(0);
                        mGp.msgListAdapter.clear();
                        mUtil.addLogMsg("W", getString(R.string.msgs_log_msg_cleared));
                    }

                    @Override
                    public void negativeResponse(Context c, Object[] o) {
                    }
                });
                commonDlg.showCommonDialog(true, "W",
                        mContext.getString(R.string.msgs_log_confirm_clear_all_msg), "", ntfy);
            }
        });
        ContextButtonUtil.setButtonLabelListener(mContext, mContextMessageButtonClear, mContext.getString(R.string.msgs_msg_cont_label_clear));
    }

    private void setMessageContextButtonNormalMode() {
        mContextMessageViewPinned.setVisibility(LinearLayout.VISIBLE);
        if (mGp.freezeMessageViewScroll) {
            mContextMessageButtonPinned.setImageResource(R.drawable.context_button_pinned_active);
        } else {
            mContextMessageButtonPinned.setImageResource(R.drawable.context_button_pinned_inactive);
        }
        mContextMessageViewMoveTop.setVisibility(LinearLayout.VISIBLE);
        mContextMessageViewMoveBottom.setVisibility(LinearLayout.VISIBLE);
        mContextMessageViewClear.setVisibility(LinearLayout.VISIBLE);
    }

    private void editSyncTask(String prof_name,
                              boolean prof_act, int prof_num) {
        SyncTaskItem item = mGp.syncTaskAdapter.getItem(prof_num);
        NotifyEvent ntfy = new NotifyEvent(mContext);
        ntfy.setListener(new NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
//				checkSafExternalSdcardTreeUri(null);
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
//				checkSafExternalSdcardTreeUri(null);
            }
        });
        SyncTaskEditor pmp = SyncTaskEditor.newInstance();
        pmp.showDialog(getSupportFragmentManager(), pmp, "EDIT", item,
                mTaskUtil, mUtil, commonDlg, mGp, ntfy);
    }

    private void syncSelectedSyncTask() {
        final ArrayList<SyncTaskItem> t_list = new ArrayList<SyncTaskItem>();
        SyncTaskItem item;
        String sync_list_tmp = "";
        String sep = "";
        boolean test_sync_task_found = false;
        for (int i = 0; i < mGp.syncTaskAdapter.getCount(); i++) {
            item = mGp.syncTaskAdapter.getItem(i);
            if (item.isChecked()) {
                t_list.add(item);
                sync_list_tmp += sep + item.getSyncTaskName();
                sep = ",";
                if (item.isSyncTestMode()) test_sync_task_found = true;
            }
        }
        final String sync_list = sync_list_tmp;

        NotifyEvent ntfy_test_mode = new NotifyEvent(mContext);
        ntfy_test_mode.setListener(new NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                if (t_list.isEmpty()) {
                    mUtil.addLogMsg("E", mContext.getString(R.string.msgs_main_sync_select_prof_no_active_profile));
                    commonDlg.showCommonDialog(false, "E", mContext.getString(R.string.msgs_main_sync_select_prof_no_active_profile), "", null);
                } else {
                    mUtil.addLogMsg("I", mContext.getString(R.string.msgs_main_sync_selected_profiles));
                    mUtil.addLogMsg("I", mContext.getString(R.string.msgs_main_sync_prof_name_list) +
                            " " + sync_list);
                    Toast.makeText(mContext,
                            mContext.getString(R.string.msgs_main_sync_selected_profiles),
                            Toast.LENGTH_SHORT)
                            .show();
                    startSyncTask(t_list);
                }
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
            }

        });
        if (test_sync_task_found) {
            commonDlg.showCommonDialog(true, "W",
                    mContext.getString(R.string.msgs_main_sync_test_mode_warnning), "", ntfy_test_mode);
        } else {
            ntfy_test_mode.notifyToListener(true, null);
        }

    }

    private void syncAutoSyncTask() {
        final ArrayList<SyncTaskItem> t_list = new ArrayList<SyncTaskItem>();
        String sync_list_tmp = "", sep = "";
        for (int i = 0; i < mGp.syncTaskAdapter.getCount(); i++) {
            SyncTaskItem item = mGp.syncTaskAdapter.getItem(i);
            if (item.isSyncTaskAuto() && !item.isSyncTestMode()) {
                t_list.add(item);
                sync_list_tmp += sep + item.getSyncTaskName();
                sep = ",";
            }
        }

        if (t_list.isEmpty()) {
            mUtil.addLogMsg("E", mContext.getString(R.string.msgs_active_sync_prof_not_found));
            commonDlg.showCommonDialog(false, "E", mContext.getString(R.string.msgs_active_sync_prof_not_found), "", null);
        } else {
            final String sync_list = sync_list_tmp;
            mUtil.addLogMsg("I", mContext.getString(R.string.msgs_main_sync_all_active_profiles));
            mUtil.addLogMsg("I", mContext.getString(R.string.msgs_main_sync_prof_name_list) + sync_list);
//			tabHost.setCurrentTabByTag(TAB_TAG_MSG);
            Toast.makeText(mContext,
                    mContext.getString(R.string.msgs_main_sync_all_active_profiles),
                    Toast.LENGTH_SHORT)
                    .show();
            startSyncTask(t_list);
        }

    }

    private void setUiEnabled() {
        mUtil.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName() + " entered");
        enableMainUi = true;

        if (!mGp.syncTaskAdapter.isShowCheckBox()) setSyncTaskContextButtonNormalMode();
        else setSyncTaskContextButtonSelectMode();

        if (!mGp.syncHistoryAdapter.isShowCheckBox()) setHistoryContextButtonNormalMode();
        else setHistoryContextButtonSelectMode();

        refreshOptionMenu();
    }

    private void setUiDisabled() {
        mUtil.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName() + " entered");
        enableMainUi = false;

        if (!mGp.syncTaskAdapter.isShowCheckBox()) setSyncTaskContextButtonNormalMode();
        else setSyncTaskContextButtonSelectMode();

        if (!mGp.syncHistoryAdapter.isShowCheckBox()) setHistoryContextButtonNormalMode();
        else setHistoryContextButtonSelectMode();

        refreshOptionMenu();
    }

    private boolean isUiEnabled() {
        return enableMainUi;
    }

    @SuppressLint("NewApi")
    final private void refreshOptionMenu() {
        mUtil.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName() + " entered");
//		if (Build.VERSION.SDK_INT>=11)
//			this.invalidateOptionsMenu();
        supportInvalidateOptionsMenu();
    }

    private void startSyncTask(ArrayList<SyncTaskItem> alp) {
        String[] task_name = new String[alp.size()];
        for (int i = 0; i < alp.size(); i++) task_name[i] = alp.get(i).getSyncTaskName();
        try {
            mSvcClient.aidlStartSpecificSyncTask(task_name);
//			mMainTabHost.setCurrentTab(2);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void syncThreadStarted() {
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
        setUiDisabled();
        mGp.progressSpinView.setVisibility(LinearLayout.VISIBLE);
        mGp.progressSpinView.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);
        mGp.progressSpinView.bringToFront();
        mGp.progressSpinSyncprof.setVisibility(TextView.VISIBLE);
        mGp.progressSpinCancel.setText(getString(R.string.msgs_progress_spin_dlg_sync_cancel));
        mGp.progressSpinCancel.setEnabled(true);
        // CANCELボタンの指定
        mGp.progressSpinCancelListener = new View.OnClickListener() {
            public void onClick(View v) {
                NotifyEvent ntfy = new NotifyEvent(mContext);
                ntfy.setListener(new NotifyEventListener() {
                    @Override
                    public void positiveResponse(Context c, Object[] o) {
                        try {
                            mSvcClient.aidlCancelSyncTask();
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        mGp.progressSpinCancel.setText(getString(R.string.msgs_progress_dlg_canceling));
                        mGp.progressSpinCancel.setEnabled(false);
                    }

                    @Override
                    public void negativeResponse(Context c, Object[] o) {
                    }
                });
                commonDlg.showCommonDialog(true, "W",
                        getString(R.string.msgs_main_sync_cancel_confirm),
                        "", ntfy);
            }
        };
        mGp.progressSpinCancel.setOnClickListener(mGp.progressSpinCancelListener);

        mGp.msgListView.setFastScrollEnabled(false);

        ScheduleUtil.setSchedulerInfo(mGp);

        LogUtil.flushLog(mContext, mGp);
    }

    private void syncThreadEnded() {
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
        LogUtil.flushLog(mContext, mGp);

        mGp.progressBarCancelListener = null;
        mGp.progressBarImmedListener = null;
        mGp.progressSpinCancelListener = null;
        mGp.progressBarCancel.setOnClickListener(null);
        mGp.progressSpinCancel.setOnClickListener(null);
        mGp.progressBarImmed.setOnClickListener(null);

        mGp.progressSpinView.setVisibility(LinearLayout.GONE);

        mGp.syncHistoryAdapter.notifyDataSetChanged();

        setUiEnabled();
    }

    private ISvcCallback mSvcCallbackStub = new ISvcCallback.Stub() {
        @Override
        public void cbThreadStarted() throws RemoteException {
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    syncThreadStarted();
                }
            });
        }

        @Override
        public void cbThreadEnded() throws RemoteException {
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    syncThreadEnded();
                }
            });
        }

        @Override
        public void cbShowConfirmDialog(final String fp, final String method) throws RemoteException {
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    showConfirmDialog(fp, method);
                }
            });
        }

        @Override
        public void cbHideConfirmDialog() throws RemoteException {
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    hideConfirmDialog();
                }
            });
        }

        @Override
        public void cbWifiStatusChanged(String status, String ssid) throws RemoteException {
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    refreshOptionMenu();
                    if (mGp.syncTaskAdapter.isShowCheckBox()) setSyncTaskContextButtonSelectMode();
                    else setSyncTaskContextButtonNormalMode();
                }
            });
        }

        @Override
        public void cbMediaStatusChanged() throws RemoteException {
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    refreshOptionMenu();
//					mGp.syncTaskAdapter.notifyDataSetChanged();
                    mGp.syncTaskAdapter.notifyDataSetChanged();
                }
            });
        }

    };

    private ISvcClient mSvcClient = null;

    private void openService(final NotifyEvent p_ntfy) {
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
        mSvcConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName arg0, IBinder service) {
                mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
                mSvcClient = ISvcClient.Stub.asInterface(service);
                p_ntfy.notifyToListener(true, null);
            }

            public void onServiceDisconnected(ComponentName name) {
                mSvcConnection = null;
                mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
//    	    	mSvcClient=null;
//    	    	synchronized(tcService) {
//        	    	tcService.notify();
//    	    	}
            }
        };

        Intent intmsg = new Intent(mContext, SyncService.class);
        intmsg.setAction("Bind");
        bindService(intmsg, mSvcConnection, BIND_AUTO_CREATE);
    }

    private void closeService() {

        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered, conn=" + mSvcConnection);

        if (mSvcConnection != null) {
//    		try {
//				if (mSvcClient!=null) mSvcClient.aidlStopService();
//			} catch (RemoteException e) {
//				e.printStackTrace();
//			}
            mSvcClient = null;
            try {
                unbindService(mSvcConnection);
            } catch (Exception e) {
                e.printStackTrace();
            }
            mSvcConnection = null;
//	    	Log.v("","close service");
        }
//        Intent intent = new Intent(this, SyncService.class);
//        stopService(intent);
    }

    final private void setCallbackListener() {
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
        try {
            mSvcClient.setCallBack(mSvcCallbackStub);
        } catch (RemoteException e) {
            e.printStackTrace();
            mUtil.addDebugMsg(1, "E", "setCallbackListener error :" + e.toString());
        }
    }

    final private void unsetCallbackListener() {
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
        if (mSvcClient != null) {
            try {
                mSvcClient.removeCallBack(mSvcCallbackStub);
            } catch (RemoteException e) {
                e.printStackTrace();
                mUtil.addDebugMsg(1, "E", "unsetCallbackListener error :" + e.toString());
            }
        }
    }

    private void reshowDialogWindow() {
        if (mGp.dialogWindowShowed) {
            syncThreadStarted();
            mGp.progressSpinSyncprof.setText(mGp.progressSpinSyncprofText);
            mGp.progressSpinMsg.setText(mGp.progressSpinMsgText);
            if (mGp.confirmDialogShowed)
                showConfirmDialog(mGp.confirmDialogFilePath, mGp.confirmDialogMethod);
        }
    }

    private void hideConfirmDialog() {
        mGp.confirmView.setVisibility(LinearLayout.GONE);
    }

    private void showConfirmDialog(String fp, String method) {
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");
        mGp.confirmDialogShowed = true;
        mGp.confirmDialogFilePath = fp;
        mGp.confirmDialogMethod = method;
        final NotifyEvent ntfy = new NotifyEvent(mContext);
        ntfy.setListener(new NotifyEventListener() {
            @Override
            public void positiveResponse(Context c, Object[] o) {
                mGp.confirmDialogShowed = false;
                try {
                    mSvcClient.aidlConfirmReply((Integer) o[0]);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                mGp.confirmYesListener = null;
                mGp.confirmYesAllListener = null;
                mGp.confirmNoListener = null;
                mGp.confirmNoAllListener = null;
                mGp.confirmCancelListener = null;
                mGp.confirmCancel.setOnClickListener(null);
                mGp.confirmYes.setOnClickListener(null);
                mGp.confirmYesAll.setOnClickListener(null);
                mGp.confirmNo.setOnClickListener(null);
                mGp.confirmNoAll.setOnClickListener(null);
            }

            @Override
            public void negativeResponse(Context c, Object[] o) {
                mGp.confirmDialogShowed = false;
                try {
                    mSvcClient.aidlConfirmReply((Integer) o[0]);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                mGp.confirmYesListener = null;
                mGp.confirmYesAllListener = null;
                mGp.confirmNoListener = null;
                mGp.confirmNoAllListener = null;
                mGp.confirmCancelListener = null;
                mGp.confirmCancel.setOnClickListener(null);
                mGp.confirmYes.setOnClickListener(null);
                mGp.confirmYesAll.setOnClickListener(null);
                mGp.confirmNo.setOnClickListener(null);
                mGp.confirmNoAll.setOnClickListener(null);
            }
        });

        mGp.confirmView.setVisibility(LinearLayout.VISIBLE);
        mGp.confirmView.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);
        mGp.confirmView.bringToFront();
        String msg_text = "";
        if (method.equals(SMBSYNC2_CONFIRM_REQUEST_COPY)) {
            msg_text = String.format(getString(R.string.msgs_mirror_confirm_copy_confirm), fp);
        } else if (method.equals(SMBSYNC2_CONFIRM_REQUEST_DELETE_FILE)) {
            msg_text = String.format(getString(R.string.msgs_mirror_confirm_delete_file_confirm), fp);
        } else if (method.equals(SMBSYNC2_CONFIRM_REQUEST_DELETE_DIR)) {
            msg_text = String.format(getString(R.string.msgs_mirror_confirm_delete_dir_confirm), fp);
        } else if (method.equals(SMBSYNC2_CONFIRM_REQUEST_MOVE)) {
            msg_text = String.format(getString(R.string.msgs_mirror_confirm_move_confirm), fp);
        }
        mGp.confirmMsg.setText(msg_text);

        // Yesボタンの指定
        mGp.confirmYesListener = new View.OnClickListener() {
            public void onClick(View v) {
                mGp.confirmView.setVisibility(LinearLayout.GONE);
                ntfy.notifyToListener(true, new Object[]{SMBSYNC2_CONFIRM_RESP_YES});
            }
        };
        mGp.confirmYes.setOnClickListener(mGp.confirmYesListener);
        // YesAllボタンの指定
        mGp.confirmYesAllListener = new View.OnClickListener() {
            public void onClick(View v) {
                mGp.confirmView.setVisibility(LinearLayout.GONE);
                ntfy.notifyToListener(true, new Object[]{SMBSYNC2_CONFIRM_RESP_YESALL});
            }
        };
        mGp.confirmYesAll.setOnClickListener(mGp.confirmYesAllListener);
        // Noボタンの指定
        mGp.confirmNoListener = new View.OnClickListener() {
            public void onClick(View v) {
                mGp.confirmView.setVisibility(LinearLayout.GONE);
                ntfy.notifyToListener(false, new Object[]{SMBSYNC2_CONFIRM_RESP_NO});
            }
        };
        mGp.confirmNo.setOnClickListener(mGp.confirmNoListener);
        // NoAllボタンの指定
        mGp.confirmNoAllListener = new View.OnClickListener() {
            public void onClick(View v) {
                mGp.confirmView.setVisibility(LinearLayout.GONE);
                ntfy.notifyToListener(false, new Object[]{SMBSYNC2_CONFIRM_RESP_NOALL});
            }
        };
        mGp.confirmNoAll.setOnClickListener(mGp.confirmNoAllListener);
        // Task cancelボタンの指定
        mGp.confirmCancelListener = new View.OnClickListener() {
            public void onClick(View v) {
                mGp.confirmView.setVisibility(LinearLayout.GONE);
                ntfy.notifyToListener(false, new Object[]{SMBSYNC2_CONFIRM_RESP_CANCEL});
            }
        };
        mGp.confirmCancel.setOnClickListener(mGp.confirmCancelListener);
    }

    final private boolean checkJcifsOptionChanged() {
        boolean changed = false;

        String  prevSmbLmCompatibility = mGp.settingsSmbLmCompatibility,
                prevSmbUseExtendedSecurity = mGp.settingsSmbUseExtendedSecurity;
        String p_response_timeout=mGp.settingsSmbClientResponseTimeout;
        String p_disable_plain_text_passwords=mGp.settingsSmbDisablePlainTextPasswords;

        mGp.initJcifsOption();

        if (!mGp.settingsSmbLmCompatibility.equals(prevSmbLmCompatibility)) changed = true;
        else if (!mGp.settingsSmbUseExtendedSecurity.equals(prevSmbUseExtendedSecurity)) changed = true;
        else if (!mGp.settingsSmbClientResponseTimeout.equals(p_response_timeout)) changed = true;
        else if (!mGp.settingsSmbDisablePlainTextPasswords.equals(p_disable_plain_text_passwords)) changed = true;

        if (changed) {
            listSettingsOption();
            NotifyEvent ntfy=new NotifyEvent(mContext);
            ntfy.setListener(new NotifyEventListener() {
                @Override
                public void positiveResponse(Context context, Object[] objects) {
                    mUtil.flushLog();
//                    Intent in_act = new Intent(context, ActivityMain.class);
//                    int pi_id = R.string.app_name;
//                    PendingIntent pi = PendingIntent.getActivity(context, pi_id, in_act, PendingIntent.FLAG_CANCEL_CURRENT);
//                    AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
//                    am.set(AlarmManager.RTC, System.currentTimeMillis() + 100, pi);
//                    Runtime.getRuntime().exit(0);
                    mGp.settingExitClean=true;
                    finish();
                }
                @Override
                public void negativeResponse(Context context, Object[] objects) {}
            });
            commonDlg.showCommonDialog(true, "W",
                    mContext.getString(R.string.msgs_smbsync_main_settings_jcifs_changed_restart), "", ntfy);
        }

        return changed;
    }

    private void saveTaskData() {
        mUtil.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName() + " entered");

        if (!isTaskTermination) {
            if (!isTaskDataExisted() || mGp.msgListAdapter.resetDataChanged()) {
//				ActivityDataHolder data = new ActivityDataHolder();
//				data.ml=mGp.msgListAdapter.getMessageList();
//				data.pl=mGp.syncTaskAdapter.getArrayList();
//				try {
//				    FileOutputStream fos = openFileOutput(SMBSYNC2_SERIALIZABLE_FILE_NAME, MODE_PRIVATE);
//				    BufferedOutputStream bos=new BufferedOutputStream(fos,4096*256);
//				    ObjectOutputStream oos = new ObjectOutputStream(bos);
//				    oos.writeObject(data);
//				    oos.flush();
//				    oos.close();
//				    mUtil.addDebugMsg(1,"I", "Task data was saved.");
//				} catch (Exception e) {
//					e.printStackTrace();
//				    mUtil.addLogMsg("E", "saveTaskData error, "+e.toString());
//				    mUtil.addLogMsg("E","StackTrace element, "+printStackTraceElement(e.getStackTrace()));
//				}
            }
        }
    }

    private String printStackTraceElement(StackTraceElement[] ste) {
        String st_msg = "";
        for (int i = 0; i < ste.length; i++) {
            st_msg += "\n at " + ste[i].getClassName() + "." +
                    ste[i].getMethodName() + "(" + ste[i].getFileName() +
                    ":" + ste[i].getLineNumber() + ")";
        }
        return st_msg;
    }

    private void restoreTaskData() {
        mUtil.addDebugMsg(2, "I", CommonUtilities.getExecutedMethodName() + " entered");
        File lf = new File(mGp.applicationRootDirectory + "/" + SMBSYNC2_SERIALIZABLE_FILE_NAME);
        if (lf.exists()) {
//            try {
////			    FileInputStream fis = new FileInputStream(lf);
////			    BufferedInputStream bis=new BufferedInputStream(fis,4096*256);
////			    ObjectInputStream ois = new ObjectInputStream(bis);
////			    ActivityDataHolder data = (ActivityDataHolder) ois.readObject();
////			    ois.close();
////			    lf.delete();
////
////			    ArrayList<SyncMessageItem> o_ml=new ArrayList<SyncMessageItem>();
////				for (int i=0;i<mGp.msgListAdapter.getCount();i++)
////					o_ml.add(mGp.msgListAdapter.getItem(i));
////
////				mGp.msgListAdapter.clear();
////
////				mGp.msgListAdapter.setMessageList(data.ml);
////
////				for (int i=0;i<o_ml.size();i++) mGp.msgListAdapter.add(o_ml.get(i));
////
////				mGp.msgListAdapter.notifyDataSetChanged();
////				mGp.msgListAdapter.resetDataChanged();
////
////				mGp.syncTaskAdapter.clear();
////				mGp.syncTaskAdapter.setArrayList(data.pl);
//                mUtil.addDebugMsg(1, "I", "Task data was restored.");
//            } catch (Exception e) {
//                e.printStackTrace();
//                mUtil.addLogMsg("E", "restoreTaskData error, " + e.toString());
//                mUtil.addLogMsg("E", "StackTrace element, " + printStackTraceElement(e.getStackTrace()));
//            }
        }
    }

    private boolean isTaskDataExisted() {
        File lf = new File(getFilesDir() + "/" + SMBSYNC2_SERIALIZABLE_FILE_NAME);
        return lf.exists();
    }

    private void deleteTaskData() {
        File lf =
                new File(mGp.applicationRootDirectory + "/" + SMBSYNC2_SERIALIZABLE_FILE_NAME);
        if (lf.exists()) {
            lf.delete();
            mUtil.addDebugMsg(1, "I", "RestartData was delete.");
        }
    }

}

//class ActivityDataHolder implements Serializable {
//    private static final long serialVersionUID = 1L;
//}
