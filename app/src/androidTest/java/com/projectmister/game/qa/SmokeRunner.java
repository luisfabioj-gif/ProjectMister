package com.projectmister.game.qa;
import android.app.*;
import android.content.*;
import android.graphics.Bitmap;
import android.os.*;
import android.view.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;

/** Real Activity navigation and match regressions; no production QA entry points. */
public final class SmokeRunner extends Instrumentation {
    private Activity activity;
    private Bundle args;
    private final StringBuilder report=new StringBuilder();
    private File output;
    public void onCreate(Bundle args){super.onCreate(args);this.args=args;start();}
    private Object call(String name,Class<?>[] types,Object... values) throws Exception {
        Method m=activity.getClass().getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(activity,values);
    }
    private Object get(String name)throws Exception {Field f=activity.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(activity);}
    private void set(String name,Object value)throws Exception {Field f=activity.getClass().getDeclaredField(name);f.setAccessible(true);f.set(activity,value);}
    interface Work {void run()throws Exception;}
    private void ui(Work w)throws Exception {
        final Throwable[] error={null};runOnMainSync(()->{try{w.run();}catch(Throwable e){error[0]=e;}});
        if(error[0]!=null)throw new Exception(error[0]);
        waitForIdleSync();
    }
    private void check(boolean ok,String label){if(!ok)throw new AssertionError(label);report.append("PASS ").append(label).append('\n');}
    private void page(String name,String method,Class<?>[] types,Object... values)throws Exception {
        ui(()->call(method,types,values));SystemClock.sleep(500);capture(name);
        check(!activity.isFinishing(),name+" navigates");
    }
    private void awaitBoolean(String field)throws Exception {
        long end=SystemClock.uptimeMillis()+75000;
        final boolean[] value={false};
        while(SystemClock.uptimeMillis()<end) {
            ui(()->value[0]=(Boolean)get(field));if(value[0])return;SystemClock.sleep(150);
        }
        throw new AssertionError("Timed out waiting for "+field);
    }
    private void awaitRound(int target)throws Exception {
        long end=SystemClock.uptimeMillis()+75000;
        final int[] round={0};
        while(SystemClock.uptimeMillis()<end) {
            ui(()->round[0]=(Integer)get("matchday"));if(round[0]>=target)return;SystemClock.sleep(150);
        }
        throw new AssertionError("Timed out waiting for full-time");
    }
    private void capture(String name)throws Exception {
        // Wait through asynchronous layout, portrait decoding and orientation changes.
        SystemClock.sleep(700);waitForIdleSync();
        Bitmap b=getUiAutomation().takeScreenshot();
        if(b==null)throw new AssertionError("Screenshot missing: "+name);
        try(FileOutputStream f=new FileOutputStream(new File(output,name+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,f);} b.recycle();
    }
    private void verifyTacticsMarkers()throws Exception {
        ui(()->{
            ViewGroup pitch=activity.getWindow().getDecorView().findViewWithTag("live-tactics-pitch");
            check(pitch!=null&&pitch.getChildCount()==12,"tactics pitch renders all eleven players");
            check(pitch.getChildAt(0).getWidth()==pitch.getWidth()&&pitch.getChildAt(0).getHeight()==pitch.getHeight(),"tactics pitch background fills measured area");
            for(int i=1;i<pitch.getChildCount();i++) {
                View a=pitch.getChildAt(i);
                check(a.getWidth()>0&&a.getHeight()>0&&a.isShown(),"tactics marker has visible layout "+i);
                android.graphics.RectF ar=new android.graphics.RectF(a.getX(),a.getY(),a.getX()+a.getWidth(),a.getY()+a.getHeight());
                check(ar.left>=0&&ar.top>=0&&ar.right<=pitch.getWidth()&&ar.bottom<=pitch.getHeight(),"tactics marker in bounds "+i);
                for(int j=i+1;j<pitch.getChildCount();j++) {
                    View b=pitch.getChildAt(j);
                    android.graphics.RectF br=new android.graphics.RectF(b.getX(),b.getY(),b.getX()+b.getWidth(),b.getY()+b.getHeight());
                    check(!android.graphics.RectF.intersects(ar,br),"tactics markers separate "+i+"/"+j);
                }
            }
        });
    }
    private android.view.accessibility.AccessibilityNodeInfo node(String text)throws Exception {
        long until=SystemClock.uptimeMillis()+5000;
        do {
            android.view.accessibility.AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();
            if(root!=null)for(android.view.accessibility.AccessibilityNodeInfo n:root.findAccessibilityNodeInfosByText(text))if(n.isVisibleToUser())return n;
            SystemClock.sleep(100);
        }while(SystemClock.uptimeMillis()<until);
        throw new AssertionError("Visible control missing: "+text);
    }
    private void tap(String text)throws Exception {
        android.view.accessibility.AccessibilityNodeInfo n=node(text);
        while(n!=null&&!n.isClickable())n=n.getParent();
        check(n!=null&&n.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK),"tap "+text);
        SystemClock.sleep(350);waitForIdleSync();
    }
    private int playerInt(Object p,String field)throws Exception {Field f=p.getClass().getDeclaredField(field);f.setAccessible(true);return f.getInt(p);}
    private void verifyOfferControls()throws Exception {
        final Object[] target={null};final int[] fee={0},before={0};
        ui(()->{target[0]=call("findPlayer",new Class[]{int.class},60);set("currentTransferBudget",500);before[0]=500;
            int value=playerInt(target[0],"valueMillions");fee[0]=Math.max(value+2,(int)Math.round(value*1.5));
            call("showPlayerProfile",new Class[]{int.class,int.class},60,3);
            call("showTransferOfferDialog",new Class[]{target[0].getClass(),boolean.class},target[0],false);
        });
        check(node("opening offer")!=null&&node("market value")!=null&&node("strong offer")!=null&&node("premium offer")!=null,"all transfer offers visible alongside budget");
        capture("25-transfer-offers");
        ui(()->((Random)get("random")).setSeed(4096));tap("premium offer");
        check(node("Rotation")!=null&&node("Star Player")!=null,"contract packages visible alongside fee");capture("26-contract-packages");
        ui(()->((Random)get("random")).setSeed(4096));tap("Star Player");
        check(node("Signing complete")!=null,"offer and contract actions complete signing");
        check(playerInt(target[0],"team")==0,"signed player changes club");
        check((Integer)get("currentTransferBudget")==before[0]-fee[0],"transfer charged exactly once");tap("View player");
        ui(()->call("loadSave",new Class[]{int.class},0));
        ui(()->target[0]=call("findPlayer",new Class[]{int.class},60));
        check(playerInt(target[0],"team")==0,"signing survives save reload");
    }
    @SuppressWarnings("unchecked") public void onStart(){
        Bundle result=new Bundle();
        try {
            Intent launch=new Intent().setClassName("com.projectmister.game","com.projectmister.game.MainActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity=startActivitySync(launch);waitForIdleSync();
            output=new File(getTargetContext().getExternalFilesDir(null),"qa");output.mkdirs();
            capture("00-startup");
            String mode=args==null?"full":args.getString("mode","full");
            if(mode.equals("seed")) {
                ui(()->{
                    set("selectedSlot",0);set("selectedClub",0);call("initialiseNewManagerDefaults",new Class[0]);
                    set("managerFirstName","Upgrade");set("managerLastName","Test");
                    call("resetCareerState",new Class[0]);call("generatePlayers",new Class[0]);
                    call("initialiseTacticsForClub",new Class[0]);call("initialiseClassicCareerSystems",new Class[0]);
                    call("saveCurrentGame",new Class[0]);
                });
                // Instrumentation.finish ends the process without a normal Activity stop.
                // Flush pending apply() writes before installing the candidate over it.
                SharedPreferences saved=getTargetContext().getSharedPreferences("project_mister",Context.MODE_PRIVATE);
                check(saved.edit().commit(),"baseline save flushed to disk");
                check("Upgrade".equals(saved.getString("save_0_manager_first","")),"baseline persisted manager");
                ui(()->{set("managerFirstName","");call("loadSave",new Class[]{int.class},0);});
                check("Upgrade".equals(get("managerFirstName")),"baseline save reloads");
                check(true,"baseline career seeded");
            } else if(mode.equals("compact")) {
                ui(()->{call("loadSave",new Class[]{int.class},0);call("startLiveMatchday",new Class[0]);set("livePaused",true);});
                capture("23-compact-match");
                for(String formation:new String[]{"4-3-3","4-2-3-1","4-4-2","3-5-2","5-3-2"}) {
                    ui(()->{set("liveFormation",formation);call("showLiveTacticsScreen",new Class[0]);});
                    capture("24-compact-"+formation);verifyTacticsMarkers();
                }
                check(true,"compact landscape formations usable");
            } else {
                ui(()->call("loadSave",new Class[]{int.class},0));
                check("Upgrade".equals(get("managerFirstName")),"old save manager preserved");
                check((Integer)get("selectedClub")==0,"old save club preserved");
                page("01-dashboard","showDashboard",new Class[0]);
                page("02-squad","showTeamPlayers",new Class[]{int.class},0);
                page("03-player-profile","showPlayerProfile",new Class[]{int.class,int.class},0,0);
                page("04-staff","showStaffHub",new Class[0]);
                page("05-staff-profile","showStaffProfile",new Class[]{String.class},"Assistant Manager");
                page("06-manager","showManagerProfile",new Class[0]);
                page("07-tactics","showTactics",new Class[0]);
                page("08-finance","showFinances",new Class[0]);
                page("09-stadium","showStadiumCentre",new Class[0]);
                page("10-scouting","showScoutCentre",new Class[0]);
                page("11-transfers","showTransferHub",new Class[0]);
                page("12-training","showTraining",new Class[0]);
                page("13-fixtures","showCompetitionCalendar",new Class[0]);
                page("14-board","showClubOffice",new Class[0]);
                page("15-inbox","showInbox",new Class[0]);
                verifyOfferControls();
                ui(()->call("startLiveMatchday",new Class[0]));SystemClock.sleep(1200);
                ui(()->set("livePaused",true));capture("16-match");
                ui(()->call("showLiveSpeedDialog",new Class[0]));
                check(node("Slow")!=null&&node("Medium")!=null&&node("Fast")!=null,"three named speed choices visible");capture("27-speed-menu");
                tap("Slow");check((Integer)get("liveSpeed")==0,"slow selected through menu");
                ui(()->call("showLiveSpeedDialog",new Class[0]));tap("Medium");check((Integer)get("liveSpeed")==1,"medium selected through menu");
                check((Boolean)get("liveMatchActive"),"live match running");
                ui(()->{Object a=get("audio");check(a!=null,"audio manager available");Field f=a.getClass().getDeclaredField("prepared");f.setAccessible(true);boolean[] ready=(boolean[])f.get(a);check(ready[0]&&ready[1],"both recorded crowd streams prepared");});
                ui(()->call("showSoundSettings",new Class[0]));tap("Stadium crowd");
                check(!getTargetContext().getSharedPreferences("project_mister",Context.MODE_PRIVATE).getBoolean("audio_crowd",true),"crowd checkbox persists mute");
                tap("Stadium crowd");tap("Done");
                page("17-live-tactics","showLiveTacticsScreen",new Class[0]);
                verifyTacticsMarkers();
                for(String formation:new String[]{"4-2-3-1","4-4-2","3-5-2","5-3-2"}) {
                    ui(()->{set("liveFormation",formation);call("showLiveTacticsScreen",new Class[0]);});
                    capture("17-formation-"+formation);verifyTacticsMarkers();
                }
                ui(()->{set("liveFormation","4-3-3");call("showLiveTacticsScreen",new Class[0]);});
                List<Integer> xi=(List<Integer>)get("liveXIIds"),bench=(List<Integer>)get("liveBenchIds");
                int off=xi.get(5),on=bench.get(0);
                ui(()->call("performLiveSubstitution",new Class[]{int.class,int.class},off,on));
                check((Integer)get("liveSubsUsed")==1,"substitution counted");
                ui(()->call("returnFromLiveTactics",new Class[0]));
                check((Boolean)get("livePaused"),"returning from tactics preserves pause");
                ui(()->call("showLiveTacticsScreen",new Class[0]));
                ui(()->call("performLiveSubstitution",new Class[]{int.class,int.class},on,off));
                check((Integer)get("liveSubsUsed")==0,"pending reversal restores allowance");
                ui(()->call("performLiveSubstitution",new Class[]{int.class,int.class},off,on));
                ui(()->{call("returnFromLiveTactics",new Class[0]);set("livePaused",false);});SystemClock.sleep(300);
                ui(()->call("showLiveTacticsScreen",new Class[0]));
                ui(()->call("performLiveSubstitution",new Class[]{int.class,int.class},on,off));
                check(!((List<Integer>)get("liveXIIds")).contains(off),"committed substitute cannot return");
                ui(()->{call("returnFromLiveTactics",new Class[0]);set("liveMinute",44);set("liveMinuteFloat",44.98f);set("livePaused",false);});
                awaitBoolean("liveHalfTimeTacticsActive");waitForIdleSync();capture("18-halftime");verifyTacticsMarkers();
                check((Boolean)get("liveHalfTimeTacticsActive"),"half-time automatically opens tactics");
                ui(()->call("returnFromLiveTactics",new Class[0]));SystemClock.sleep(200);
                check((Boolean)get("liveHalfTimeBreakTaken"),"second half resumes");
                ui(()->{set("liveMinute",89);set("liveMinuteFloat",89.98f);});awaitRound(1);waitForIdleSync();
                capture("19-fulltime");check((Integer)get("matchday")==1,"full-time commits one round");
                ui(()->call("loadSave",new Class[]{int.class},0));
                check((Integer)get("matchday")==1,"completed match survives reload");
                capture("20-reloaded");
                // Exercise complete event chains and AI reviews, not only time-boundary jumps.
                ui(()->{call("startLiveMatchday",new Class[0]);call("setLiveSpeed",new Class[]{int.class},2);});
                awaitBoolean("liveHalfTimeTacticsActive");capture("21-natural-halftime");
                ui(()->call("returnFromLiveTactics",new Class[0]));
                awaitRound(2);capture("22-natural-fulltime");
                int shots=(Integer)get("liveHomeShots")+(Integer)get("liveAwayShots");
                int target=(Integer)get("liveHomeOnTarget")+(Integer)get("liveAwayOnTarget");
                int goals=(Integer)get("liveHomeGoals")+(Integer)get("liveAwayGoals");
                int recorded=0;for(int n:((Map<Integer,Integer>)get("matchGoals")).values())recorded+=n;
                check(shots>0,"complete match creates attacking events");
                check(goals<=target&&target<=shots,"complete match shot totals are consistent");
                check(recorded==goals,"scorer events match final score");
                int aiSubs=(Integer)get("aiSubsUsed");
                check(aiSubs>=0&&aiSubs<=5,"opposition respects substitution limit");
                report.append("MATCH shots=").append(shots).append(" target=").append(target).append(" goals=").append(goals).append(" AI subs=").append(aiSubs).append('\n');
                ui(()->call("loadSave",new Class[]{int.class},0));
                check((Integer)get("matchday")==2,"complete simulation survives reload");
            }
            try(FileOutputStream f=new FileOutputStream(new File(output,"report.txt"))){f.write(report.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}
            result.putString("stream",report.toString());finish(Activity.RESULT_OK,result);
        }catch(Throwable e){
            android.util.Log.e("BOSSXI_QA","Feature regression failed",e);
            result.putString("stream",report+"FAIL "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);
        }
    }
}
