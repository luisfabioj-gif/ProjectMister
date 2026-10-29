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
    private int emulatorAnrs;
    public void onCreate(Bundle args){super.onCreate(args);this.args=args;start();}
    private Object call(String name,Class<?>[] types,Object... values) throws Exception {
        Method m=activity.getClass().getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(activity,values);
    }
    private Object get(String name)throws Exception {Field f=activity.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(activity);}
    private void set(String name,Object value)throws Exception {Field f=activity.getClass().getDeclaredField(name);f.setAccessible(true);f.set(activity,value);}
    interface Work {void run()throws Exception;}
    private void ui(Work w)throws Exception {
        final Throwable[] error={null};final boolean[] idle={true};
        runOnMainSync(()->{try{w.run();idle[0]=!((Boolean)get("liveMatchActive")&&!(Boolean)get("livePaused"));}catch(Throwable e){error[0]=e;}});
        if(error[0]!=null)throw new Exception(error[0]);
        // A continuously rendered match need not become globally idle. Waiting
        // here can starve the bounded match-progress checks on a slow emulator.
        // Paused/layout screens still receive the original idle/layout checks.
        if(idle[0])waitForIdleSync();
    }
    private void check(boolean ok,String label){if(!ok)throw new AssertionError(label);report.append("PASS ").append(label).append('\n');}
    private void page(String name,String method,Class<?>[] types,Object... values)throws Exception {
        ui(()->call(method,types,values));SystemClock.sleep(500);capture(name);
        check(!activity.isFinishing(),name+" navigates");
    }
    private void awaitBoolean(String field)throws Exception {
        long end=SystemClock.uptimeMillis()+120000;
        final boolean[] value={false};
        while(SystemClock.uptimeMillis()<end) {
            ui(()->value[0]=(Boolean)get(field));if(value[0])return;SystemClock.sleep(150);
        }
        throw new AssertionError("Timed out waiting for "+field);
    }
    private void awaitRound(int target)throws Exception {
        long end=SystemClock.uptimeMillis()+120000;
        final int[] round={0};final String[] state={""};
        while(SystemClock.uptimeMillis()<end) {
            ui(()->{round[0]=(Integer)get("matchday");state[0]="round="+round[0]+", minute="+get("liveMinuteFloat")+", active="+get("liveMatchActive")+", paused="+get("livePaused");});if(round[0]>=target)return;SystemClock.sleep(150);
        }
        throw new AssertionError("Timed out waiting for full-time (target="+target+", "+state[0]+")");
    }
    private void awaitNaturalMatchPhase(boolean halfTime,int targetRound)throws Exception {
        long started=SystemClock.uptimeMillis(),end=started+240000,lastProgress=started,lastLog=started;
        float previous=-1;final float[] minute={0};final boolean[] done={false};final String[] state={""};
        while(SystemClock.uptimeMillis()<end){
            ui(()->{minute[0]=(Float)get("liveMinuteFloat");done[0]=halfTime?(Boolean)get("liveHalfTimeTacticsActive"):(Integer)get("matchday")>=targetRound;state[0]="round="+get("matchday")+", minute="+minute[0]+", active="+get("liveMatchActive")+", paused="+get("livePaused");});
            if(done[0])return;
            long now=SystemClock.uptimeMillis();if(minute[0]>previous+.0001f){previous=minute[0];lastProgress=now;}
            if(now-lastLog>=10000){android.util.Log.i("BOSSXI_QA_PROGRESS",state[0]);lastLog=now;}
            if(now-lastProgress>45000)throw new AssertionError("Natural match stopped advancing for 45 seconds ("+state[0]+")");
            SystemClock.sleep(150);
        }
        throw new AssertionError("Natural match exceeded four-minute phase deadline ("+state[0]+")");
    }
    @SuppressWarnings("unchecked") private void verifyNaturalMatch()throws Exception{
        ui(()->{check((Boolean)call("loadSave",new Class[]{int.class},0),"natural match loads upgraded career");((java.util.Random)get("random")).setSeed(35);call("startLiveMatchday",new Class[0]);call("setLiveSpeed",new Class[]{int.class},2);});
        awaitNaturalMatchPhase(true,1);capture("natural-progress-halftime");verifyTacticsMarkers();check(true,"natural first half reaches half-time without clock jumps");
        ui(()->call("returnFromLiveTactics",new Class[0]));awaitNaturalMatchPhase(false,1);capture("natural-progress-fulltime");
        int shots=(Integer)get("liveHomeShots")+(Integer)get("liveAwayShots"),target=(Integer)get("liveHomeOnTarget")+(Integer)get("liveAwayOnTarget"),goals=(Integer)get("liveHomeGoals")+(Integer)get("liveAwayGoals"),recorded=0;
        for(int n:((Map<Integer,Integer>)get("matchGoals")).values())recorded+=n;
        check(shots>0&&goals<=target&&target<=shots&&recorded==goals,"natural match event totals and scorers are consistent");
        ui(()->check((Boolean)call("loadSave",new Class[]{int.class},0)&&(Integer)get("matchday")==1,"natural full-time result survives career reload"));
        check(true,"natural full match advances without stalls and preserves its result");
    }
    private void dismissEmulatorSystemAnr() throws Exception {
        android.view.accessibility.AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();
        if(root==null || !"android".contentEquals(root.getPackageName())) return;
        // Only observed AOSP launcher/System UI dialogs. BOSS XI ANRs must still fail.
        String observed=null;
        for(String title:new String[]{"Quickstep isn't responding","System UI isn't responding"})
            for(android.view.accessibility.AccessibilityNodeInfo n:root.findAccessibilityNodeInfosByText(title))
                if(n.isVisibleToUser()&&title.contentEquals(n.getText()))observed=title;
        if(observed==null)return;
        if(++emulatorAnrs>2)throw new AssertionError("Repeated emulator system ANR: "+observed);
        Bitmap evidence=getUiAutomation().takeScreenshot();
        if(evidence!=null){try(FileOutputStream f=new FileOutputStream(new File(output,"emulator-system-anr-"+emulatorAnrs+".png"))){evidence.compress(Bitmap.CompressFormat.PNG,100,f);}evidence.recycle();}
        for(android.view.accessibility.AccessibilityNodeInfo n:root.findAccessibilityNodeInfosByText("Close app")) {
            if(n.isVisibleToUser()&&n.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)) {
                report.append("ENVIRONMENT dismissed emulator ").append(observed).append("; game checks remain enabled\n");
                SystemClock.sleep(500);waitForIdleSync();return;
            }
        }
        throw new AssertionError("Emulator system ANR could not be dismissed");
    }
    private void capture(String name)throws Exception {
        // Wait through asynchronous layout, portrait decoding and orientation changes.
        SystemClock.sleep(700);waitForIdleSync();
        dismissEmulatorSystemAnr();
        Bitmap b=getUiAutomation().takeScreenshot();
        if(b==null)throw new AssertionError("Screenshot missing: "+name);
        try(FileOutputStream f=new FileOutputStream(new File(output,name+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,f);} b.recycle();
        Bundle checkpoint=new Bundle();checkpoint.putString("stream","CHECKPOINT screenshot "+name+"\n");sendStatus(0,checkpoint);
    }
    private void verifyTacticsMarkers()throws Exception {
        ui(()->{
            ViewGroup pitch=activity.getWindow().getDecorView().findViewWithTag("live-tactics-pitch");
            check(pitch!=null&&pitch.getChildCount()==12,"tactics pitch renders all eleven players");
            check(pitch.getChildAt(0).getWidth()==pitch.getWidth()&&pitch.getChildAt(0).getHeight()==pitch.getHeight(),"tactics pitch background fills measured area");
            for(int i=1;i<pitch.getChildCount();i++) {
                View a=pitch.getChildAt(i);
                check(a.getWidth()>0&&a.getHeight()>0&&a.isShown(),"tactics marker has visible layout "+i);
                android.graphics.Rect visible=new android.graphics.Rect();check(a.getLocalVisibleRect(visible)&&visible.top==0&&visible.bottom==a.getHeight(),"entire tactics token visible without pitch scrolling "+i);
                android.graphics.RectF ar=new android.graphics.RectF(a.getX(),a.getY(),a.getX()+a.getWidth(),a.getY()+a.getHeight());
                check(ar.left>=0&&ar.top>=0&&ar.right<=pitch.getWidth()&&ar.bottom<=pitch.getHeight(),"tactics marker in bounds "+i);
                for(String tag:new String[]{"boss-live-ability","boss-live-condition"}){
                    android.widget.TextView stat=a.findViewWithTag(tag);check(stat!=null&&stat.getLayout()!=null,"tactics stat is laid out "+tag+"/"+i);
                    check(stat.getPaint().measureText(stat.getText().toString())<=stat.getWidth()-stat.getPaddingLeft()-stat.getPaddingRight()+.5f&&stat.getLayout().getEllipsisCount(0)==0,"complete tactics stat fits without clipping "+tag+"/"+i);
                    check(stat.getPaint().getFontMetrics().descent-stat.getPaint().getFontMetrics().ascent<=stat.getHeight()+.5f,"tactics stat fits its row height "+tag+"/"+i);
                }
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
            dismissEmulatorSystemAnr();
            android.view.accessibility.AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();
            if(root!=null){
                List<android.view.accessibility.AccessibilityNodeInfo> matches=root.findAccessibilityNodeInfosByText(text);
                for(android.view.accessibility.AccessibilityNodeInfo n:matches)if(n.isVisibleToUser()&&n.getText()!=null&&text.contentEquals(n.getText()))return n;
                for(android.view.accessibility.AccessibilityNodeInfo n:matches)if(n.isVisibleToUser()&&n.isClickable())return n;
                for(android.view.accessibility.AccessibilityNodeInfo n:matches){if(n.isVisibleToUser())return n;n.performAction(android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN.getId());}
            }
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
    private void collectScreenText(View view,StringBuilder text) {
        if(view instanceof android.widget.TextView)text.append(((android.widget.TextView)view).getText()).append('\n');
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)collectScreenText(((ViewGroup)view).getChildAt(i),text);
    }
    private String screenText(){StringBuilder b=new StringBuilder();collectScreenText(activity.getWindow().getDecorView(),b);return b.toString();}
    private void premiumPage(String name,String method,Class<?>[] types,Object... values)throws Exception {
        page("premium-"+name,method,types,values);
        ui(()->{
            View root=activity.getWindow().getDecorView().findViewWithTag("boss-premium-root");check(root!=null,name+" uses the shared premium shell");
            View heading=root.findViewWithTag("boss-screen-title");check(heading!=null&&heading.getWidth()>0&&heading.getHeight()>0,name+" heading has a measured layout");
            ViewGroup nav=root.findViewWithTag("boss-bottom-nav");if(nav!=null){check(nav.getChildCount()==5,name+" has all five navigation destinations");for(int i=0;i<5;i++){View item=nav.getChildAt(i);check(item.getWidth()>0&&item.getHeight()>=Math.round(48*activity.getResources().getDisplayMetrics().density),name+" navigation touch target "+i);}}
        });
    }
    private void verifyPremiumUI()throws Exception {
        SharedPreferences prefs=getTargetContext().getSharedPreferences("project_mister",Context.MODE_PRIVATE);byte[] original=backupBytes(prefs.getAll());
        try{
            ui(()->check((Boolean)call("loadSave",new Class[]{int.class},0),"premium update loads the legacy career"));
            premiumPage("career-hub","showMainMenu",new Class[0]);
            ui(()->{seedRulesCareer("ENG",1,true);set("managerFirstName","Luis");set("managerLastName","Jardim");set("attributeMasking",false);call("saveCurrentGame",new Class[0]);});
            int club=(Integer)get("selectedClub"),player=club*20;
            premiumPage("dashboard","showDashboard",new Class[0]);tap("Squad");tap("GK");ui(()->check((Integer)get("squadFilter")==1,"goalkeeper filter responds to a real touch"));capture("premium-squad-goalkeepers");tap("All");ui(()->check((Integer)get("squadFilter")==0,"all-player filter restores the squad"));
            premiumPage("squad","showTeamPlayers",new Class[]{int.class},club);
            premiumPage("player","showPlayerProfile",new Class[]{int.class,int.class},player,club);
            premiumPage("staff","showStaffHub",new Class[0]);premiumPage("staff-profile","showStaffProfile",new Class[]{String.class},"Assistant Manager");premiumPage("manager","showManagerProfile",new Class[0]);
            premiumPage("tactics","showTactics",new Class[0]);
            ui(()->{ViewGroup board=activity.getWindow().getDecorView().findViewWithTag("boss-squad-board");check(board!=null&&board.getChildCount()==12,"portrait tactics board has the pitch and eleven tokens");Set<String> names=new HashSet<>();for(Object p:(List<?>)get("players")){int id=playerInt(p,"id");if(playerInt(p,"team")==club&&((int[])get("playerRoleStatus"))[id]==2)names.add(playerString(p,"name"));}Set<String> shown=new HashSet<>();for(int i=1;i<board.getChildCount();i++){View token=board.getChildAt(i);check(token.getLeft()>=0&&token.getTop()>=0&&token.getRight()<=board.getWidth()&&token.getBottom()<=board.getHeight(),"portrait lineup token fits "+i);String label=String.valueOf(token.getContentDescription());String name=label.substring(0,label.lastIndexOf(", "));check(names.contains(name)&&shown.add(name),"portrait lineup shows a unique actual selected player "+i);}check(board.getChildAt(1).performClick(),"portrait lineup opens its assignment action");});check(node("Reserve / Unselected")!=null,"lineup touch opens the actual player assignment menu");capture("premium-tactics-assignment");tap("Cancel");
            premiumPage("training","showTraining",new Class[0]);premiumPage("transfers","showTransferHub",new Class[0]);premiumPage("world-market","showWorldMarket",new Class[0]);premiumPage("scouting","showScoutCentre",new Class[0]);premiumPage("scout-search","showScoutSearch",new Class[0]);
            premiumPage("finances","showFinances",new Class[0]);premiumPage("stadium","showStadiumCentre",new Class[0]);premiumPage("table","showLeagueTable",new Class[0]);premiumPage("calendar","showCompetitionCalendar",new Class[0]);premiumPage("history","showHistoryHub",new Class[0]);premiumPage("board","showClubOffice",new Class[0]);premiumPage("inbox","showInbox",new Class[0]);premiumPage("more","showClubHub",new Class[0]);
            ui(()->call("showSoundSettings",new Class[0]));capture("premium-audio-sheet");tap("Done");
            ui(()->call("showDashboard",new Class[0]));tap("Tactics");ui(()->check(screenText().contains("TEAM TACTICS"),"floating navigation opens tactics"));tap("Inbox");ui(()->check(screenText().contains("Inbox"),"floating navigation opens inbox"));tap("More");ui(()->check(screenText().contains("Club hub"),"floating navigation opens the club hub"));tap("Home");ui(()->check(activity.getWindow().getDecorView().findViewWithTag("boss-fixture-hero")!=null,"floating Home returns to the next fixture"));
            ui(()->{call("startLiveMatchday",new Class[0]);set("livePaused",true);});capture("premium-live-match");
            ui(()->call("showLiveStatsDialog",new Class[0]));capture("premium-live-stats");tap("Close");
            ui(()->call("showLiveTacticsScreen",new Class[0]));capture("premium-live-tactics");verifyTacticsMarkers();
            ui(()->{call("stopLiveMatchTicker",new Class[0]);call("stopLiveMatchAudio",new Class[0]);set("matchInProgress",false);set("liveTacticsSessionActive",false);check((Boolean)call("loadSave",new Class[]{int.class},2),"premium career reloads after match preview");call("showDashboard",new Class[0]);});
            check(true,"premium shell, all club screens, functional navigation, squad filters, lineup selection and live presentation verified");
        }finally{ui(()->{call("stopLiveMatchTicker",new Class[0]);call("stopLiveMatchAudio",new Class[0]);set("matchInProgress",false);set("liveTacticsSessionActive",false);gameClass("BackupRestore").getMethod("restore",SharedPreferences.class,byte[].class).invoke(null,prefs,original);call("loadSave",new Class[]{int.class},0);});}
    }

    private Object objectGet(Object object,String name)throws Exception{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
    private void objectSet(Object object,String name,Object value)throws Exception{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);f.set(object,value);}
    private Object objectCall(Object object,String name,Class<?>[] types,Object... values)throws Exception{Method m=object.getClass().getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(object,values);}
    private void verifyRequestedFixes()throws Exception{
        SharedPreferences prefs=getTargetContext().getSharedPreferences("project_mister",Context.MODE_PRIVATE);byte[] original=backupBytes(prefs.getAll());
        try{
            ui(()->{check((Boolean)call("loadSave",new Class[]{int.class},1)&&get("division")!=null&&(Integer)get("fixtureVersion")==6,"2.3.13 linked modern career upgrades into 2.3.14");check((Boolean)call("loadSave",new Class[]{int.class},0)&&(Integer)get("fixtureVersion")==0,"legacy fixture order upgrades into 2.3.14");seedRulesCareer("PT",1,true);set("managerFirstName","Luis");set("managerLastName","Jardim");call("saveCurrentGame",new Class[0]);});
            page("fixes-home","showDashboard",new Class[0]);
            ui(()->{ViewGroup tabs=activity.getWindow().getDecorView().findViewWithTag("boss-home-tabs");check(tabs!=null&&tabs.getChildCount()==3,"all dashboard tabs exist");for(int i=0;i<3;i++){View tab=tabs.getChildAt(i);check(tab.getTop()>=0&&tab.getBottom()<=tabs.getHeight(),"dashboard tab fully inside row "+i);android.graphics.Rect visible=new android.graphics.Rect();check(tab.getLocalVisibleRect(visible)&&visible.top==0&&visible.bottom==tab.getHeight(),"dashboard tab fully visible "+i);check(tab.getElevation()>0,"tab has raised surface "+i);}check(!screenText().contains("MANAGER BRIEFING"),"manager briefing removed");});
            page("fixes-world-market","showWorldMarket",new Class[0]);tap("All players");capture("fixes-database");
            ui(()->{List<?> rows=(List<?>)call("recruitmentRows",new Class[0]);Set<String> keys=new HashSet<>();int active=0,free=0,world=0;for(Object row:rows){String key=(String)row.getClass().getField("key").get(row);if(!keys.add(key))throw new AssertionError("Duplicate database player "+key);if(key.startsWith("P"))active++;else if(key.startsWith("Mfree"))free++;else world++;}check(active==((List<?>)get("players")).size()&&free==60&&world>1000,"database includes every active player free agent and worldwide recruit");ViewGroup results=activity.getWindow().getDecorView().findViewWithTag("recruitment-results");check(results!=null&&results.getChildCount()==26,"full database is paginated instead of building thousands of views");});
            tap("Sort");tap("Youngest first");ui(()->check((Integer)get("recruitmentSort")==2,"database sort responds to touch"));
            tap("Filter");tap("Position: All");tap("Goalkeepers");tap("Apply filters");capture("fixes-database-filtered");
            ui(()->{Object filter=get("recruitmentFilter");check(filter.getClass().getField("position").get(filter).equals("Goalkeepers"),"position filter responds to touch");check(screenText().contains("Filter • active"),"active filter shown");});
            page("fixes-unattached","showMarketClub",new Class[]{String.class,String.class,int.class},"free","Unattached players",0);tap("Sort");tap("Future ability ↓");tap("Filter");capture("fixes-unattached-filters");tap("Reset");
            ui(()->{call("startLiveMatchday",new Class[0]);set("livePaused",true);});capture("fixes-live-before");
            ui(()->{
                call("stopLiveMatchTicker",new Class[0]);set("livePaused",false);Object pitch=get("livePitchView");
                int count=0;while((Boolean)objectCall(pitch,"restartInProgress",new Class[0])&&count++<3000)objectCall(pitch,"stepSimulation",new Class[]{float.class},1f/120);
                check(count<3000,"opening teams actually reach kickoff positions");
                int team=(Integer)get("liveHome"),slot=7;set("livePossessionTeam",team);set("liveCarrierSlot",slot);
                float x=(Float)objectCall(pitch,"getPlayerX",new Class[]{int.class,int.class},team,slot),y=(Float)objectCall(pitch,"getPlayerY",new Class[]{int.class,int.class},team,slot);
                objectCall(pitch,"holdBallAt",new Class[]{int.class,int.class},team,slot);objectCall(pitch,"animateDribble",new Class[]{int.class,int.class,float.class,float.class,float.class},team,slot,x+.12f,y+.05f,1.8f);
                for(int i=0;i<200;i++){objectCall(pitch,"stepSimulation",new Class[]{float.class},1f/120);float px=(Float)objectCall(pitch,"getPlayerX",new Class[]{int.class,int.class},team,slot),py=(Float)objectCall(pitch,"getPlayerY",new Class[]{int.class,int.class},team,slot),bx=(Float)objectCall(pitch,"getBallX",new Class[0]),by=(Float)objectCall(pitch,"getBallY",new Class[0]);if(Math.hypot(px-bx,(py-by)*.648f)>.018f)throw new AssertionError("ball left dribbler at frame "+i);}check(true,"ball remains at carrier feet for every dribble frame");
                objectCall(pitch,"prepareReceivingRun",new Class[]{int.class,int.class,boolean.class},team,8,true);objectCall(pitch,"animateBallToSlot",new Class[]{int.class,int.class,int.class,int.class,float.class},team,slot,team,8,1.2f);
                for(int i=0;i<150;i++)objectCall(pitch,"stepSimulation",new Class[]{float.class},1f/120);
                float px=(Float)objectCall(pitch,"getPlayerX",new Class[]{int.class,int.class},team,8),py=(Float)objectCall(pitch,"getPlayerY",new Class[]{int.class,int.class},team,8),bx=(Float)objectCall(pitch,"getBallX",new Class[0]),by=(Float)objectCall(pitch,"getBallY",new Class[0]);check(Math.hypot(px-bx,(py-by)*.648f)<.018f,"moving reception arrives at the actual receiving player");
                set("pendingGoalTeam",team);set("pendingGoalSlot",8);call("completePendingGoal",new Class[0]);float minute=(Float)get("liveMinuteFloat");
                count=0;while((Boolean)objectCall(pitch,"restartInProgress",new Class[0])&&count++<4000){call("advanceLiveSimulation",new Class[]{float.class},1f/120);if(((Float)get("liveMinuteFloat")).floatValue()!=minute)throw new AssertionError("Clock advanced during celebration/reset");}
                check(count>408&&count<4000,"goal celebration and physical reset both complete");
                int taking=(Integer)get("livePossessionTeam");check(taking==(Integer)get("liveAway"),"conceding team takes the kickoff");
                for(String side:new String[]{"home","away"}){float[] xs=(float[])objectGet(pitch,side+"X");boolean home=side.equals("home");for(int i=0;i<11;i++)check(home?xs[i]<.51f:xs[i]>.49f,"both teams reposition before restart "+side+i);}
                check(!(Boolean)get("liveNeedsKickoffReset"),"goal reset cleared only after arrival");set("livePaused",true);set("liveHalfTimeTacticsActive",true);
            });
            page("fixes-half-time","showLiveTacticsScreen",new Class[0]);verifyTacticsMarkers();
            ui(()->{ViewGroup board=activity.getWindow().getDecorView().findViewWithTag("live-tactics-pitch");for(int i=1;i<board.getChildCount();i++){String label=String.valueOf(board.getChildAt(i).getContentDescription());check(label.contains("CA ")&&label.contains("condition"),"half-time token shows ability and condition "+i);check(board.getChildAt(i) instanceof ViewGroup,"half-time token uses numbered tactics marker "+i);}});
            ui(()->call("showLiveRatingsDialog",new Class[0]));capture("fixes-contribution");tap("Close");
            ui(()->{call("stopLiveMatchTicker",new Class[0]);call("stopLiveMatchAudio",new Class[0]);set("matchInProgress",false);set("liveTacticsSessionActive",false);call("loadSave",new Class[]{int.class},2);});
            page("fixes-inbox","showInbox",new Class[0]);
            ui(()->{
                List<?> inbox=(List<?>)get("inbox");int count=inbox.size();call("ensureMediaNews",new Class[0]);check(inbox.size()==count,"weekly newspaper and interview are not duplicated");Object interview=null;for(Object n:inbox)if(((String)n.getClass().getField("action").get(n)).startsWith("interview:")){interview=n;break;}check(interview!=null,"fictional journalist requests an interview");check(interview.getClass().getField("date").get(interview)!=null,"new messages have career dates");
                Object player=((List<?>)get("players")).get(0);int morale=playerInt(player,"morale");call("answerInterview",new Class[]{interview.getClass(),int.class},interview,0);check(playerInt(player,"morale")==Math.min(100,morale+1),"interview response changes actual squad morale");call("answerInterview",new Class[]{interview.getClass(),int.class},interview,0);check(playerInt(player,"morale")==Math.min(100,morale+1),"repeat interview cannot change morale again");String encoded=(String)call("encodeInbox",new Class[0]);call("saveCurrentGame",new Class[0]);call("loadSave",new Class[]{int.class},2);check(call("encodeInbox",new Class[0]).equals(encoded),"dates read states and interview replies survive reload");
                java.time.LocalDate now=(java.time.LocalDate)get("currentDate");call("advanceCareerDate",new Class[]{java.time.LocalDate.class},now.plusDays(7));check(((List<?>)get("inbox")).size()>count,"new press stories arrive as career advances");call("saveCurrentGame",new Class[0]);checkBackupReadable();call("showInbox",new Class[0]);
            });capture("fixes-inbox-new-stories");
            check(true,"all nine requested recruitment UI inbox possession and goal-reset fixes verified");
        }finally{ui(()->{call("stopLiveMatchTicker",new Class[0]);call("stopLiveMatchAudio",new Class[0]);set("matchInProgress",false);set("liveTacticsSessionActive",false);gameClass("BackupRestore").getMethod("restore",SharedPreferences.class,byte[].class).invoke(null,prefs,original);call("loadSave",new Class[]{int.class},0);});}
    }

    private void verifyFeedback()throws Exception {
        SharedPreferences prefs=getTargetContext().getSharedPreferences("project_mister",Context.MODE_PRIVATE);byte[] original=backupBytes(prefs.getAll());
        final String[] identity={null},reports={null};final int[] target={-1};final java.time.LocalDate[] started={null};
        ui(()->{
            String[] leagues=(String[])get("LEAGUE_OPTIONS");check(leagues.length==20&&!Arrays.toString(leagues).contains("Classic"),"new careers offer twenty real divisions without Classic");
            Object profiles=get("clubProfiles");check(profiles!=null&&(Integer)profiles.getClass().getMethod("size").invoke(profiles)==365,"all 365 playable clubs have validated observed profiles");
            seedRulesCareer("ENG",1,true);Object world=get("division");String[] ids=(String[])world.getClass().getField("clubIds").get(world);int arsenal=Arrays.asList(ids).indexOf("eng:arsenal"),hull=Arrays.asList(ids).indexOf("eng:hull-city"),city=Arrays.asList(ids).indexOf("eng:manchester-city");
            int[] colours=(int[])get("primaryColours"),strength=(int[])get("strength"),budgets=(int[])get("budgets");
            check(android.graphics.Color.red(colours[arsenal])>android.graphics.Color.blue(colours[arsenal])&&android.graphics.Color.blue(colours[city])>android.graphics.Color.red(colours[city]),"Arsenal red and Manchester City blue replace arbitrary colours");
            check(strength[arsenal]>strength[hull]&&budgets[arsenal]>budgets[hull],"squad-value calibration distinguishes title contenders and smaller clubs");
            Set<String> nations=new HashSet<>();for(int i=0;i<20;i++)nations.add(playerString(call("findPlayer",new Class[]{int.class},arsenal*20+i),"nationality"));check(nations.size()>5&&nations.contains("England"),"English club has observed multinational squad proportions");
            set("selectedClub",arsenal);call("initialiseTacticsForClub",new Class[0]);call("resetFinanceStadiumState",new Class[0]);set("attributeMasking",false);target[0]=(arsenal==0?20:0);Object p=call("findPlayer",new Class[]{int.class},target[0]);call("showPlayerProfile",new Class[]{int.class,int.class},target[0],playerInt(p,"team"));
            check(screenText().contains("Scout player"),"explicit scout button remains available with masking off");
            started[0]=(java.time.LocalDate)get("currentDate");identity[0]=(String)call("playerIdentitySnapshot",new Class[0]);
        });capture("feedback-player-scout-action");tap("Scout player");
        ui(()->{
            Object book=get("scoutingReports"),entry=book.getClass().getMethod("get",String.class).invoke(book,"P"+target[0]);check(entry!=null&&(Boolean)entry.getClass().getMethod("pending").invoke(entry),"player action creates an actual scouting assignment");
            call("advanceCareerDate",new Class[]{java.time.LocalDate.class},started[0].plusDays(6));check(((List<?>)entry.getClass().getMethod("report").invoke(entry)).isEmpty(),"scouting report is unavailable before seven days");
            call("advanceCareerDate",new Class[]{java.time.LocalDate.class},started[0].plusDays(7));entry=book.getClass().getMethod("get",String.class).invoke(book,"P"+target[0]);check(!(Boolean)entry.getClass().getMethod("pending").invoke(entry)&&((List<?>)entry.getClass().getMethod("report").invoke(entry)).size()>=15,"dated scouting report completes through calendar progression");
            call("showScoutingReport",new Class[]{String.class},"P"+target[0]);String t=screenText();check(t.contains("Potential")&&t.contains("Current ability")&&t.contains("Recommendation")&&t.contains("Nationality")&&t.contains("Weekly wage"),"report presents ability potential nationality costs risks and recommendation");
            reports[0]=(String)book.getClass().getMethod("snapshot").invoke(book);call("saveCurrentGame",new Class[0]);check((Boolean)call("loadSave",new Class[]{int.class},2),"new identities and scout report load successfully");check(identity[0].equals(call("playerIdentitySnapshot",new Class[0]))&&reports[0].equals(get("scoutingReports").getClass().getMethod("snapshot").invoke(get("scoutingReports"))),"all player identities and dated assessments survive exact reload");checkBackupReadable();
            Map<String,Object> good=new HashMap<>(prefs.getAll());for(String field:new String[]{"p_identity","scouting_reports"}){Map<String,Object> bad=new HashMap<>(good);bad.put("save_2_"+field,"corrupted");boolean rejected=false;try{gameClass("BackupRestore").getMethod("restore",SharedPreferences.class,byte[].class).invoke(null,prefs,(Object)backupBytes(bad));}catch(InvocationTargetException expected){rejected=true;}check(rejected&&prefs.getAll().equals(good),"corrupt "+field+" rejected before any career changes");}
            call("showScoutingReport",new Class[]{String.class},"P"+target[0]);
        });capture("feedback-completed-scout-report");
        ui(()->{call("resetLeagueStandings",new Class[0]);call("showLeagueTable",new Class[0]);String t=screenText();check(t.contains("1st")&&t.contains("2nd")&&t.contains("3rd")&&t.contains("20th")&&!t.contains("Equal position")&&!t.contains("=2"),"level league table displays unique sequential ordinal positions");});capture("feedback-ordinal-table");
        ui(()->{call("showDashboard",new Class[0]);check(!screenText().contains("1/20"),"dashboard uses readable league position");});capture("feedback-dashboard");
        ui(()->{set("liveHome",0);set("liveAway",1);set("liveHomePasses",0);set("liveAwayPasses",0);View stats=(View)call("matchStatsCard",new Class[]{boolean.class},false);StringBuilder t=new StringBuilder();collectScreenText(stats,t);check(t.toString().contains("Possession")&&t.toString().contains("Shots")&&t.toString().contains("—"),"match summary has separate comparison rows and honest unavailable details");call("showLiveStatsDialog",new Class[0]);});capture("feedback-match-comparison");tap("Close");
        ui(()->{
            set("stadiumCapacity",10000);set("stadiumSeatedCapacity",10000);set("stadiumExpansionLimit",150000);set("financeBalanceK",5000000);set("stadiumProjectWeeks",0);set("stadiumProjectCode",0);
            for(String field:new String[]{"stadiumNorthLevel","stadiumEastLevel","stadiumSouthLevel","stadiumWestLevel","stadiumTrainingLevel","stadiumMedicalLevel","stadiumGymLevel","stadiumYouthLevel","stadiumShopLevel","stadiumBarsLevel","stadiumRestaurantsLevel","stadiumHospitalityLevel","stadiumFanZoneLevel","stadiumParkingLevel","stadiumMediaLevel","stadiumMuseumLevel"})set(field,1);
            call("showStadiumCentre",new Class[0]);
        });capture("feedback-complex-before");
        final int[] tapAt={0,0};ui(()->{View map=(View)get("stadiumMap");int[] location=new int[2];map.getLocationOnScreen(location);tapAt[0]=location[0]+(int)(map.getWidth()*.30f);tapAt[1]=location[1]+(int)(map.getHeight()*.12f);for(int stage=1;stage<=5;stage++){try(InputStream in=getTargetContext().getAssets().open("stadium/complex-level-"+stage+".webp")){Bitmap image=android.graphics.BitmapFactory.decodeStream(in);check(image!=null&&image.getWidth()==1280&&image.getHeight()==853,"architectural stage "+stage+" decodes on Android");image.recycle();}}});
        long touch=SystemClock.uptimeMillis();sendPointerSync(MotionEvent.obtain(touch,touch,MotionEvent.ACTION_DOWN,tapAt[0],tapAt[1],0));sendPointerSync(MotionEvent.obtain(touch,touch+80,MotionEvent.ACTION_UP,tapAt[0],tapAt[1],0));waitForIdleSync();check(node("North stand")!=null&&node("Plan upgrade")!=null,"complex touch opens the actual selected stand");capture("feedback-stand-selected");tap("Close");
        int[] codes={101,102,103,104,1,2,3,4,5,6,7,8,9,10,11,12};
        for(int stage=2;stage<=5;stage++)for(int code:codes){final int part=code,level=stage;final int[] bank={0},capacity={0},duration={0};
            ui(()->{bank[0]=(Integer)get("financeBalanceK");capacity[0]=(Integer)get("stadiumCapacity");call("showStadiumPart",new Class[]{int.class},part);});tap("Plan upgrade");tap("Approve");
            ui(()->{duration[0]=(Integer)get("stadiumProjectWeeks");check(duration[0]>0&&(Integer)call("stadiumPartLevel",new Class[]{int.class},part)==level-1,"component "+part+" stage "+level+" waits for construction");for(int week=0;week<duration[0];week++)call("advanceStadiumProject",new Class[0]);check((Integer)call("stadiumPartLevel",new Class[]{int.class},part)==level,"component "+part+" completes architectural stage "+level);check((Integer)get("financeBalanceK")<bank[0]&&(part<100?(Integer)get("stadiumCapacity")==capacity[0]:(Integer)get("stadiumCapacity")>capacity[0]),"component "+part+" preserves charged cash and correct capacity");call("saveCurrentGame",new Class[0]);});
            if(code==12){ui(()->{check((Boolean)call("loadSave",new Class[]{int.class},2),"all sixteen upgraded components reload at stage "+level);call("showStadiumCentre",new Class[0]);});capture("feedback-complex-stage-"+stage);}
        }
        ui(()->{call("showHistoryHub",new Class[0]);check(!screenText().contains("Official source"),"history hub has no source controls");});capture("feedback-history-hub");tap("UEFA Champions League");ui(()->{String t=screenText();check(t.contains("Real-world winners")&&!t.contains("Official source"),"history competition shows winner rows without source buttons");});capture("feedback-history-winners");
        ui(()->{call("showFinances",new Class[0]);check(!screenText().contains("Bank balance          "),"finance uses responsive labelled account rows");});capture("feedback-finances");
        ui(()->{gameClass("BackupRestore").getMethod("restore",SharedPreferences.class,byte[].class).invoke(null,prefs,original);check((Boolean)call("loadSave",new Class[]{int.class},0),"preserved baseline restored after feedback scenarios");});
        check(true,"all eleven feedback areas, exact identities, timed scouting, ordinal tables and sixty-four stadium upgrades verified");
    }
    private void verifyWorldMarket()throws Exception {
        SharedPreferences prefs=getTargetContext().getSharedPreferences("project_mister",Context.MODE_PRIVATE);
        byte[] original=backupBytes(prefs.getAll());final int[] first={-1};
        ui(()->{
            set("currentDate",java.time.LocalDate.of(2026,8,9));set("currentTransferBudget",10000);set("wageBudgetK",1000000);
            Object catalog=get("catalog");java.util.List<?> divisions=(java.util.List<?>)catalog.getClass().getField("divisions").get(catalog);
            int base=((java.util.List<?>)get("players")).size(),count=0;
            for(Object division:divisions) {
                java.util.List<?> clubs=(java.util.List<?>)division.getClass().getField("clubs").get(division);
                String club=null;for(Object candidate:clubs){String id=(String)candidate.getClass().getField("id").get(candidate);if((Integer)call("marketLocalClub",new Class[]{String.class},id)<0){club=id;break;}}
                check(club!=null,"division offers an external club in legacy QA career");int tier=division.getClass().getField("tier").getInt(division);
                int id=(Integer)call("signMarketPlayer",new Class[]{String.class},club+"~"+tier+"~17");check(id==base+count,"market recruit receives contiguous saved identity");if(count++==0)first[0]=id;
            }
            int budget=(Integer)get("currentTransferBudget");int free=(Integer)call("signMarketPlayer",new Class[]{String.class},"free~0~4");
            check(free==base+20&&(Integer)get("currentTransferBudget")==budget,"free agent joins without transfer fee");
            check((Integer)call("signMarketPlayer",new Class[]{String.class},"free~0~4")==-1,"duplicate free-agent signing rejected");
            set("currentTransferBudget",0);check((Integer)call("signMarketPlayer",new Class[]{String.class},"eng:arsenal~1~18")==-1,"unaffordable worldwide transfer rejected");
            set("wageBudgetK",0);check((Integer)call("signMarketPlayer",new Class[]{String.class},"free~0~5")==-1,"unaffordable free-agent wages rejected");
            set("wageBudgetK",1000000);set("currentDate",java.time.LocalDate.of(2026,10,15));int offWindow=(Integer)call("signMarketPlayer",new Class[]{String.class},"free~0~6");check(offWindow==base+21,"eligible Portuguese free agent joins outside the ordinary window");
            set("currentTransferBudget",1000);check((Integer)call("signMarketPlayer",new Class[]{String.class},"eng:arsenal~1~18")==-1,"contracted players remain blocked outside the ordinary window");
            set("currentDate",java.time.LocalDate.of(2027,3,2));check((Integer)call("signMarketPlayer",new Class[]{String.class},"free~0~8")==-1,"Portuguese free-agent final deadline enforced");
            call("loadSave",new Class[]{int.class},0);check(((java.util.List<?>)get("players")).size()==base+22,"all worldwide and off-window free-agent recruits survive reload once");
            check(playerInt(call("findPlayer",new Class[]{int.class},first[0]),"team")==0,"overseas recruit belongs to managed club after reload");
            checkBackupReadable();
            int[] roles=(int[])get("playerRoleStatus"),slots=(int[])get("playerSelectedSlot");
            String[] positions=(String[])get("playerSelectedPosition"),formation=(String[])call("formationSlots",new Class[0]);
            java.util.Arrays.fill(roles,0);java.util.Arrays.fill(slots,-1);
            int[] eleven={0,2,3,4,5,6,8,9,12,15,first[0]};
            for(int i=0;i<eleven.length;i++){roles[eleven[i]]=2;slots[eleven[i]]=i;positions[eleven[i]]=formation[i];}
            check((Integer)call("countRole",new Class[]{int.class},2)==11,"explicit QA lineup includes eleven starters");
            call("startLiveMatchday",new Class[0]);set("livePaused",true);
            check(((java.util.List<?>)get("liveHomeLineupIds")).contains(first[0])||((java.util.List<?>)get("liveAwayLineupIds")).contains(first[0]),"worldwide recruit participates in live lineup");
            check((Integer)call("signMarketPlayer",new Class[]{String.class},"free~0~7")==-1,"recruitment cannot mutate a staged live match");
            call("stopLiveMatchTicker",new Class[0]);set("matchInProgress",false);
        });
        ui(()->{gameClass("BackupRestore").getMethod("restore",SharedPreferences.class,byte[].class).invoke(null,prefs,original);call("loadSave",new Class[]{int.class},0);call("showWorldMarket",new Class[0]);});
        capture("31-worldwide-market");page("32-history-hub","showHistoryHub",new Class[0]);
        check(true,"worldwide transfers free agents reload budget and lineup verified");
    }
    private void seedRulesCareer(String country,int tier,boolean modern)throws Exception {
        Object catalog=get("catalog"),data=null;for(Object d:(List<?>)catalog.getClass().getField("divisions").get(catalog))if(d.getClass().getField("id").get(d).equals(country.toLowerCase(Locale.ROOT)+":"+tier))data=d;
        Object world=gameClass("CareerDivision").getMethod("countryCareer",catalog.getClass(),data.getClass()).invoke(null,catalog,data);call("configureDivision",new Class[]{gameClass("CareerDivision")},world);set("selectedSlot",2);set("selectedClub",0);call("initialiseNewManagerDefaults",new Class[0]);call("resetCareerState",new Class[0]);if(modern){set("fixtureVersion",6);call("resetLeagueStandings",new Class[0]);}call("generatePlayers",new Class[0]);call("initialiseTacticsForClub",new Class[0]);call("initialiseClassicCareerSystems",new Class[0]);call("resetFinanceStadiumState",new Class[0]);set("wageBudgetK",1000000);set("currentTransferBudget",1000000);
    }
    private void verifyRules()throws Exception {
        SharedPreferences prefs=getTargetContext().getSharedPreferences("project_mister",Context.MODE_PRIVATE);byte[] original=backupBytes(prefs.getAll());
        for(String country:new String[]{"PT","DE","ENG","ES","IT","FR","NL","BE","SCO","TR"})for(int tier=1;tier<=2;tier++) {final int level=tier;ui(()->{
            seedRulesCareer(country,level,false);int base=((List<?>)get("players")).size();set("currentDate",java.time.LocalDate.of(2026,8,15));int budget=(Integer)get("currentTransferBudget");check((Integer)call("signMarketPlayer",new Class[]{String.class},"free~0~2")==base&&(Integer)get("currentTransferBudget")==budget,country+" tier "+level+" open-window free hire has no fee");
            set("currentDate",java.time.LocalDate.of(2026,10,15));int hired=(Integer)call("signMarketPlayer",new Class[]{String.class},"free~0~1");check(country.equals("TR")?hired==-1:hired==base+1,country+" tier "+level+" actual ordinary free-agent exception enforced");
            String foreign=country.equals("ENG")?"pt:sl-benfica~1~18":"eng:arsenal~1~18";check((Integer)call("signMarketPlayer",new Class[]{String.class},foreign)==-1,country+" tier "+level+" ordinary contracted transfer blocked out of window");
            call("saveCurrentGame",new Class[0]);check((Boolean)call("loadSave",new Class[]{int.class},2),country+" registration hire reloads");checkBackupReadable();
        });}
        ui(()->{
            seedRulesCareer("PT",1,true);java.time.LocalDate start=(java.time.LocalDate)get("careerSeasonStart");int initial=(Integer)get("financePlayerWagesK"),wages=(Integer)call("totalPlayerWagesK",new Class[0]);set("stadiumProjectWeeks",5);set("stadiumProjectCode",1);
            call("advanceCareerDate",new Class[]{java.time.LocalDate.class},start.plusDays(6));check((Integer)get("financePlayerWagesK")==initial&&(Integer)get("stadiumProjectWeeks")==5,"Six calendar days cannot charge a full week or advance a facility");
            call("advanceCareerDate",new Class[]{java.time.LocalDate.class},start.plusDays(7));check((Integer)get("financePlayerWagesK")==initial+wages&&(Integer)get("stadiumProjectWeeks")==4,"One calendar week charges wages and advances a facility once");
            call("processFinanceWeek",new Class[]{boolean.class},true);call("processFinanceWeek",new Class[]{boolean.class},true);check((Integer)get("financePlayerWagesK")==initial+wages,"Two fixtures cannot double-charge weekly wages");
            call("advanceCareerDate",new Class[]{java.time.LocalDate.class},start.plusDays(14));check((Integer)get("financePlayerWagesK")==initial+2*wages&&(Integer)get("stadiumProjectWeeks")==3,"Second calendar week processed exactly once");call("saveCurrentGame",new Class[0]);check((Boolean)call("loadSave",new Class[]{int.class},2),"Calendar-week progress reloads");check((Integer)get("careerWeeksProcessed")==2&& (Integer)get("financePlayerWagesK")==initial+2*wages,"Weekly costs retain exact saved progress");checkBackupReadable();checkModernBackupCompleteness();
        });
        for(String country:new String[]{"PT","DE","ENG","ES","IT","FR","NL","BE","SCO","TR"}) {
            ui(()->{
                seedRulesCareer(country,1,true);
                // Isolate final league equality from the real all-competition career gate.
                // A jump to May cannot retain the unplayed opening Portuguese League Cup.
                for(String field:new String[]{"leagueCup","domesticCup","nationalCups","scottishLeagueCup","modernPortugueseCup","europeanCampaign"})set(field,null);
                Object world=get("division");int size=((String[])world.getClass().getField("clubIds").get(world)).length,rounds=(Integer)call("seasonRounds",new Class[0]);if(country.equals("SCO"))set("splitOrder",world.getClass().getMethod("members",int.class).invoke(world,1));
                Object ledger=gameClass("LeagueResults").getConstructor(int.class,boolean.class).newInstance(size,true),decisions=gameClass("LeagueDecisions").getConstructor(int.class,int.class).newInstance(2026,size);
                for(int round=0;round<rounds;round++)for(Object fixture:(Object[])call("fixturesForRound",new Class[]{int.class},round)){int home=fixture.getClass().getField("home").getInt(fixture),away=fixture.getClass().getField("away").getInt(fixture);ledger.getClass().getMethod("record",int.class,int.class,int.class,int.class,int.class).invoke(ledger,round,home,away,0,0);decisions.getClass().getMethod("recordDiscipline",int.class,int.class,int.class,int.class,int.class,int.class,int.class).invoke(decisions,round,home,away,0,0,0,0);}
                set("leagueResults",ledger);set("leagueDecisions",decisions);int[] points=(int[])get("points"),gf=(int[])get("goalsFor"),ga=(int[])get("goalsAgainst");for(int c=0;c<size;c++){points[c]=90-2*c;gf[c]=20;ga[c]=10;}points[1]=points[0];set("matchday",rounds);set("currentDate",call("leagueFixtureDate",new Class[]{int.class},rounds-1));String regular=(String)ledger.getClass().getMethod("snapshot").invoke(ledger);int[] originalPoints=points.clone();call("prepareLeagueDecisions",new Class[0]);
                if(country.equals("FR")||country.equals("IT"))check((Boolean)decisions.getClass().getMethod("complete").invoke(decisions),country+" isolated final equality uses a saved draw");
                else{Object event=decisions.getClass().getMethod("next",int.class).invoke(decisions,(Integer)get("selectedClub"));int index=(Integer)decisions.getClass().getMethod("fixtureIndex",event.getClass()).invoke(decisions,event);Object disciplinary=call("cupDisciplineFixture",new Class[0]);check(index>=0&&disciplinary.getClass().getField("round").getInt(disciplinary)==index,country+" deciding event has a stable valid discipline fixture index");call("advanceCareerDate",new Class[]{java.time.LocalDate.class},event.getClass().getField("date").get(event));for(Object player:(List<?>)get("players")){Field injury=player.getClass().getDeclaredField("injuredWeeks");injury.setAccessible(true);injury.setInt(player,0);}call("initialiseTacticsForClub",new Class[0]);call("advanceLeagueCup",new Class[0]);check((Boolean)get("liveLeagueDecision")&&(Boolean)get("matchInProgress"),country+" isolated neutral league decider is playable");set("livePaused",true);set("liveHomeGoals",2);set("liveAwayGoals",0);call("finishLiveMatch",new Class[0]);check((Boolean)decisions.getClass().getMethod("complete").invoke(decisions),country+" actual deciding score resolves the places");}
                check(regular.equals(ledger.getClass().getMethod("snapshot").invoke(ledger))&&Arrays.equals(originalPoints,(int[])get("points")),country+" deciding outcome cannot rewrite regular points/results");call("saveCurrentGame",new Class[0]);String saved=(String)decisions.getClass().getMethod("snapshot").invoke(decisions);check((Boolean)call("loadSave",new Class[]{int.class},2),country+" deciding outcome reloads");check(saved.equals(get("leagueDecisions").getClass().getMethod("snapshot").invoke(get("leagueDecisions"))),country+" exact deciding history retained");checkBackupReadable();call("showLeagueDecisionHistory",new Class[0]);
            });if(country.equals("PT")||country.equals("FR"))capture("decisions-"+country);
        }
        ui(()->{gameClass("BackupRestore").getMethod("restore",SharedPreferences.class,byte[].class).invoke(null,prefs,original);check((Boolean)call("loadSave",new Class[]{int.class},0),"Original career restored after isolated rule scenarios");});
        check(true,"twenty registration cases, calendar-week accounting and ten isolated final league decisions verified");
    }
    private void verifyCalendarIntegrity()throws Exception {
        final Object[] old={null,null,null};
        ui(()->{
            old[0]=get("currentDate");old[1]=get("selectedClub");old[2]=get("promotion");
            set("currentDate",java.time.LocalDate.of(2070,1,1));
            for(String tab:new String[]{"Cup","Taça","League Cup","Europe"})
                check(((java.util.List<?>)call("buildClubFixtures",new Class[]{String.class},tab)).isEmpty(),"calendar never invents elapsed-date results: "+tab);
            set("currentDate",old[0]);set("selectedClub",3);
            Object p=gameClass("FrenchPromotion").getConstructor(int[].class,int[].class).newInstance(new int[]{0,1,2,3,4},new int[]{15,16,17});
            Class<?> pc=p.getClass();
            while(!(Boolean)pc.getMethod("complete").invoke(p)) {
                Object tie=pc.getMethod("current").invoke(p);Class<?> tc=tie.getClass();
                String phase=tc.getMethod("phase").invoke(tie).toString();
                if(phase.equals("REGULATION"))tc.getMethod("recordRegulation",int.class,int.class).invoke(tie,0,0);
                else if(phase.equals("EXTRA_TIME"))tc.getMethod("recordExtraTime",int.class,int.class).invoke(tie,0,0);
                else {int home=(Integer)tc.getMethod("home").invoke(tie);tc.getMethod("recordPenalties",int.class,int.class).invoke(tie,home==2?2:4,home==2?4:2);}
            }
            set("promotion",p);String before=(String)pc.getMethod("snapshot").invoke(p);
            call("showCalendarTab",new Class[]{String.class},"Play-offs");
            StringBuilder text=new StringBuilder();collectScreenText(activity.getWindow().getDecorView(),text);
            String[] names=(String[])get("clubNames");
            check(text.toString().contains((String)gameClass("GameNames").getMethod("display",String.class).invoke(null,"Penalties: "+names[15]+" 4–2 "+names[3])),"playoff penalties identify return-leg home and away clubs");
            check(text.toString().contains((String)gameClass("GameNames").getMethod("display",String.class).invoke(null,"Winner: "+names[15])),"playoff calendar shows the stored winner");
            check(text.toString().contains("dates have not been scheduled"),"playoff calendar does not invent match dates");
            check(before.equals(pc.getMethod("snapshot").invoke(p)),"calendar projection preserves saved playoff state");
        });
        capture("30-playoff-calendar");
        ui(()->{set("currentDate",old[0]);set("selectedClub",old[1]);set("promotion",old[2]);call("showDashboard",new Class[0]);});
        check(true,"calendar recorded results and shootout orientation verified");
    }

    private void verifyPortugueseLeagueCup()throws Exception {
        SharedPreferences prefs=getTargetContext().getSharedPreferences("project_mister",Context.MODE_PRIVATE);
        byte[] original=backupBytes(prefs.getAll());
        ui(()->{
            Object catalog=get("catalog"),data=null;
            for(Object d:(List<?>)catalog.getClass().getField("divisions").get(catalog))if(d.getClass().getField("id").get(d).equals("pt:1"))data=d;
            check(data!=null,"Portuguese division found");Class<?> wc=gameClass("CareerDivision");
            Object world=wc.getMethod("countryCareer",catalog.getClass(),data.getClass()).invoke(null,catalog,data);
            call("configureDivision",new Class[]{wc},world);set("selectedSlot",2);set("selectedClub",0);
            call("initialiseNewManagerDefaults",new Class[0]);call("resetCareerState",new Class[0]);call("generatePlayers",new Class[0]);
            call("initialiseTacticsForClub",new Class[0]);call("initialiseClassicCareerSystems",new Class[0]);
            check(get("leagueCup")!=null,"new linked Portuguese career creates published cup draw");
            set("matchday",11);set("currentDate",java.time.LocalDate.of(2026,10,25));
            call("startLiveMatchday",new Class[0]);set("livePaused",true);call("finishLiveMatch",new Class[0]);
            check(get("currentDate").equals(java.time.LocalDate.of(2026,10,27)),"league week stops at intervening cup date");
            call("saveCurrentGame",new Class[0]);check((Boolean)call("loadSave",new Class[]{int.class},2),"cup schedule reloads");
        });
        final int[] balance={0},games={0};final String[] ledger={null};
        for(int stage=0;stage<3;stage++) {
            final int round=stage;
            ui(()->{
                if(round==1){set("matchday",22);set("currentDate",java.time.LocalDate.of(2027,1,5));}
                call("advanceCareerDate",new Class[]{java.time.LocalDate.class},get("currentDate"));
                for(Object p:(List<?>)get("players")){Field injury=p.getClass().getDeclaredField("injuredWeeks");injury.setAccessible(true);injury.setInt(p,0);}
                call("initialiseTacticsForClub",new Class[0]);
                balance[0]=(Integer)get("financeBalanceK");games[0]=((int[])get("played"))[0];
                Object history=get("leagueResults");ledger[0]=(String)history.getClass().getMethod("snapshot").invoke(history);
                call("startLiveMatchday",new Class[0]);set("livePaused",true);
                check((Boolean)get("liveCup")&&(Boolean)get("matchInProgress"),"cup advances background ties and starts managed live match");
                check(((List<?>)get("liveOtherFixtures")).isEmpty(),"cup has no league fixtures attached");
                check((Integer)get("liveHome")==0||(Integer)get("liveAway")==0,"manager participates in drawn cup tie");
                set("liveHomeGoals",(Integer)get("liveHome")==0?2:0);set("liveAwayGoals",(Integer)get("liveAway")==0?2:0);
            });
            if(stage==2)capture("33-league-cup-final-live");
            ui(()->{
                call("finishLiveMatch",new Class[0]);
                check((Integer)get("financeBalanceK")==balance[0],"cup does not repeat weekly finance");
                check(((int[])get("played"))[0]==games[0],"cup leaves league appearances unchanged");
                Object history=get("leagueResults");check(ledger[0].equals(history.getClass().getMethod("snapshot").invoke(history)),"cup leaves league ledger unchanged");
                check((Boolean)call("loadSave",new Class[]{int.class},2),"cup result reloads");checkBackupReadable();
                if(round==0)check(get("currentDate").equals(java.time.LocalDate.of(2026,11,1)),"quarter-final returns to scheduled league date");
            });
        }
        ui(()->{
            Object cup=get("leagueCup");check((Integer)cup.getClass().getMethod("winner").invoke(cup)==0,"managed club wins saved cup final");
            check(get("currentDate").equals(java.time.LocalDate.of(2027,1,10)),"final returns to next league date");
            String snapshot=(String)cup.getClass().getMethod("snapshot").invoke(cup);
            call("startLiveMatchday",new Class[0]);set("livePaused",true);check(!(Boolean)get("liveCup"),"league resumes after cup final");
            call("finishLiveMatch",new Class[0]);check((Integer)get("matchday")==23,"league advances exactly once");
            check(snapshot.equals(cup.getClass().getMethod("snapshot").invoke(cup)),"league result cannot rewrite cup trophy");
            call("showCalendarTab",new Class[]{String.class},"League Cup");
        });
        capture("34-league-cup-winners");
        ui(()->{
            Map<String,Object> invalid=new HashMap<>(prefs.getAll());invalid.put("save_2_date","2026-08-09");
            boolean rejected=false;try{gameClass("BackupRestore").getMethod("validate",byte[].class).invoke(null,(Object)backupBytes(invalid));}catch(java.lang.reflect.InvocationTargetException expected){rejected=true;}
            check(rejected,"backup rejects future cup results");
            gameClass("BackupRestore").getMethod("restore",SharedPreferences.class,byte[].class).invoke(null,prefs,original);
            call("loadSave",new Class[]{int.class},0);
        });
        check(true,"Portuguese cup live quarter-final semi-final final and league resumption verified");
    }
    private void verifyEuropeanDraw(Object phase,Object previous,String label)throws Exception {
        Class<?> type=gameClass("EuropeanLeaguePhase");String snapshot=(String)type.getMethod("snapshot").invoke(phase);
        Object restored=type.getMethod("restore",String.class).invoke(null,snapshot);
        check(snapshot.equals(type.getMethod("snapshot").invoke(restored)),label+" validates all opponent/pot rules and restores canonically");
        int rounds=type.getField("competition").get(phase).toString().equals("CONFERENCE")?6:8;
        Map<Integer,boolean[]> venues=new HashMap<>();for(Object club:(List<?>)type.getMethod("clubs").invoke(phase))venues.put(club.getClass().getField("id").getInt(club),new boolean[rounds]);
        Set<String> previousVenues=new HashSet<>();if(previous!=null)for(int i=0;i<(Integer)type.getMethod("fixtureCount").invoke(previous);i++){Object f=type.getMethod("fixture",int.class).invoke(previous,i);previousVenues.add(f.getClass().getField("home").getInt(f)+":"+f.getClass().getField("away").getInt(f));}
        for(int i=0;i<(Integer)type.getMethod("fixtureCount").invoke(phase);i++){Object f=type.getMethod("fixture",int.class).invoke(phase,i);int home=f.getClass().getField("home").getInt(f),away=f.getClass().getField("away").getInt(f),round=f.getClass().getField("round").getInt(f);venues.get(home)[round]=true;if(previousVenues.contains(home+":"+away))throw new AssertionError(label+" repeats a previous-season venue");}
        for(boolean[] home:venues.values()){if(home[0]==home[1]||home[rounds-1]==home[rounds-2])throw new AssertionError(label+" opening/closing venues unbalanced");for(int r=0;r<rounds-2;r++)if(home[r]==home[r+1]&&home[r]==home[r+2])throw new AssertionError(label+" has three consecutive identical venues");}
        check(venues.size()==36,label+" preserves 36 entrants, balanced venue sequences and previous-season venue exclusions");
    }
    private void verifyCapturedEuropeanDraw()throws Exception {
        String input;try(InputStream in=getContext().getAssets().open("eng-2027-draw.txt")){input=new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).trim();}
        String[] parts=input.split("\\n--PREVIOUS--\\n"),rows=parts[0].split("\\n"),header=rows[1].split("\\|");
        Class<?> type=gameClass("EuropeanLeaguePhase"),clubType=gameClass("EuropeanLeaguePhase$Club"),competitionType=gameClass("EuropeanLeaguePhase$Competition");
        Object competition=competitionType.getMethod("valueOf",String.class).invoke(null,"CHAMPIONS"),previous=type.getMethod("restore",String.class).invoke(null,parts[1]);
        ArrayList<Object> field=new ArrayList<>();for(int i=2;i<rows.length;i++){String[] c=rows[i].split("\\|");field.add(clubType.getConstructor(int.class,String.class,int.class,int.class).newInstance(Integer.parseInt(c[1]),c[2],Integer.parseInt(c[3]),Integer.parseInt(c[4])));}
        int year=Integer.parseInt(header[0]);long seed=Long.parseLong(header[1]);ArrayList<java.time.LocalDate> dates=new ArrayList<>();for(int r=0;r<8;r++)dates.add(java.time.LocalDate.of(year,9,10).plusWeeks(r*2L));
        Method create=gameClass("EuropeanDraw").getMethod("create",competitionType,int.class,List.class,List.class,long.class,type);
        Object phase=create.invoke(null,competition,year,field,dates,seed,previous);verifyEuropeanDraw(phase,previous,"Captured England regression");
        check((Boolean)type.getMethod("seedingAdjusted").invoke(phase),"captured impossible England pots still require an explicit saved seeding adjustment");
        int changed=0;for(Object club:(List<?>)type.getMethod("clubs").invoke(phase)){int id=clubType.getField("id").getInt(club);Object original=null;for(Object candidate:field)if(clubType.getField("id").getInt(candidate)==id)original=candidate;if(original==null)throw new AssertionError("Captured draw lost a qualified club");for(String name:new String[]{"association","coefficient"})if(!clubType.getField(name).get(club).equals(clubType.getField(name).get(original)))throw new AssertionError("Captured draw changed "+name);int seedPot=clubType.getField("seedPot").getInt(club);if(seedPot!=clubType.getField("pot").getInt(original))throw new AssertionError("Captured draw changed the original seed");if(clubType.getField("pot").getInt(club)!=seedPot)changed++;if(id==clubType.getField("id").getInt(field.get(0))&&clubType.getField("pot").getInt(club)!=0)throw new AssertionError("Captured titleholder lost pot one");}
        check(changed==4,"captured England field retains all identities and coefficients with exactly two seed exchanges");
        check(type.getMethod("snapshot").invoke(phase).equals(type.getMethod("snapshot").invoke(create.invoke(null,competition,year,field,dates,seed,previous))),"captured England adjusted draw replays deterministically on Android");
    }
    private void verifyEuropeanCareer(String country)throws Exception {
        SharedPreferences prefs=getTargetContext().getSharedPreferences("project_mister",Context.MODE_PRIVATE);byte[] original=backupBytes(prefs.getAll());
        if(country.equals("ENG"))verifyCapturedEuropeanDraw();
        final int[] watched={0},quick={0},qualifying={0};final boolean[] finished={false};final java.time.LocalDate[] lastManaged={null};
        ui(()->{
            Object data=null;for(Object d:(List<?>)get("catalog").getClass().getField("divisions").get(get("catalog")))if(d.getClass().getField("id").get(d).equals(country.toLowerCase(Locale.ROOT)+":1"))data=d;
            Object world=call("newCountryCareer",new Class[]{data.getClass()},data);call("configureDivision",new Class[]{gameClass("CareerDivision")},world);set("selectedSlot",2);set("selectedClub",0);((Random)get("random")).setSeed(23931+country.hashCode());
            call("initialiseNewManagerDefaults",new Class[0]);call("resetCareerState",new Class[0]);Object europe=get("europeanCampaign");check(europe!=null,country+" playable UEFA campaign initialized");
            int managed=-1;for(Object editions:(List<?>)europe.getClass().getMethod("competitions").invoke(europe)){Object season=editions.getClass().getMethod("active").invoke(editions),phase=season.getClass().getMethod("leaguePhase").invoke(season);for(Object club:(List<?>)phase.getClass().getMethod("clubs").invoke(phase))if(club.getClass().getField("association").get(club).equals(country)){managed=club.getClass().getField("id").getInt(club);break;}if(managed>=0)break;}
            check(managed>=0,country+" real local participant is available");set("selectedClub",managed);
            check(!((java.time.LocalDate)call("leagueFixtureDate",new Class[]{int.class},(Integer)call("seasonRounds",new Class[0])-1)).isAfter(java.time.LocalDate.of(2027,5,10)),country+" regular season leaves dated postseason room");call("generatePlayers",new Class[0]);call("initialiseTacticsForClub",new Class[0]);call("initialiseClassicCareerSystems",new Class[0]);call("saveCurrentGame",new Class[0]);checkBackupReadable();call("showCalendarTab",new Class[]{String.class},"Europe");
        });capture("europe-"+country+"-opening");
        for(int edition=0;edition<2;edition++){
            final int year=2026+edition,iteration=edition;finished[0]=false;lastManaged[0]=null;
            for(int step=0;step<240&&!finished[0];step++){
                final int index=step;
                ui(()->{
                    java.time.LocalDate before=(java.time.LocalDate)get("currentDate");int round=(Integer)get("matchday");String ledger=(String)get("leagueResults").getClass().getMethod("snapshot").invoke(get("leagueResults"));call("initialiseTacticsForClub",new Class[0]);call("startLiveMatchday",new Class[0]);
                    if((Boolean)get("matchInProgress")){
                        java.time.LocalDate date=(java.time.LocalDate)get("currentDate");check(lastManaged[0]==null||date.isAfter(lastManaged[0]),country+" managed club is never double booked");lastManaged[0]=date;
                        boolean euro=(Boolean)get("liveEuropean"),cup=(Boolean)get("liveCup");Object event=euro?get("liveEuropeanEvent"):null;int leg=euro&&event.getClass().getField("tie").get(event)!=null?(Integer)event.getClass().getField("tie").get(event).getClass().getMethod("playedLegs").invoke(event.getClass().getField("tie").get(event)):0;
                        set("livePaused",true);
                        if(euro&&(watched[0]+quick[0])%3==0){call("completeQuickResult",new Class[0]);quick[0]++;}
                        else{set("liveHomeGoals",get("liveHome").equals(get("selectedClub"))?5:0);set("liveAwayGoals",get("liveAway").equals(get("selectedClub"))?5:0);call("finishLiveMatch",new Class[0]);if(euro)watched[0]++;}
                        if(cup)check(round==(Integer)get("matchday")&&ledger.equals(get("leagueResults").getClass().getMethod("snapshot").invoke(get("leagueResults"))),country+" cup results preserve the league ledger");
                        if(euro){int fixture=event.getClass().getField("fixture").getInt(event);int[] recorded;
                            if(fixture>=0){Object season=get("europeanCampaign").getClass().getMethod("season",event.getClass().getField("competition").getType()).invoke(get("europeanCampaign"),event.getClass().getField("competition").get(event));Object phase=season.getClass().getMethod("leaguePhase").invoke(season);recorded=(int[])phase.getClass().getMethod("result",int.class).invoke(phase,fixture);}
                            else{Object tie=event.getClass().getField("tie").get(event);recorded=(int[])tie.getClass().getMethod("regulationScore",int.class).invoke(tie,leg);if(event.getClass().getField("qualifying").get(event)!=null)qualifying[0]++;}
                            check(recorded[0]==(Integer)get("liveHomeGoals")&&recorded[1]==(Integer)get("liveAwayGoals"),country+" actual watched/quick UEFA score stored");
                        }
                    }
                    check(!((java.time.LocalDate)get("currentDate")).isBefore(before),country+" combined calendar never rewinds");
                    if(index%17==0){call("saveCurrentGame",new Class[0]);String saved=(String)get("europeanCampaign").getClass().getMethod("snapshot").invoke(get("europeanCampaign"));check((Boolean)call("loadSave",new Class[]{int.class},2),country+" combined career reloads");check(saved.equals(get("europeanCampaign").getClass().getMethod("snapshot").invoke(get("europeanCampaign"))),country+" exact UEFA state after reload");checkBackupReadable();}
                    finished[0]=(Integer)get("matchday")== (Integer)call("seasonRounds",new Class[0])&&(Boolean)call("cupsComplete",new Class[0]);
                });
            }
            if(country.equals("ENG")&&year==2027){ui(()->{Object europe=get("europeanCampaign"),editions=((List<?>)europe.getClass().getMethod("competitions").invoke(europe)).get(0),season=editions.getClass().getMethod("active").invoke(editions),phase=season.getClass().getMethod("leaguePhase").invoke(season);verifyEuropeanDraw(phase,null,"ENG actual second-season draw");call("showEuropeanEdition",new Class[]{season.getClass(),boolean.class},season,false);});capture("europe-ENG-second-season-draw");}
            ui(()->{
                check(finished[0],country+" domestic and all three UEFA editions complete");Object europe=get("europeanCampaign");List<?> editions=(List<?>)europe.getClass().getMethod("competitions").invoke(europe);Map<String,String> completed=new HashMap<>();for(Object seasons:editions){Object season=seasons.getClass().getMethod("active").invoke(seasons),phase=season.getClass().getMethod("leaguePhase").invoke(season);check((Boolean)season.getClass().getMethod("complete").invoke(season),country+" UEFA final completed");completed.put(phase.getClass().getField("competition").get(phase).toString(),(String)season.getClass().getMethod("snapshot").invoke(season));}
                String dutchArchive=null;if(country.equals("NL")){Object playoff=get("dutchEuropeanPlayoff");check(playoff!=null&&(Boolean)playoff.getClass().getMethod("complete").invoke(playoff),"NL actual European ticket playoff completed");dutchArchive=(String)playoff.getClass().getMethod("snapshot").invoke(playoff);call("saveCurrentGame",new Class[0]);check((Boolean)call("loadSave",new Class[]{int.class},2),"NL completed European ticket reloads");check(dutchArchive.equals(get("dutchEuropeanPlayoff").getClass().getMethod("snapshot").invoke(get("dutchEuropeanPlayoff"))),"NL exact European playoff state retained");checkBackupReadable();call("showDutchEuropeanPlayoff",new Class[]{gameClass("DutchEuropeanPlayoff"),boolean.class},get("dutchEuropeanPlayoff"),false);}
                check((Boolean)call("preparePromotion",new Class[0]),country+" domestic sporting outcomes resolved");
                while(!(Boolean)get("promotion").getClass().getMethod("complete").invoke(get("promotion"))) {
                    java.time.LocalDate before=(java.time.LocalDate)get("currentDate");call("simulatePromotionLeg",new Class[0]);check(!((java.time.LocalDate)get("currentDate")).isBefore(before),country+" promotion dates never rewind");
                    call("saveCurrentGame",new Class[0]);String promotionDates=(String)get("promotionDates").getClass().getMethod("snapshot").invoke(get("promotionDates"));check((Boolean)call("loadSave",new Class[]{int.class},2),country+" dated promotion leg reloads");check(promotionDates.equals(get("promotionDates").getClass().getMethod("snapshot").invoke(get("promotionDates"))),country+" promotion dates retained exactly");checkBackupReadable();
                }if(country.equals("PT"))completePortuguesePostseason();call("continueDivisionSeason",new Class[0]);
                check(((java.time.LocalDate)get("careerSeasonStart")).getYear()==year+1,country+" annual admissions roll forward");Object next=get("europeanCampaign"),q=next.getClass().getMethod("qualifying").invoke(next);check(q!=null,country+" fresh qualifying campaign created");
                if(country.equals("NL")){Object admissions=q.getClass().getMethod("admissions").invoke(q),results=admissions.getClass().getMethod("domestic").invoke(admissions),playoff=results.getClass().getMethod("dutchPlayoff").invoke(results);check(dutchArchive.equals(playoff.getClass().getMethod("snapshot").invoke(playoff)),"NL annual qualification archives the actual playoff exactly");}
                for(Object seasons:(List<?>)next.getClass().getMethod("competitions").invoke(next)){Object season=seasons.getClass().getMethod("active").invoke(seasons),phase=season.getClass().getMethod("leaguePhase").invoke(season);check(completed.get(phase.getClass().getField("competition").get(phase).toString()).equals(season.getClass().getMethod("snapshot").invoke(season)),country+" completed UEFA edition preserved exactly before qualifying draw");}
                if(iteration==0){Object admissions=q.getClass().getMethod("admissions").invoke(q),world=get("division");int[] tiers=(int[])world.getClass().getField("clubTiers").get(world);String[] ids=(String[])world.getClass().getField("clubIds").get(world);int managed=-1;for(Object entry:(List<?>)admissions.getClass().getMethod("entries").invoke(admissions)){int club=entry.getClass().getField("club").getInt(entry);if(entry.getClass().getField("round").getInt(entry)<5&&tiers[club]==1&&ids[club].startsWith(country.toLowerCase(Locale.ROOT)+":")){managed=club;break;}}check(managed>=0,country+" local qualifier can be managed in the next edition");set("selectedClub",managed);call("initialiseTacticsForClub",new Class[0]);}
                call("saveCurrentGame",new Class[0]);check((Boolean)call("loadSave",new Class[]{int.class},2),country+" next annual campaign reloads");checkBackupReadable();call("showHistoryHub",new Class[0]);
            });capture("europe-"+country+"-history-"+year);
        }
        ui(()->{
            check(watched[0]>0&&quick[0]>0&&qualifying[0]>=2,country+" watched, quick and two-leg qualifying matches exercised");byte[] valid=backupBytes(prefs.getAll());Map<String,Object> broken=new HashMap<>(prefs.getAll());broken.put("save_2_european_campaign","broken");boolean rejected=false;try{gameClass("BackupRestore").getMethod("restore",SharedPreferences.class,byte[].class).invoke(null,prefs,backupBytes(broken));}catch(InvocationTargetException expected){rejected=true;}check(rejected&&Arrays.equals(valid,backupBytes(prefs.getAll())),country+" corrupt UEFA backup rejected atomically");gameClass("BackupRestore").getMethod("restore",SharedPreferences.class,byte[].class).invoke(null,prefs,original);check((Boolean)call("loadSave",new Class[]{int.class},0),"original career restored after UEFA test");
        });check(true,country+" combined UEFA qualifying domestic calendars two seasons watched quick matches exact archives and backups verified");
    }
    private void verifyGermanCupSeasons()throws Exception {
        SharedPreferences prefs=getTargetContext().getSharedPreferences("project_mister",Context.MODE_PRIVATE);
        byte[] original=backupBytes(prefs.getAll());final int[] age={0},managed={0};final boolean[] done={false},pendingChecked={false};
        ui(()->{
            Object catalog=get("catalog"),data=null;
            for(Object d:(List<?>)catalog.getClass().getField("divisions").get(catalog))if(d.getClass().getField("id").get(d).equals("de:1"))data=d;
            Object world=call("newCountryCareer",new Class[]{data.getClass()},data);
            call("configureDivision",new Class[]{gameClass("CareerDivision")},world);set("selectedSlot",2);set("selectedClub",0);
            ((Random)get("random")).setSeed(23671);
            call("initialiseNewManagerDefaults",new Class[0]);call("resetCareerState",new Class[0]);call("generatePlayers",new Class[0]);
            call("initialiseTacticsForClub",new Class[0]);call("initialiseClassicCareerSystems",new Class[0]);
            String[] clubs=(String[])get("clubNames");int local=0;for(String id:(String[])world.getClass().getField("clubIds").get(world))if(id.startsWith("de:"))local++;
            check(local==64&&((List<?>)get("players")).size()==clubs.length*20,"German career includes all 64 cup clubs, foreign clubs and their players");
            set("europeanCampaign",null); // Isolate domestic cups while retaining the new career's saved calendar and league decisions.
            check(get("nationalCups")!=null&&get("domesticCup")==null&&get("leagueCup")==null,"German career initializes its own cup");
            age[0]=playerInt(((List<?>)get("players")).get(0),"age");call("saveCurrentGame",new Class[0]);checkBackupReadable();
            call("showCalendarTab",new Class[]{String.class},"Cup");
        });
        capture("37-german-cup-draw");
        for(int edition=0;edition<2;edition++) {
            done[0]=false;final int year=2026+edition;
            for(int event=0;event<70&&!done[0];event++) {
                final int step=event;
                ui(()->{
                    java.time.LocalDate before=(java.time.LocalDate)get("currentDate");int round=(Integer)get("matchday");
                    call("initialiseTacticsForClub",new Class[0]);
                    if((Boolean)call("cupDue",new Class[0])) {
                        Object history=get("leagueResults");String ledger=(String)history.getClass().getMethod("snapshot").invoke(history);
                        if(!pendingChecked[0]) {
                            Object cup=get("nationalCups").getClass().getMethod("active").invoke(get("nationalCups"));
                            for(int i=0;i<(Integer)cup.getClass().getMethod("drawnCount").invoke(cup);i++) {
                                Object tie=cup.getClass().getMethod("at",int.class).invoke(cup,i);
                                if((Integer)tie.getClass().getField("firstHome").get(tie)==0||(Integer)tie.getClass().getField("firstAway").get(tie)==0)continue;
                                tie.getClass().getMethod("recordRegulation",int.class,int.class).invoke(tie,1,1);
                                tie.getClass().getMethod("recordExtraTime",int.class,int.class).invoke(tie,0,0);
                                call("saveCurrentGame",new Class[0]);check((Boolean)call("loadSave",new Class[]{int.class},2),"pending German shootout reloads");checkBackupReadable();
                                Object reloaded=get("nationalCups").getClass().getMethod("active").invoke(get("nationalCups"));
                                Object pending=reloaded.getClass().getMethod("at",int.class).invoke(reloaded,i);
                                check(pending.getClass().getMethod("phase").invoke(pending).toString().equals("PENALTIES"),"German shootout retained after reload");
                                pendingChecked[0]=true;break;
                            }
                        }
                        call("startLiveMatchday",new Class[0]);
                        if((Boolean)get("matchInProgress")) {
                            check((Boolean)get("liveCup")&&(Boolean)get("liveNationalCup"),"German cup enters watched match flow");set("livePaused",true);
                            set("liveHomeGoals",(Integer)get("liveHome")==0?3:0);set("liveAwayGoals",(Integer)get("liveAway")==0?3:0);
                            call("finishLiveMatch",new Class[0]);managed[0]++;
                        }
                        check(round==(Integer)get("matchday")&&ledger.equals(get("leagueResults").getClass().getMethod("snapshot").invoke(get("leagueResults"))),"German cup leaves league standings and round unchanged");
                    } else if(round<34)call("quickResult",new Class[0]);
                    check(!((java.time.LocalDate)get("currentDate")).isBefore(before),"German cup calendar never rewinds");
                    if(step%7==0||round>=34) {call("saveCurrentGame",new Class[0]);check((Boolean)call("loadSave",new Class[]{int.class},2),"German cup career reloads throughout season");checkBackupReadable();}
                    done[0]=(Integer)get("matchday")==34&&(Boolean)call("cupsComplete",new Class[0]);
                });
            }
            ui(()->{
                check(done[0],"German league and cup finish together");
                Object seasons=get("nationalCups"),cup=seasons.getClass().getMethod("active").invoke(seasons);
                check((Integer)cup.getClass().getMethod("drawnCount").invoke(cup)==63&&(Integer)cup.getClass().getMethod("winner").invoke(cup)==0,"German cup resolves all 63 ties and keeps managed champion");
                check(playerInt(((List<?>)get("players")).get(0),"age")==age[0]+year-2025,"German season ages players exactly once");
                check((Integer)get("careerWeeksProcessed")>0,"German calendar-week wages and recovery run through final");
                String saved=(String)cup.getClass().getMethod("snapshot").invoke(cup);completeProfessionalPostseason("DE");
                check(saved.equals(get("nationalCups").getClass().getMethod("active").invoke(get("nationalCups")).getClass().getMethod("snapshot").invoke(get("nationalCups").getClass().getMethod("active").invoke(get("nationalCups")))),"German completed cup remains exact through promotion playoffs");
                call("continueDivisionSeason",new Class[0]);
                check(((java.time.LocalDate)get("careerSeasonStart")).getYear()==year+1,"German season rolls forward after all sporting outcomes");
                Object next=get("nationalCups"),active=next.getClass().getMethod("active").invoke(next);List<?> archives=(List<?>)next.getClass().getMethod("archives").invoke(next);
                check(archives.size()==year-2025&&archives.get(archives.size()-1).equals(saved),"German completed edition archived without changes");
                check((Integer)active.getClass().getField("season").get(active)==year+1&&!(Boolean)active.getClass().getMethod("complete").invoke(active),"German rollover creates fresh playable draw");
                check((Boolean)call("loadSave",new Class[]{int.class},2),"German next season and trophy archives reload");checkBackupReadable();
            });
        }
        ui(()->{
            check(managed[0]==12&&pendingChecked[0],"two watched German cup runs and saved shootout verified");call("showHistoryHub",new Class[0]);
        });
        capture("38-german-cup-history");
        ui(()->{
            Object seasons=get("nationalCups");List<?> archives=(List<?>)seasons.getClass().getMethod("archives").invoke(seasons);
            call("showNationalCupRound",new Class[]{String.class,int.class,boolean.class},archives.get(0),5,true);
            Map<String,Object> invalid=new HashMap<>(prefs.getAll());invalid.put("save_2_national_cups","broken");
            byte[] corrupt=backupBytes(invalid);boolean rejected=false;
            try{gameClass("BackupRestore").getMethod("restore",SharedPreferences.class,byte[].class).invoke(null,prefs,corrupt);}catch(java.lang.reflect.InvocationTargetException expected){rejected=true;}
            check(rejected,"corrupt German cup backup rejected");checkBackupReadable();
        });
        capture("39-german-cup-archived-final");
        ui(()->{gameClass("BackupRestore").getMethod("restore",SharedPreferences.class,byte[].class).invoke(null,prefs,original);call("loadSave",new Class[]{int.class},0);});
        check(true,"German recurring cup live matches two seasons reloads history and backup rejection verified");
    }
    private void verifyExpandedCups(String country)throws Exception {
        SharedPreferences prefs=getTargetContext().getSharedPreferences("project_mister",Context.MODE_PRIVATE);
        byte[] original=backupBytes(prefs.getAll());final int[] age={0},watched={0},quick={0};final boolean[] done={false};
        ui(()->{
            Object catalog=get("catalog"),data=null;
            for(Object d:(List<?>)catalog.getClass().getField("divisions").get(catalog))if(d.getClass().getField("id").get(d).equals(country.toLowerCase(Locale.ROOT)+":1"))data=d;
            Object world=call("newCountryCareer",new Class[]{data.getClass()},data);call("configureDivision",new Class[]{gameClass("CareerDivision")},world);
            set("selectedSlot",2);set("selectedClub",0);((Random)get("random")).setSeed(23800+country.hashCode());
            call("initialiseNewManagerDefaults",new Class[0]);call("resetCareerState",new Class[0]);call("generatePlayers",new Class[0]);call("initialiseTacticsForClub",new Class[0]);call("initialiseClassicCareerSystems",new Class[0]);
            String[] ids=(String[])world.getClass().getField("clubIds").get(world);boolean foreign=false;for(String id:ids)if(!id.startsWith(country.toLowerCase(Locale.ROOT)+":"))foreign=true;
            check(foreign&&ids.length>100&&((List<?>)get("players")).size()==ids.length*20,country+" expanded world has real foreign and lower cup squads");
            check(get("nationalCups")!=null,country+" national cup initialized");check((get("scottishLeagueCup")!=null)==country.equals("SCO"),country+" correct Scottish League Cup initialization");
            set("europeanCampaign",null);
            age[0]=playerInt(((List<?>)get("players")).get(0),"age");call("saveCurrentGame",new Class[0]);checkBackupReadable();call("showCalendarTab",new Class[]{String.class},"Cup");
        });capture("cup-"+country+"-opening");
        for(int edition=0;edition<2;edition++) {
            final int year=2026+edition;done[0]=false;
            for(int event=0;event<160&&!done[0];event++) {
                final int step=event;
                ui(()->{
                    java.time.LocalDate before=(java.time.LocalDate)get("currentDate");int round=(Integer)get("matchday");call("initialiseTacticsForClub",new Class[0]);
                    if((Boolean)call("cupDue",new Class[0])) {
                        Object results=get("leagueResults");String ledger=(String)results.getClass().getMethod("snapshot").invoke(results);
                        if(year==2027&&quick[0]==0){int apps=0;for(int i=0;i<20;i++)apps+=playerInt(((List<?>)get("players")).get(i),"appearances");call("quickResult",new Class[0]);int after=0;for(int i=0;i<20;i++)after+=playerInt(((List<?>)get("players")).get(i),"appearances");if(after>apps)quick[0]++;}
                        else {
                            call("startLiveMatchday",new Class[0]);
                            if((Boolean)get("matchInProgress")){check((Boolean)get("liveCup"),country+" watched cup flow");set("livePaused",true);set("liveHomeGoals",(Integer)get("liveHome")==0?3:0);set("liveAwayGoals",(Integer)get("liveAway")==0?3:0);call("finishLiveMatch",new Class[0]);watched[0]++;}
                        }
                        check(round==(Integer)get("matchday")&&ledger.equals(get("leagueResults").getClass().getMethod("snapshot").invoke(get("leagueResults"))),country+" cup does not change league results");
                    } else if(round<(Integer)call("seasonRounds",new Class[0]))call("quickResult",new Class[0]);
                    check(!((java.time.LocalDate)get("currentDate")).isBefore(before),country+" date never rewinds");
                    if(step%9==0){call("saveCurrentGame",new Class[0]);check((Boolean)call("loadSave",new Class[]{int.class},2),country+" cup career reloads");checkBackupReadable();}
                    done[0]=(Integer)get("matchday")== (Integer)call("seasonRounds",new Class[0])&&(Boolean)call("cupsComplete",new Class[0]);
                });
            }
            ui(()->{
                check(done[0],country+" league and every cup completed");check(playerInt(((List<?>)get("players")).get(0),"age")==age[0]+year-2025,country+" ages players once per season");
                Object campaign=get("nationalCups");List<?> competitions=(List<?>)campaign.getClass().getMethod("competitions").invoke(campaign);Map<String,String> completed=new HashMap<>();
                for(Object seasons:competitions){Object cup=seasons.getClass().getMethod("active").invoke(seasons);check((Boolean)cup.getClass().getMethod("complete").invoke(cup),country+" national edition finished");completed.put((String)cup.getClass().getField("competition").get(cup),(String)cup.getClass().getMethod("snapshot").invoke(cup));}
                String scottish="";if(get("scottishLeagueCup")!=null){Object cup=get("scottishLeagueCup").getClass().getMethod("active").invoke(get("scottishLeagueCup"));check((Boolean)cup.getClass().getMethod("complete").invoke(cup),"Scottish groups and knockout finished");scottish=(String)cup.getClass().getMethod("snapshot").invoke(cup);}
                completeProfessionalPostseason(country);call("continueDivisionSeason",new Class[0]);check(((java.time.LocalDate)get("careerSeasonStart")).getYear()==year+1,country+" season rolls forward");
                Object next=get("nationalCups");List<?> histories=(List<?>)next.getClass().getMethod("competitions").invoke(next);
                for(Object seasons:histories){Object active=seasons.getClass().getMethod("active").invoke(seasons);List<?> archives=(List<?>)seasons.getClass().getMethod("archives").invoke(seasons);check(archives.size()==year-2025&&archives.get(archives.size()-1).equals(completed.get(active.getClass().getField("competition").get(active))),country+" cup archive remains exact");check(!(Boolean)active.getClass().getMethod("complete").invoke(active),country+" next cup draw playable");}
                if(!scottish.isEmpty()){Object nextScottish=get("scottishLeagueCup");List<?> archives=(List<?>)nextScottish.getClass().getMethod("archives").invoke(nextScottish);check(archives.size()==year-2025&&archives.get(archives.size()-1).equals(scottish),"Scottish League Cup exact archive");}
                check((Boolean)call("loadSave",new Class[]{int.class},2),country+" next season and archives reload");checkBackupReadable();
            });
        }
        ui(()->{check(watched[0]>0&&quick[0]>0,country+" watched and quick cup results exercised");call("showHistoryHub",new Class[0]);});capture("cup-"+country+"-history");
        ui(()->{
            byte[] valid=backupBytes(prefs.getAll());Map<String,Object> broken=new HashMap<>(prefs.getAll());broken.put("save_2_national_cups","broken");boolean rejected=false;
            try{gameClass("BackupRestore").getMethod("restore",SharedPreferences.class,byte[].class).invoke(null,prefs,backupBytes(broken));}catch(InvocationTargetException expected){rejected=true;}check(rejected&&Arrays.equals(valid,backupBytes(prefs.getAll())),country+" corrupt cup backup rejected atomically");
            if(country.equals("SCO")){broken=new HashMap<>(prefs.getAll());broken.put("save_2_scottish_league_cup","broken");rejected=false;try{gameClass("BackupRestore").getMethod("restore",SharedPreferences.class,byte[].class).invoke(null,prefs,backupBytes(broken));}catch(InvocationTargetException expected){rejected=true;}check(rejected&&Arrays.equals(valid,backupBytes(prefs.getAll())),"Scottish League Cup corruption preserves all careers");call("showCalendarTab",new Class[]{String.class},"League Cup");}
        });if(country.equals("SCO"))capture("cup-SCO-league-groups");
        ui(()->{gameClass("BackupRestore").getMethod("restore",SharedPreferences.class,byte[].class).invoke(null,prefs,original);check((Boolean)call("loadSave",new Class[]{int.class},0),"original career restored after "+country+" cups");});
        check(true,country+" recurring cups watched and quick matches two seasons reloads history and backup rejection verified");
    }
    private void verifyCupPlayers()throws Exception {
        SharedPreferences prefs=getTargetContext().getSharedPreferences("project_mister",Context.MODE_PRIVATE);byte[] original=backupBytes(prefs.getAll());
        final int[] dismissed={-1};final boolean[] sawBan={false};
        for(int scenario=0;scenario<2;scenario++) {
            final boolean transfer=scenario==0;
            ui(()->{
                Object data=null;for(Object d:(List<?>)get("catalog").getClass().getField("divisions").get(get("catalog")))if(d.getClass().getField("id").get(d).equals("de:1"))data=d;
                Object world=call("newCountryCareer",new Class[]{data.getClass()},data);call("configureDivision",new Class[]{gameClass("CareerDivision")},world);set("selectedSlot",2);set("selectedClub",0);((Random)get("random")).setSeed(24117);
                call("initialiseNewManagerDefaults",new Class[0]);call("resetCareerState",new Class[0]);set("europeanCampaign",null);call("generatePlayers",new Class[0]);call("initialiseTacticsForClub",new Class[0]);call("initialiseClassicCareerSystems",new Class[0]);call("saveCurrentGame",new Class[0]);
            });
            dismissed[0]=-1;sawBan[0]=false;final boolean[] complete={false};
            for(int step=0;step<120&&!complete[0];step++)ui(()->{
                for(Object p:(List<?>)get("players")){Field injury=p.getClass().getDeclaredField("injuredWeeks");injury.setAccessible(true);injury.setInt(p,0);}
                call("initialiseTacticsForClub",new Class[0]);
                if(dismissed[0]>=0&&(Boolean)call("cupDue",new Class[0])&&(Boolean)call("nextCupIsNational",new Class[0])) {
                    Object tie=call("nextCupTie",new Class[0]);int h=(Integer)tie.getClass().getMethod("home").invoke(tie),a=(Integer)tie.getClass().getMethod("away").invoke(tie);
                    if(h==0||a==0) {
                        int[] roles=(int[])get("playerRoleStatus"),slots=(int[])get("playerSelectedSlot");int slot=-1;
                        for(Object p:(List<?>)get("players"))if(playerInt(p,"team")==0&&roles[playerInt(p,"id")]==2){int id=playerInt(p,"id");slot=slots[id];roles[id]=0;slots[id]=-1;break;}
                        roles[dismissed[0]]=2;slots[dismissed[0]]=slot;
                        call("startLiveMatchday",new Class[0]);check(!(Boolean)get("matchInProgress"),"Actual cup blocks suspended selected starter");sawBan[0]=true;
                        call("initialiseTacticsForClub",new Class[0]);check(((int[])get("playerRoleStatus"))[dismissed[0]]!=2,"Assistant selects eligible replacement");
                    }
                }
                call("startLiveMatchday",new Class[0]);if(!(Boolean)get("matchInProgress"))return;set("livePaused",true);
                boolean cup=(Boolean)get("liveNationalCup");
                if(cup&&dismissed[0]<0) {
                    List<?> xi=(List<?>)get("liveXIIds");dismissed[0]=(Integer)xi.get(2);Object player=call("findPlayer",new Class[]{int.class},dismissed[0]);
                    call("recordLiveYellow",new Class[]{player.getClass()},player);call("recordLiveYellow",new Class[]{player.getClass()},player);
                    check(((Set<?>)get("matchDismissals")).contains(dismissed[0]),"Actual second yellow dismisses player");check(!((List<?>)get("liveHomeLineupIds")).contains(dismissed[0])&&!((List<?>)get("liveAwayLineupIds")).contains(dismissed[0]),"Dismissed player leaves pitch without repacking other slots");
                    check((Integer)get("liveSubsUsed")==0,"Sending-off consumes no substitution");
                } else if(cup&&dismissed[0]>=0) {
                    check(!((Set<?>)get("matchParticipants")).contains(dismissed[0]),"Suspended player is absent from actual cup appearances");complete[0]=true;
                }
                set("liveHomeGoals",(Integer)get("liveHome")==0?4:0);set("liveAwayGoals",(Integer)get("liveAway")==0?4:0);call("finishLiveMatch",new Class[0]);
                if(cup) {
                    String saved=(String)get("matchDiscipline").getClass().getMethod("snapshot").invoke(get("matchDiscipline")),participation=(String)get("cupEligibility").getClass().getMethod("snapshot").invoke(get("cupEligibility"));check((Boolean)call("loadSave",new Class[]{int.class},2),"Player disciplinary career reloads");check(saved.equals(get("matchDiscipline").getClass().getMethod("snapshot").invoke(get("matchDiscipline"))),"Player disciplinary state is exact after reload");check(participation.equals(get("cupEligibility").getClass().getMethod("snapshot").invoke(get("cupEligibility"))),"Actual participant and registration history is exact after reload");check(((Set<?>)get("cupEligibility").getClass().getMethod("playedClubs",int.class).invoke(get("cupEligibility"),dismissed[0])).contains(0),"Dismissed actual player retains original club appearance");checkBackupReadable();
                    if(transfer&&!complete[0]) {
                        set("selectedClub",1);set("currentTransferBudget",1000);set("wageBudgetK",1000000);Object player=call("findPlayer",new Class[]{int.class},dismissed[0]);Field ambition=player.getClass().getDeclaredField("ambition");ambition.setAccessible(true);ambition.setInt(player,10);((Random)get("random")).setSeed(1);
                        call("resolveContractOffer",new Class[]{player.getClass(),int.class,int.class,int.class,int.class,String.class},player,1,0,playerInt(player,"weeklyWageK")*10,3,"Star Player");check(playerInt(player,"team")==1,"Actual contract transfer changes club with stable player identity");check((Boolean)call("loadSave",new Class[]{int.class},2),"Transfer with pending cup ban reloads");
                        call("showPlayerProfile",new Class[]{int.class,int.class},dismissed[0],1);StringBuilder screen=new StringBuilder();collectScreenText(activity.getWindow().getDecorView(),screen);check(screen.toString().contains("suspended"),"Transferred player profile explains retained ban");checkBackupReadable();complete[0]=true;
                    }
                }
            });
            check(complete[0]&&(transfer||sawBan[0]),"Actual cup player scenario completes");capture(transfer?"cup-player-transfer-ban":"cup-player-ban-served");
            ui(()->{
                if(!transfer){List<?> descriptions=(List<?>)get("matchDiscipline").getClass().getMethod("playerSummary",int.class).invoke(get("matchDiscipline"),dismissed[0]);check(descriptions.stream().noneMatch(x->x.toString().contains("suspended")),"Eligible cup fixture serves pending ban exactly once");}
                Map<String,?> previous=new HashMap<>(prefs.getAll());Map<String,Object> broken=new HashMap<>(previous);broken.remove("save_2_match_discipline");boolean rejected=false;
                try{gameClass("BackupRestore").getMethod("restore",SharedPreferences.class,byte[].class).invoke(null,prefs,backupBytes(broken));}catch(InvocationTargetException expected){rejected=expected.getCause() instanceof IOException;}check(rejected&&prefs.getAll().equals(previous),"Missing disciplinary state rejected without replacing any career");
                broken=new HashMap<>(previous);broken.remove("save_2_cup_participation");rejected=false;
                try{gameClass("BackupRestore").getMethod("restore",SharedPreferences.class,byte[].class).invoke(null,prefs,backupBytes(broken));}catch(InvocationTargetException expected){rejected=expected.getCause() instanceof IOException;}check(rejected&&prefs.getAll().equals(previous),"Missing participation state rejected without replacing any career");
                gameClass("BackupRestore").getMethod("restore",SharedPreferences.class,byte[].class).invoke(null,prefs,original);check((Boolean)call("loadSave",new Class[]{int.class},0),"Original career restored after cup-player scenario");
            });
        }
        check(true,"actual cup dismissal, eligible lineup, ban service, transfer persistence and atomic backup rejection verified");
        verifyScottishCupTransfer();
        verifyFrenchCupContinuity();
    }
    private void verifyFrenchCupContinuity()throws Exception {
        SharedPreferences prefs=getTargetContext().getSharedPreferences("project_mister",Context.MODE_PRIVATE);byte[] original=backupBytes(prefs.getAll());final boolean[] tested={false};final ArrayList<Integer> fresh=new ArrayList<>(),heldXi=new ArrayList<>(),heldBench=new ArrayList<>();final java.time.LocalDate[] properDate={null};
        ui(()->{
            Object data=null;for(Object d:(List<?>)get("catalog").getClass().getField("divisions").get(get("catalog")))if(d.getClass().getField("id").get(d).equals("fr:1"))data=d;
            Object world=call("newCountryCareer",new Class[]{data.getClass()},data);call("configureDivision",new Class[]{gameClass("CareerDivision")},world);set("selectedSlot",2);set("selectedClub",0);((Random)get("random")).setSeed(27317);
            call("initialiseNewManagerDefaults",new Class[0]);call("resetCareerState",new Class[0]);set("europeanCampaign",null);call("generatePlayers",new Class[0]);call("initialiseTacticsForClub",new Class[0]);call("initialiseClassicCareerSystems",new Class[0]);call("saveCurrentGame",new Class[0]);
            Object cup=get("nationalCups").getClass().getMethod("active").invoke(get("nationalCups")),round=cup.getClass().getMethod("round",int.class).invoke(cup,2);properDate[0]=(java.time.LocalDate)round.getClass().getMethod("date",int.class).invoke(round,0);
        });
        for(int step=0;step<400&&!tested[0];step++)ui(()->{
            java.time.LocalDate next=(java.time.LocalDate)call("nextCareerEventDate",new Class[0]);if(next!=null&&next.isAfter((java.time.LocalDate)get("currentDate")))call("advanceCareerDate",new Class[]{java.time.LocalDate.class},next);
            for(Object p:(List<?>)get("players")){Field injury=p.getClass().getDeclaredField("injuredWeeks");injury.setAccessible(true);injury.setInt(p,0);}call("initialiseTacticsForClub",new Class[0]);
            java.time.LocalDate today=(java.time.LocalDate)get("currentDate");
            if(fresh.isEmpty()&&!today.isBefore(java.time.LocalDate.of(2026,12,1))&&!today.isAfter(properDate[0].minusDays(2))) {
                int[] roles=(int[])get("playerRoleStatus");for(Object p:(List<?>)get("players"))if(playerInt(p,"team")==0){int id=playerInt(p,"id");if(roles[id]==2)heldXi.add(id);else if(roles[id]==1)heldBench.add(id);}
                set("currentTransferBudget",10000);set("wageBudgetK",1000000);for(int slot:new int[]{40,42,43,44,45,46,48,49,50,52,57}){int id=(Integer)call("signMarketPlayer",new Class[]{String.class},"free~0~"+slot);check(id>=0,"Actual French free agent signs at least two days before the cup fixture");fresh.add(id);check(today.plusDays(2).equals(get("cupEligibility").getClass().getMethod("qualifiedOn",int.class).invoke(get("cupEligibility"),id)),"Actual French signing saves two-day qualification delay");}
            }
            if(!fresh.isEmpty()){int[] roles=(int[])get("playerRoleStatus"),slots=(int[])get("playerSelectedSlot");Arrays.fill(roles,0);Arrays.fill(slots,-1);for(int id:heldXi)roles[id]=2;for(int id:heldBench)roles[id]=1;}
            if((Boolean)call("cupDue",new Class[0])&&(Boolean)call("nextCupIsNational",new Class[0])) {
                Object tie=call("nextCupTie",new Class[0]),fixture=call("cupDisciplineFixture",new Class[0]);int home=(Integer)tie.getClass().getMethod("home").invoke(tie),away=(Integer)tie.getClass().getMethod("away").invoke(tie),phase=fixture.getClass().getField("phase").getInt(fixture);
                if(phase>=9&&(home==0||away==0)) {
                    Set<?> previous=(Set<?>)get("cupEligibility").getClass().getMethod("recentParticipants",int.class).invoke(get("cupEligibility"),0);check(previous.size()>=7,"French actual recent first-team participants exist before competition proper");ArrayList<Integer> oldBench=new ArrayList<>();for(Object value:previous)if(oldBench.size()<7)oldBench.add((Integer)value);
                    check(fresh.size()==11,"Eleven genuine signings completed before the French match date");for(int id:fresh){Object p=call("findPlayer",new Class[]{int.class},id);check(((String)call("cupPlayerReason",new Class[]{fixture.getClass(),p.getClass()},fixture,p)).isEmpty(),"Two-day qualification is complete before testing continuity");check(!previous.contains(id),"Unplayed new signing is not invented in previous match history");}
                    int[] roles=(int[])get("playerRoleStatus"),slots=(int[])get("playerSelectedSlot");Arrays.fill(roles,0);Arrays.fill(slots,-1);for(int id:fresh)roles[id]=2;
                    call("startLiveMatchday",new Class[0]);check(!(Boolean)get("matchInProgress"),"Actual Coupe de France rejects eleven entirely new players without seven recent participants");for(int id:oldBench)roles[id]=1;
                    call("startLiveMatchday",new Class[0]);check((Boolean)get("matchInProgress")&&(Boolean)get("liveNationalCup"),"Seven genuine recent participants on the bench allow actual French cup match");set("livePaused",true);Set<?> participants=(Set<?>)get("matchParticipants");check(participants.containsAll(fresh),"All eleven newly registered starters actually participate");for(int id:oldBench)check(!participants.contains(id),"Unused continuity bench does not receive a new appearance");
                    set("liveHomeGoals",home==0?4:0);set("liveAwayGoals",away==0?4:0);call("finishLiveMatch",new Class[0]);String saved=(String)get("cupEligibility").getClass().getMethod("snapshot").invoke(get("cupEligibility"));check((Boolean)call("loadSave",new Class[]{int.class},2),"French continuity result and transfers reload");check(saved.equals(get("cupEligibility").getClass().getMethod("snapshot").invoke(get("cupEligibility"))),"French actual continuity and transfer records reload exactly");Set<?> after=(Set<?>)get("cupEligibility").getClass().getMethod("recentParticipants",int.class).invoke(get("cupEligibility"),0);check(after.containsAll(fresh),"New actual cup participants enter recent first-team history");checkBackupReadable();tested[0]=true;return;
                }
            }
            call("startLiveMatchday",new Class[0]);if(!(Boolean)get("matchInProgress"))return;set("livePaused",true);set("liveHomeGoals",(Integer)get("liveHome")==0?4:0);set("liveAwayGoals",(Integer)get("liveAway")==0?4:0);call("finishLiveMatch",new Class[0]);
        });
        check(tested[0],"Actual French continuity scenario completes");capture("cup-player-french-continuity-replacement");ui(()->{gameClass("BackupRestore").getMethod("restore",SharedPreferences.class,byte[].class).invoke(null,prefs,original);check((Boolean)call("loadSave",new Class[]{int.class},0),"Original careers restored after French continuity scenario");});check(true,"French actual cup match-sheet continuity, free-agent registrations, unused bench distinction and exact saves verified");
    }
    private void verifyScottishCupTransfer()throws Exception {
        SharedPreferences prefs=getTargetContext().getSharedPreferences("project_mister",Context.MODE_PRIVATE);byte[] original=backupBytes(prefs.getAll());final int[] moved={-1};final boolean[] tested={false};
        final boolean[] goalkeeperTested={false};final ArrayList<Integer> promoted=new ArrayList<>();
        ui(()->{
            Object data=null;for(Object d:(List<?>)get("catalog").getClass().getField("divisions").get(get("catalog")))if(d.getClass().getField("id").get(d).equals("sco:1"))data=d;
            check(data!=null,"Scottish player-rule career is available");Object world=call("newCountryCareer",new Class[]{data.getClass()},data);call("configureDivision",new Class[]{gameClass("CareerDivision")},world);set("selectedSlot",2);set("selectedClub",0);((Random)get("random")).setSeed(27311);
            call("initialiseNewManagerDefaults",new Class[0]);call("resetCareerState",new Class[0]);set("europeanCampaign",null);call("generatePlayers",new Class[0]);call("initialiseTacticsForClub",new Class[0]);call("initialiseClassicCareerSystems",new Class[0]);call("saveCurrentGame",new Class[0]);
        });
        for(int step=0;step<400&&!tested[0];step++)ui(()->{
            java.time.LocalDate next=(java.time.LocalDate)call("nextCareerEventDate",new Class[0]);if(next!=null&&next.isAfter((java.time.LocalDate)get("currentDate")))call("advanceCareerDate",new Class[]{java.time.LocalDate.class},next);
            for(Object p:(List<?>)get("players")){Field injury=p.getClass().getDeclaredField("injuredWeeks");injury.setAccessible(true);injury.setInt(p,0);}call("initialiseTacticsForClub",new Class[0]);
            if(!goalkeeperTested[0]&&promoted.isEmpty()&&(Boolean)call("cupDue",new Class[0])&&(Boolean)call("nextCupIsNational",new Class[0])) {
                Object tie=call("nextCupTie",new Class[0]);int club=(Integer)get("selectedClub"),home=(Integer)tie.getClass().getMethod("home").invoke(tie),away=(Integer)tie.getClass().getMethod("away").invoke(tie);
                if(home==club||away==club) {
                    ArrayList<Integer> injured=new ArrayList<>();int before=((List<?>)get("players")).size();
                    for(Object p:(List<?>)get("players"))if(playerInt(p,"team")==club&&playerString(p,"position").equals("GK")){Field injury=p.getClass().getDeclaredField("injuredWeeks");injury.setAccessible(true);injury.setInt(p,2);injured.add(playerInt(p,"id"));}
                    call("initialiseTacticsForClub",new Class[0]);int[] roles=(int[])get("playerRoleStatus");int starters=0,reserves=0;
                    for(Object p:(List<?>)get("players"))if(playerInt(p,"team")==club&&playerInt(p,"id")>=before){int id=playerInt(p,"id");check(playerString(p,"position").equals("GK"),"Unavailable Scottish keepers produce genuine goalkeeper roster identities");promoted.add(id);if(roles[id]==2)starters++;if(roles[id]==1)reserves++;}
                    check(promoted.size()==2&&starters==1&&reserves==1,"Scottish Cup assistant selects a fit starting and reserve youth goalkeeper");for(int id:injured)check(playerInt(call("findPlayer",new Class[]{int.class},id),"injuredWeeks")==2,"Keeper promotion preserves the original player's injury");
                    String roster=(String)get("rosterExtensions").getClass().getMethod("snapshot").invoke(get("rosterExtensions"));call("initialiseTacticsForClub",new Class[0]);check(roster.equals(get("rosterExtensions").getClass().getMethod("snapshot").invoke(get("rosterExtensions"))),"Repeated cup selection does not duplicate goalkeeper promotions");
                }
            }
            if(moved[0]>=0&&(Boolean)call("cupDue",new Class[0])&&(Boolean)call("nextCupIsNational",new Class[0])) {
                Object tie=call("nextCupTie",new Class[0]);int home=(Integer)tie.getClass().getMethod("home").invoke(tie),away=(Integer)tie.getClass().getMethod("away").invoke(tie),club=(Integer)get("selectedClub");
                if(home==club||away==club) {
                    Object p=call("findPlayer",new Class[]{int.class},moved[0]),fixture=call("cupDisciplineFixture",new Class[0]);String reason=(String)call("cupPlayerReason",new Class[]{fixture.getClass(),p.getClass()},fixture,p);
                    check(reason.contains("another club"),"Actual Scottish Cup fixture explains the transferred player's restriction");int[] roles=(int[])get("playerRoleStatus"),slots=(int[])get("playerSelectedSlot");
                    check(roles[moved[0]]!=2,"Assistant excludes cup-tied transfer");for(Object teammate:(List<?>)get("players"))if(playerInt(teammate,"team")==club&&roles[playerInt(teammate,"id")]==2){int id=playerInt(teammate,"id");roles[id]=0;slots[id]=-1;break;}roles[moved[0]]=2;
                    call("startLiveMatchday",new Class[0]);check(!(Boolean)get("matchInProgress"),"Actual watched Scottish Cup rejects manually selected cup-tied transfer");call("initialiseTacticsForClub",new Class[0]);call("startLiveMatchday",new Class[0]);check((Boolean)get("matchInProgress")&&(Boolean)get("liveNationalCup"),"Eligible replacement allows actual Scottish Cup progression");set("livePaused",true);check(!((Set<?>)get("matchParticipants")).contains(moved[0]),"Cup-tied footballer has no actual new-club cup appearance");set("liveHomeGoals",home==club?4:0);set("liveAwayGoals",away==club?4:0);call("finishLiveMatch",new Class[0]);
                    verifyScottishKeeperResult(club,promoted,goalkeeperTested);
                    String saved=(String)get("cupEligibility").getClass().getMethod("snapshot").invoke(get("cupEligibility"));check((Boolean)call("loadSave",new Class[]{int.class},2),"Scottish player-rule career reloads after actual result");check(saved.equals(get("cupEligibility").getClass().getMethod("snapshot").invoke(get("cupEligibility"))),"Cup-tied transfer and actual eligible result reload exactly");checkBackupReadable();tested[0]=true;return;
                }
            }
            call("startLiveMatchday",new Class[0]);if(!(Boolean)get("matchInProgress"))return;set("livePaused",true);boolean ownCup=(Boolean)get("liveNationalCup");int club=(Integer)get("selectedClub"),candidate=-1;
            if(ownCup&&moved[0]<0)for(Object id:(List<?>)get("liveXIIds")){Object p=call("findPlayer",new Class[]{int.class},(Integer)id);Field position=p.getClass().getDeclaredField("position");position.setAccessible(true);if(!position.get(p).equals("GK")){candidate=(Integer)id;break;}}
            set("liveHomeGoals",(Integer)get("liveHome")==club?4:0);set("liveAwayGoals",(Integer)get("liveAway")==club?4:0);call("finishLiveMatch",new Class[0]);
            if(ownCup)verifyScottishKeeperResult(club,promoted,goalkeeperTested);
            if(candidate>=0) {
                Object cup=get("nationalCups").getClass().getMethod("active").invoke(get("nationalCups")),world=get("division");int drawn=(Integer)cup.getClass().getMethod("drawnCount").invoke(cup),destination=-1;
                for(int index=0;index<drawn&&destination<0;index++){Object tie=cup.getClass().getMethod("at",int.class).invoke(cup,index);if((Integer)tie.getClass().getMethod("winner").invoke(tie)<0)for(String side:new String[]{"home","away"}){int team=(Integer)tie.getClass().getMethod(side).invoke(tie);if(team!=club&&(Boolean)world.getClass().getMethod("contains",int.class).invoke(world,team)){destination=team;break;}}}
                check(destination>=0,"Another playable Scottish top-division club has an actual remaining cup tie");Object p=call("findPlayer",new Class[]{int.class},candidate);set("selectedClub",destination);set("currentTransferBudget",1000);set("wageBudgetK",1000000);Field ambition=p.getClass().getDeclaredField("ambition");ambition.setAccessible(true);ambition.setInt(p,10);((Random)get("random")).setSeed(1);
                call("resolveContractOffer",new Class[]{p.getClass(),int.class,int.class,int.class,int.class,String.class},p,1,club,playerInt(p,"weeklyWageK")*10,3,"Star Player");check(playerInt(p,"team")==destination,"Actual Scottish contract moves footballer with unchanged identity");check((Boolean)call("loadSave",new Class[]{int.class},2),"Actual Scottish transfer registration reloads");moved[0]=candidate;
            }
        });
        check(tested[0]&&goalkeeperTested[0],"Scottish actual cup transfer and goalkeeper scenarios complete");capture("cup-player-scottish-transfer-restriction");ui(()->{gameClass("BackupRestore").getMethod("restore",SharedPreferences.class,byte[].class).invoke(null,prefs,original);check((Boolean)call("loadSave",new Class[]{int.class},0),"Original careers retained after Scottish eligibility scenario");});check(true,"Scottish Cup transfer restriction blocks an actual ineligible lineup and preserves playable progression and exact saves");
    }
    private void verifyScottishKeeperResult(int club,ArrayList<Integer> promoted,boolean[] checked)throws Exception {
        if(checked[0]||promoted.isEmpty())return;
        int appearances=0;
        for(int id:promoted)if(((Set<?>)get("cupEligibility").getClass().getMethod("playedClubs",int.class).invoke(get("cupEligibility"),id)).contains(club))appearances++;
        check(appearances==1,"Only the youth goalkeeper who actually played receives a cup appearance");
        String roster=(String)get("rosterExtensions").getClass().getMethod("snapshot").invoke(get("rosterExtensions"));
        String training=(String)get("trainingHistory").getClass().getMethod("snapshot").invoke(get("trainingHistory"));
        String participation=(String)get("cupEligibility").getClass().getMethod("snapshot").invoke(get("cupEligibility"));
        check((Boolean)call("loadSave",new Class[]{int.class},2),"Scottish goalkeeper replacements reload after an actual cup result");
        check(roster.equals(get("rosterExtensions").getClass().getMethod("snapshot").invoke(get("rosterExtensions"))),"Actual goalkeeper promotions retain ordered identities after reload");
        check(training.equals(get("trainingHistory").getClass().getMethod("snapshot").invoke(get("trainingHistory"))),"Promoted goalkeeper training histories reload exactly");
        check(participation.equals(get("cupEligibility").getClass().getMethod("snapshot").invoke(get("cupEligibility"))),"Starting and unused reserve goalkeeper appearances reload exactly");
        for(int id:promoted)check(playerInt(call("findPlayer",new Class[]{int.class},id),"team")==club,"Promoted goalkeeper keeps the original club and player ID");
        checkBackupReadable();checked[0]=true;
    }
    private void completeProfessionalPostseason(String country)throws Exception {
        check((Boolean)call("preparePromotion",new Class[0]),country+" final standings resolve required promotion field");
        String ledger=(String)get("leagueResults").getClass().getMethod("snapshot").invoke(get("leagueResults"));
        for(int leg=0;leg<32&&!(Boolean)get("promotion").getClass().getMethod("complete").invoke(get("promotion"));leg++) {
            java.time.LocalDate before=(java.time.LocalDate)get("currentDate");call("simulatePromotionLeg",new Class[0]);
            check(!((java.time.LocalDate)get("currentDate")).isBefore(before),country+" isolated domestic promotion calendar never rewinds");
            call("saveCurrentGame",new Class[0]);String saved=(String)get("promotion").getClass().getMethod("snapshot").invoke(get("promotion"));
            String dates=(String)get("promotionDates").getClass().getMethod("snapshot").invoke(get("promotionDates"));
            check((Boolean)call("loadSave",new Class[]{int.class},(Integer)get("selectedSlot")),country+" domestic promotion result reloads");
            check(saved.equals(get("promotion").getClass().getMethod("snapshot").invoke(get("promotion")))&&dates.equals(get("promotionDates").getClass().getMethod("snapshot").invoke(get("promotionDates"))),country+" domestic promotion result and dates retained exactly");checkBackupReadable();
        }
        check((Boolean)get("promotion").getClass().getMethod("complete").invoke(get("promotion")),country+" promotion completes before annual cup archive");
        check(ledger.equals(get("leagueResults").getClass().getMethod("snapshot").invoke(get("leagueResults"))),country+" promotion preserves regular league ledger");
    }
    private void completePortuguesePostseason()throws Exception {
        check((Boolean)call("preparePromotion",new Class[0]),"Portuguese upper promotion field resolved");Object upper=get("promotion");while(!(Boolean)upper.getClass().getMethod("complete").invoke(upper))call("simulatePromotionLeg",new Class[0]);
        Object fourth=get("portugueseFourth"),activeFourth=fourth.getClass().getMethod("active").invoke(fourth);
        java.time.LocalDate lowerFinal=(java.time.LocalDate)activeFourth.getClass().getMethod("finalDate").invoke(activeFourth),current=(java.time.LocalDate)get("currentDate");
        call("advanceCareerDate",new Class[]{java.time.LocalDate.class},lowerFinal.isBefore(current)?current:lowerFinal);
        check((Boolean)call("lowerLeaguesComplete",new Class[0]),"Portuguese lower qualification results complete before barrage");
        java.time.LocalDate before=(java.time.LocalDate)get("currentDate");
        check((Boolean)call("prepareLowerPromotion",new Class[0]),"Liga 3 merit and Liga 2 bottom places determine barrage");call("advanceLowerPromotion",new Class[]{boolean.class},false);check(!(Boolean)get("matchInProgress"),"managed club has no outstanding lower barrage");Object lower=get("lowerPromotion");check((Boolean)lower.getClass().getMethod("complete").invoke(lower),"saved Liga 2 / Liga 3 barrage completed");
        check(!((java.time.LocalDate)get("currentDate")).isBefore(before),"Portuguese lower barrage never rewinds the promotion calendar");
        if((Integer)get("fixtureVersion")>=6){Object dates=get("lowerPromotionDates");check(dates!=null&&((List<?>)dates.getClass().getMethod("fixtures").invoke(dates)).size()==2,"Both lower barrage legs have saved dates");String saved=(String)dates.getClass().getMethod("snapshot").invoke(dates);call("saveCurrentGame",new Class[0]);check((Boolean)call("loadSave",new Class[]{int.class},(Integer)get("selectedSlot")),"Dated Portuguese lower barrage reloads");check(saved.equals(get("lowerPromotionDates").getClass().getMethod("snapshot").invoke(get("lowerPromotionDates"))),"Exact Portuguese lower barrage dates retained");checkBackupReadable();}
    }
    private void verifyDomesticCupSeason()throws Exception {
        SharedPreferences prefs=getTargetContext().getSharedPreferences("project_mister",Context.MODE_PRIVATE);
        byte[] original=backupBytes(prefs.getAll());final int[] initialAge={0};
        ui(()->{
            Object catalog=get("catalog"),data=null;
            for(Object d:(List<?>)catalog.getClass().getField("divisions").get(catalog))if(d.getClass().getField("id").get(d).equals("pt:1"))data=d;
            Object world=call("newCountryCareer",new Class[]{data.getClass()},data);
            call("configureDivision",new Class[]{gameClass("CareerDivision")},world);set("selectedSlot",2);set("selectedClub",0);
            call("initialiseNewManagerDefaults",new Class[0]);call("resetCareerState",new Class[0]);call("generatePlayers",new Class[0]);
            call("initialiseTacticsForClub",new Class[0]);call("initialiseClassicCareerSystems",new Class[0]);
            ((Random)get("random")).setSeed(73451);
            check(((String[])get("clubNames")).length>483&&((List<?>)get("players")).size()==((String[])get("clubNames")).length*20,"expanded Portuguese world retains local and foreign clubs and their players");
            set("europeanCampaign",null);set("fixtureVersion",3);
            check(get("domesticCup")!=null&&get("leagueCup")!=null&&get("portugueseThird")!=null&&get("portugueseFourth")!=null&&get("portugueseDistrict")!=null,"Portuguese cups and lower seasons initialized");
            initialAge[0]=playerInt(((List<?>)get("players")).get(0),"age");
            call("saveCurrentGame",new Class[0]);checkBackupReadable();
        });
        final boolean[] done={false};final int[] cupMatches={0},quickMatches={0};
        for(int event=0;event<90&&!done[0];event++) {
            final int step=event;
            ui(()->{
                java.time.LocalDate before=(java.time.LocalDate)get("currentDate");
                call("initialiseTacticsForClub",new Class[0]);
                int round=(Integer)get("matchday");boolean cup=(Boolean)call("cupDue",new Class[0]);
                if(cup) {
                    Object history=get("leagueResults");String ledger=(String)history.getClass().getMethod("snapshot").invoke(history);
                    call("startLiveMatchday",new Class[0]);
                    if((Boolean)get("matchInProgress")) {
                        check((Boolean)get("liveCup"),"cup window starts cup match");set("livePaused",true);
                        set("liveHomeGoals",(Integer)get("liveHome")==0?3:0);set("liveAwayGoals",(Integer)get("liveAway")==0?3:0);
                        call("finishLiveMatch",new Class[0]);cupMatches[0]++;
                    }
                    check(round==(Integer)get("matchday")&&ledger.equals(history.getClass().getMethod("snapshot").invoke(history)),"cup leaves league round and results unchanged");
                } else if(round<34) {
                    call("quickResult",new Class[0]);
                    check((Integer)get("matchday")==round+1&&!(Boolean)get("matchInProgress"),"quick result completes exactly one league match");
                    int goals=0;for(Object value:((Map<?,?>)get("matchGoals")).values())goals+=(Integer)value;
                    check(goals==(Integer)get("liveHomeGoals")+(Integer)get("liveAwayGoals"),"quick-result goals credited to players");quickMatches[0]++;
                }
                check(!((java.time.LocalDate)get("currentDate")).isBefore(before),"career calendar never rewinds");
                if(step%7==0||round>=34) {
                    call("saveCurrentGame",new Class[0]);int weeks=(Integer)get("postseasonWeeksProcessed"),balance=(Integer)get("financeBalanceK");
                    String development=(String)call("encodePlayerField",new Class[]{String.class},"age")+call("encodePlayerField",new Class[]{String.class},"ability")+call("encodePlayerField",new Class[]{String.class},"potential")+call("encodePlayerField",new Class[]{String.class},"value");
                    check((Boolean)call("loadSave",new Class[]{int.class},2),"expanded career reloads throughout cup season");checkBackupReadable();
                    check(development.equals((String)call("encodePlayerField",new Class[]{String.class},"age")+call("encodePlayerField",new Class[]{String.class},"ability")+call("encodePlayerField",new Class[]{String.class},"potential")+call("encodePlayerField",new Class[]{String.class},"value")),"age development and value survive reload");
                    check(weeks==(Integer)get("postseasonWeeksProcessed")&&balance==(Integer)get("financeBalanceK"),"reload does not repeat postseason wages");
                }
                done[0]=(Integer)get("matchday")==34&&(Boolean)call("cupsComplete",new Class[0]);
            });
        }
        ui(()->{
            check(done[0]&&quickMatches[0]==34&&cupMatches[0]>=8,"full league plus both managed cup finals completed");
            Object cup=get("domesticCup");check((Integer)cup.getClass().getMethod("drawnCount").invoke(cup)==145,"all 145 domestic cup ties resolved");
            check((Integer)cup.getClass().getMethod("winner").invoke(cup)==0,"managed domestic cup champion saved");
            check((Boolean)get("seasonAgeApplied")&&playerInt(((List<?>)get("players")).get(0),"age")==initialAge[0]+1,"season ageing runs once after final cup");
            check((Integer)get("postseasonWeeksProcessed")>0,"postseason recovery and wages advance through May");
            call("showCalendarTab",new Class[]{String.class},"Cup");
        });
        capture("35-domestic-cup-winners");
        ui(()->{
            String cup=(String)get("domesticCup").getClass().getMethod("snapshot").invoke(get("domesticCup"));
            completePortuguesePostseason();call("continueDivisionSeason",new Class[0]);
            check(((java.time.LocalDate)get("careerSeasonStart")).getYear()==2027&&(Integer)get("matchday")==0,"completed cup season advances to next year");
            Object archives=get("domesticCupHistory");List<?> editions=(List<?>)archives.getClass().getMethod("editions").invoke(archives);
            check(editions.size()==1&&cup.equals(editions.get(0)),"completed cup archive survives next season");
            check((Integer)get("domesticCup").getClass().getField("season").get(get("domesticCup"))==2027&&!(Boolean)get("domesticCup").getClass().getMethod("complete").invoke(get("domesticCup")),"fresh playable cup in second season");
            check((Boolean)call("loadSave",new Class[]{int.class},2),"next season with expanded world reloads");checkBackupReadable();
            Map<String,Object> invalid=new HashMap<>(prefs.getAll());invalid.put("save_2_postseason_weeks",-1);
            boolean rejected=false;try{gameClass("BackupRestore").getMethod("validate",byte[].class).invoke(null,(Object)backupBytes(invalid));}catch(java.lang.reflect.InvocationTargetException expected){rejected=true;}
            check(rejected,"invalid postseason counter rejected");
        });
        done[0]=false;
        for(int event=0;event<90&&!done[0];event++) {
            final int step=event;
            ui(()->{
                java.time.LocalDate before=(java.time.LocalDate)get("currentDate");call("initialiseTacticsForClub",new Class[0]);
                if((Boolean)call("cupDue",new Class[0])) {
                    int round=(Integer)get("matchday");call("startLiveMatchday",new Class[0]);
                    if((Boolean)get("matchInProgress")) {
                        check((Boolean)get("liveCup"),"second-year cup starts live match");set("livePaused",true);
                        set("liveHomeGoals",(Integer)get("liveHome")==0?3:0);set("liveAwayGoals",(Integer)get("liveAway")==0?3:0);
                        call("finishLiveMatch",new Class[0]);
                    }
                    check(round==(Integer)get("matchday"),"recurring cup does not advance league round");
                } else if((Integer)get("matchday")<34)call("quickResult",new Class[0]);
                check(!((java.time.LocalDate)get("currentDate")).isBefore(before),"second-year calendar never rewinds");
                if(step%9==0){call("saveCurrentGame",new Class[0]);check((Boolean)call("loadSave",new Class[]{int.class},2),"second-year cup reloads");checkBackupReadable();}
                done[0]=(Integer)get("matchday")==34&&(Boolean)call("cupsComplete",new Class[0]);
            });
        }
        ui(()->{
            check(done[0],"second full league and recurring cup completed");
            check(playerInt(((List<?>)get("players")).get(0),"age")==initialAge[0]+2,"exactly two annual ageing passes");
            Object cup=get("domesticCup");check((Integer)cup.getClass().getMethod("winner").invoke(cup)==0,"managed club wins recurring cup");
            completePortuguesePostseason();call("continueDivisionSeason",new Class[0]);
            check(((java.time.LocalDate)get("careerSeasonStart")).getYear()==2028,"second rollover creates third season");
            Object archives=get("domesticCupHistory");check(((List<?>)archives.getClass().getMethod("editions").invoke(archives)).size()==2,"two distinct trophy archives retained");
            check((Boolean)call("loadSave",new Class[]{int.class},2),"third-season draw and history reload");checkBackupReadable();
            call("showHistoryHub",new Class[0]);
        });
        capture("36-recurring-cup-history");
        ui(()->{
            Object world=get("division"),type=get("modernPortugueseCup");Object active=type.getClass().getMethod("active").invoke(type);int[] direct=(int[])active.getClass().getMethod("direct").invoke(active);
            String[] identities=(String[])world.getClass().getField("clubIds").get(world);int[] tiers=(int[])world.getClass().getField("clubTiers").get(world);boolean[] reserves=(boolean[])world.getClass().getField("reserves").get(world);int club=-1;Set<Integer> excluded=new HashSet<>();for(int c:direct)excluded.add(c);
            int[] levels=new int[tiers.length];for(int c=0;c<tiers.length;c++){levels[c]=(Integer)world.getClass().getMethod("level",int.class).invoke(world,c);if(club<0&&tiers[c]==2&&!reserves[c]&&!excluded.contains(c))club=c;}
            check(club>=0,"available non-European Liga 2 club for new-format league phase");Object changed=world.getClass().getMethod("withLocalLevels",int[].class,int.class).invoke(world,levels,club);set("division",changed);set("selectedClub",club);call("initialiseTacticsForClub",new Class[0]);call("resetFinanceStadiumState",new Class[0]);call("saveCurrentGame",new Class[0]);checkBackupReadable();
            check((Boolean)call("loadSave",new Class[]{int.class},2),"new-format Liga 2 manager saves and reloads");call("showCalendarTab",new Class[]{String.class},"League Cup");
        });
        capture("40-portuguese-new-league-cup");
        final int[] leaguePhase={0},quickPhase={0};done[0]=false;
        for(int event=0;event<100&&!done[0];event++){
            final int step=event;
            ui(()->{
                java.time.LocalDate before=(java.time.LocalDate)get("currentDate");call("initialiseTacticsForClub",new Class[0]);int round=(Integer)get("matchday");boolean due=(Boolean)call("cupDue",new Class[0]);
                if(due){
                    // Continue can simulate earlier foreign cup fixtures before opening the
                    // manager's match. Inspect the active fixture AFTER that transition.
                    call("startLiveMatchday",new Class[0]);
                    if((Boolean)get("matchInProgress")) {
                        check((Boolean)get("liveCup"),"future cup starts live match");set("livePaused",true);
                        int manager=(Integer)get("selectedClub");
                        Object active=get("modernPortugueseCup").getClass().getMethod("active").invoke(get("modernPortugueseCup"));
                        Object f=active.getClass().getMethod("current").invoke(active);
                        boolean phase=(Boolean)get("liveModernPortugueseCup")&&f!=null&&(Boolean)f.getClass().getField("league").get(f);
                        if(phase) {
                            check(((Integer)f.getClass().getField("home").get(f)==manager||(Integer)f.getClass().getField("away").get(f)==manager),"counted league-phase fixture contains manager");
                            leaguePhase[0]++;
                        }
                        if(phase&&quickPhase[0]==0) {
                            int apps=appearancesForTeam(manager);call("completeQuickResult",new Class[0]);
                            check(appearancesForTeam(manager)>=apps+11,"quick new-format league-phase appearances credited");quickPhase[0]++;
                        } else {
                            set("liveHomeGoals",(Integer)get("liveHome")==manager?3:0);set("liveAwayGoals",(Integer)get("liveAway")==manager?3:0);call("finishLiveMatch",new Class[0]);
                        }
                    }
                    check(round==(Integer)get("matchday"),"future cup leaves league round unchanged");
                }else if(round<34){call("startLiveMatchday",new Class[0]);check((Boolean)get("matchInProgress"),"future league playable");set("livePaused",true);int manager=(Integer)get("selectedClub");set("liveHomeGoals",(Integer)get("liveHome")==manager?3:0);set("liveAwayGoals",(Integer)get("liveAway")==manager?3:0);call("finishLiveMatch",new Class[0]);}
                check(!((java.time.LocalDate)get("currentDate")).isBefore(before),"future Portuguese calendar never rewinds");if(step%9==0){call("saveCurrentGame",new Class[0]);check((Boolean)call("loadSave",new Class[]{int.class},2),"future League Cup and lower phases reload");checkBackupReadable();}
                done[0]=(Integer)get("matchday")==34&&(Boolean)call("cupsComplete",new Class[0]);
            });
        }
        ui(()->{
            check(done[0]&&leaguePhase[0]==2&&quickPhase[0]==1,"two actual league-phase matches exercised, watched and quick (finished="+done[0]+", matches="+leaguePhase[0]+", quick="+quickPhase[0]+")");Object cups=get("modernPortugueseCup"),active=cups.getClass().getMethod("active").invoke(cups);String edition=(String)active.getClass().getMethod("snapshot").invoke(active);completePortuguesePostseason();call("continueDivisionSeason",new Class[0]);check(((java.time.LocalDate)get("careerSeasonStart")).getYear()==2029,"third Portuguese season rolls forward");List<?> archives=(List<?>)get("modernPortugueseCup").getClass().getMethod("archives").invoke(get("modernPortugueseCup"));check(archives.size()==2&&archives.get(1).equals(edition),"two new-format League Cups archived exactly");Object third=get("portugueseThird"),fourth=get("portugueseFourth");check(((List<?>)third.getClass().getMethod("archives").invoke(third)).size()==3&&((List<?>)fourth.getClass().getMethod("archives").invoke(fourth)).size()==3,"three complete Portuguese lower-division archives retained");check((Boolean)call("loadSave",new Class[]{int.class},2),"fourth season cup fields and promotions reload");checkBackupReadable();call("showPortugueseLowerHistory",new Class[]{boolean.class},true);
        });
        capture("41-portuguese-lower-history");
        ui(()->{
            Map<String,Object> invalid=new HashMap<>(prefs.getAll());invalid.put("save_2_domestic_cup_history","broken");
            boolean rejected=false;try{gameClass("BackupRestore").getMethod("validate",byte[].class).invoke(null,(Object)backupBytes(invalid));}catch(java.lang.reflect.InvocationTargetException expected){rejected=true;}
            check(rejected,"corrupt cup archive blocks backup replacement");
            Map<String,?> beforeInvalid=prefs.getAll();for(String field:new String[]{"modern_portuguese_cup","portuguese_third","portuguese_fourth","portuguese_district","portuguese_lower_promotion"}){Map<String,Object> bad=new HashMap<>(prefs.getAll());bad.put("save_2_"+field,"broken");boolean blocked=false;try{gameClass("BackupRestore").getMethod("restore",SharedPreferences.class,byte[].class).invoke(null,prefs,(Object)backupBytes(bad));}catch(java.lang.reflect.InvocationTargetException expected){blocked=true;}check(blocked&&prefs.getAll().equals(beforeInvalid),"invalid "+field+" backup rejected");}
            check(true,"Portuguese future League Cup and lower qualification three native seasons watched quick matches archives and backups verified");
            gameClass("BackupRestore").getMethod("restore",SharedPreferences.class,byte[].class).invoke(null,prefs,original);call("loadSave",new Class[]{int.class},0);
        });
    }
    private int appearancesForTeam(int club)throws Exception {int total=0;for(Object p:(List<?>)get("players"))if(playerInt(p,"team")==club)total+=playerInt(p,"appearances");return total;}
    private int playerInt(Object p,String field)throws Exception {Field f=p.getClass().getDeclaredField(field);f.setAccessible(true);return f.getInt(p);}
    private String playerString(Object player,String field)throws Exception {Field value=player.getClass().getDeclaredField(field);value.setAccessible(true);return (String)value.get(player);}
    private void forceStarter(int playerId)throws Exception {
        int[] roles=(int[])get("playerRoleStatus"),slots=(int[])get("playerSelectedSlot");String[] positions=(String[])get("playerSelectedPosition");Object incoming=call("findPlayer",new Class[]{int.class},playerId);if(roles[playerId]==2)return;
        for(Object player:(List<?>)get("players")){int id=playerInt(player,"id");if(playerInt(player,"team")== (Integer)get("selectedClub")&&roles[id]==2&&!playerString(player,"position").equals("GK")){int slot=slots[id];roles[id]=0;slots[id]=-1;roles[playerId]=2;slots[playerId]=slot;positions[playerId]=playerString(incoming,"position");return;}}throw new AssertionError("No outfield starter to replace");
    }
    private void verifyUEFAPlayers()throws Exception {
        SharedPreferences prefs=getTargetContext().getSharedPreferences("project_mister",Context.MODE_PRIVATE);byte[] original=backupBytes(prefs.getAll());final int[] late={-1},dismissed={-1};final boolean[] blocked={false},served={false},winterSigned={false},knockout={false};final Set<Integer> winter=new HashSet<>();
        ui(()->{
            Object data=null;for(Object d:(List<?>)get("catalog").getClass().getField("divisions").get(get("catalog")))if(d.getClass().getField("id").get(d).equals("pt:1"))data=d;
            Object world=call("newCountryCareer",new Class[]{data.getClass()},data);call("configureDivision",new Class[]{gameClass("CareerDivision")},world);set("selectedSlot",2);set("selectedClub",0);((Random)get("random")).setSeed(29317);call("initialiseNewManagerDefaults",new Class[0]);call("resetCareerState",new Class[0]);call("generatePlayers",new Class[0]);call("initialiseTacticsForClub",new Class[0]);call("initialiseClassicCareerSystems",new Class[0]);set("wageBudgetK",1000000);set("currentTransferBudget",1000000);call("saveCurrentGame",new Class[0]);check(get("europeanCampaign")!=null,"Portuguese career has actual European competitions");
            int base=((List<?>)get("players")).size(),first=(Integer)call("signMarketPlayer",new Class[]{String.class},"free~0~57");check(first==base,"First market hire has a stable supplementary identity");call("showUEFAPlayerLists",new Class[0]);check(((List<?>)get("players")).size()==base+2,"Real academy goalkeeper is added between market hires");check((Integer)call("signMarketPlayer",new Class[]{String.class},"free~0~58")==base+2,"Second market hire appends after the academy identity");Object player=call("findPlayer",new Class[]{int.class},first);check((Integer)call("playerPortraitIdentity",new Class[]{player.getClass()},player)=="free~0~57".hashCode(),"Academy addition preserves the market-player portrait identity");String origins=(String)get("rosterExtensions").getClass().getMethod("snapshot").invoke(get("rosterExtensions"));check((Boolean)call("loadSave",new Class[]{int.class},2),"Interleaved players reload");check(origins.equals(get("rosterExtensions").getClass().getMethod("snapshot").invoke(get("rosterExtensions"))),"Supplementary market and academy order reloads exactly");checkBackupReadable();call("showUEFAPlayerLists",new Class[0]);
        });capture("uefa-player-lists-opening");
        for(int step=0;step<650&&!knockout[0];step++)ui(()->{
            call("advanceCareerDate",new Class[]{java.time.LocalDate.class},call("nextCareerEventDate",new Class[0]));for(Object player:(List<?>)get("players")){Field injury=player.getClass().getDeclaredField("injuredWeeks");injury.setAccessible(true);injury.setInt(player,0);}call("initialiseTacticsForClub",new Class[0]);java.time.LocalDate today=(java.time.LocalDate)get("currentDate");
            if(!winterSigned[0]&&!today.isBefore(java.time.LocalDate.of(2027,1,1))){set("currentTransferBudget",1000000);set("wageBudgetK",1000000);for(int slot:new int[]{52,53,54}){int id=(Integer)call("signMarketPlayer",new Class[]{String.class},"free~0~"+slot);check(id>=0,"Actual January free-agent signing completed");winter.add(id);}winterSigned[0]=true;}
            if((Boolean)call("cupDue",new Class[0])&&(Boolean)call("nextCupIsEuropean",new Class[0])) {
                Object tie=call("nextCupTie",new Class[0]),fixture=call("cupDisciplineFixture",new Class[0]);int home=(Integer)tie.getClass().getMethod("home").invoke(tie),away=(Integer)tie.getClass().getMethod("away").invoke(tie),phase=fixture.getClass().getField("phase").getInt(fixture);
                if(home==0||away==0){call("ensureUEFARegistration",new Class[]{fixture.getClass()},fixture);
                    if(late[0]<0){set("currentTransferBudget",1000000);set("wageBudgetK",1000000);late[0]=(Integer)call("signMarketPlayer",new Class[]{String.class},"free~0~59");check(late[0]>=0,"Actual free agent signs after league-phase List A closes");winter.add(late[0]);Object player=call("findPlayer",new Class[]{int.class},late[0]);check(!((String)call("cupPlayerReason",new Class[]{fixture.getClass(),player.getClass()},fixture,player)).isEmpty(),"Late free agent is excluded from the actual European league-phase list");forceStarter(late[0]);call("startLiveMatchday",new Class[0]);check(!(Boolean)get("matchInProgress"),"Actual European match blocks an unlisted starter");call("initialiseTacticsForClub",new Class[0]);check(((int[])get("playerRoleStatus"))[late[0]]!=2,"Assistant replaces the unlisted starter");blocked[0]=true;}
                    if(phase>=11){Object registration=get("uefaRegistration"),book=registration.getClass().getMethod("book",int.class,String.class,int.class).invoke(registration,0,fixture.getClass().getField("competition").get(fixture),phase);check(winter.size()==4&&(Integer)book.getClass().getMethod("winterAdditions").invoke(book)==3,"Actual knockout list admits three of four genuine winter players");int omitted=-1,permitted=-1;for(int id:winter){if((Boolean)book.getClass().getMethod("contains",int.class).invoke(book,id))permitted=id;else omitted=id;}check(omitted>=0&&permitted>=0,"Fourth winter player remains outside the saved list");forceStarter(omitted);call("startLiveMatchday",new Class[0]);check(!(Boolean)get("matchInProgress"),"Actual knockout match blocks the fourth new player");call("initialiseTacticsForClub",new Class[0]);forceStarter(permitted);call("startLiveMatchday",new Class[0]);check((Boolean)get("matchInProgress")&&(Boolean)get("liveEuropean")&&((Set<?>)get("matchParticipants")).contains(permitted),"Registered winter starter actually participates in the playable knockout match");knockout[0]=true;}
                }
            }
            if(!(Boolean)get("matchInProgress"))call("startLiveMatchday",new Class[0]);if(!(Boolean)get("matchInProgress"))return;set("livePaused",true);boolean european=(Boolean)get("liveEuropean"),serving=european&&dismissed[0]>=0&&!served[0];
            if(european&&dismissed[0]<0){for(Object id:(List<?>)get("liveXIIds")){Object player=call("findPlayer",new Class[]{int.class},id);if(!playerString(player,"position").equals("GK")){dismissed[0]=(Integer)id;call("recordLiveYellow",new Class[]{player.getClass()},player);call("recordLiveYellow",new Class[]{player.getClass()},player);break;}}check(dismissed[0]>=0&&((Set<?>)get("matchDismissals")).contains(dismissed[0]),"Actual listed UEFA player receives a second-yellow dismissal");}
            if(serving)check(!((Set<?>)get("matchParticipants")).contains(dismissed[0]),"Suspended UEFA player is absent from the eligible serving fixture");
            set("liveHomeGoals",(Integer)get("liveHome")==0?4:0);set("liveAwayGoals",(Integer)get("liveAway")==0?4:0);call("finishLiveMatch",new Class[0]);if(serving){List<?> summary=(List<?>)get("matchDiscipline").getClass().getMethod("playerSummary",int.class).invoke(get("matchDiscipline"),dismissed[0]);check(summary.stream().noneMatch(x->x.toString().contains("UEFA")&&x.toString().contains("suspended")),"Actual listed eligible UEFA fixture serves the saved ban");served[0]=true;}
        });check(knockout[0]&&blocked[0]&&served[0],"Actual UEFA registration and ban-service scenario completes");
        ui(()->{
            String lists=(String)get("uefaRegistration").getClass().getMethod("snapshot").invoke(get("uefaRegistration")),origins=(String)get("rosterExtensions").getClass().getMethod("snapshot").invoke(get("rosterExtensions")),training=(String)get("trainingHistory").getClass().getMethod("snapshot").invoke(get("trainingHistory"));check((Boolean)call("loadSave",new Class[]{int.class},2),"Actual UEFA match, supplementary players and youth histories reload");check(lists.equals(get("uefaRegistration").getClass().getMethod("snapshot").invoke(get("uefaRegistration")))&&origins.equals(get("rosterExtensions").getClass().getMethod("snapshot").invoke(get("rosterExtensions")))&&training.equals(get("trainingHistory").getClass().getMethod("snapshot").invoke(get("trainingHistory"))),"Actual player lists and profiles retain exact saves");call("showUEFAPlayerLists",new Class[0]);
            Map<String,?> previous=new HashMap<>(prefs.getAll());for(String field:new String[]{"uefa_player_lists","roster_extensions","player_training_history"}){Map<String,Object> broken=new HashMap<>(previous);broken.remove("save_2_"+field);boolean rejected=false;try{gameClass("BackupRestore").getMethod("restore",SharedPreferences.class,byte[].class).invoke(null,prefs,backupBytes(broken));}catch(InvocationTargetException expected){rejected=expected.getCause() instanceof IOException;}check(rejected&&prefs.getAll().equals(previous),"Missing "+field+" rejected without replacing any career");}checkBackupReadable();
        });capture("uefa-player-lists-winter");ui(()->{gameClass("BackupRestore").getMethod("restore",SharedPreferences.class,byte[].class).invoke(null,prefs,original);check((Boolean)call("loadSave",new Class[]{int.class},0),"Original careers restored after UEFA player-list scenario");});check(true,"UEFA actual A/B lists, late free-agent exclusion, saved goalkeeper identities, registered ban service, three winter additions and atomic backups verified");
    }
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
    private void verifyDivisionCareers() throws Exception {
        Object catalog=get("catalog");check(catalog!=null,"competition catalog available");
        List<?> divisions=(List<?>)catalog.getClass().getField("divisions").get(catalog);
        Class<?> worldClass=activity.getClassLoader().loadClass("com.projectmister.game.CareerDivision");
        for(int index=0;index<divisions.size()*2;index++) {
            final boolean linked=index>=divisions.size();
            Object data=divisions.get(index%divisions.size());String id=(String)data.getClass().getField("id").get(data);
            Object world=linked?worldClass.getMethod("countryCareer",catalog.getClass(),data.getClass()).invoke(null,catalog,data):worldClass.getConstructor(data.getClass()).newInstance(data);
            int count=((List<?>)data.getClass().getField("clubs").get(data)).size();final int leagueIndex=index%divisions.size();
            final int worldCount=((String[])worldClass.getField("names").get(world)).length;
            final int[] memberships=(int[])worldClass.getField("clubTiers").get(world);
            ui(()->{
                call("configureDivision",new Class[]{worldClass},world);set("managerLeagueIndex",leagueIndex);
                set("selectedSlot",1);set("selectedClub",count-1);call("initialiseNewManagerDefaults",new Class[0]);
                set("managerLeagueIndex",leagueIndex);set("managerFirstName","Division");set("managerLastName",id);
                call("resetCareerState",new Class[0]);call("generatePlayers",new Class[0]);
                call("initialiseTacticsForClub",new Class[0]);call("initialiseClassicCareerSystems",new Class[0]);
                check(((List<?>)get("players")).size()==worldCount*20,id+" generates full squads");
                String[] options=(String[])get("LEAGUE_OPTIONS");check(options.length==divisions.size()&&leagueIndex>=0&&leagueIndex<options.length&&options[leagueIndex].contains((String)data.getClass().getField("name").get(data)),id+" maps to the real zero-based league chooser");
                call("saveCurrentGame",new Class[0]);SharedPreferences saved=getTargetContext().getSharedPreferences("project_mister",Context.MODE_PRIVATE);check(saved.getInt("save_1_manager_league_index",-1)==leagueIndex&&options[leagueIndex].equals(saved.getString("save_1_manager_league","")),id+" saves the exact chooser index and label");call("showMainMenu",new Class[0]);
            });
            if(id.equals("eng:2")||id.equals("be:2")||id.equals("fr:2")||id.equals("sco:1"))capture("division-"+id.replace(':','-')+"-save");
            ui(()->{
                check((Boolean)call("loadSave",new Class[]{int.class},1),id+" save loads");
                check((Integer)get("managerLeagueIndex")==leagueIndex,id+" restores the selected real division index");
                check(((String[])get("clubNames")).length==worldCount && (Integer)get("selectedClub")==count-1,id+" club identity survives reload");
                if(id.equals("eng:2")) {
                    call("startLiveMatchday",new Class[0]);set("livePaused",true);
                    check((Boolean)get("liveMatchActive"),"24-club career starts live match");
                    call("finishLiveMatch",new Class[0]);
                    check(((int[])get("played"))[count-1]==1,"24-club live result reaches table");
                    call("loadSave",new Class[]{int.class},1);
                    check(((int[])get("played"))[count-1]==1,"24-club live result survives reload");
                    call("resetLeagueStandings",new Class[0]);
                }
                if(count%2==1) {
                    int bye=-1;int total=(Integer)call("seasonRounds",new Class[0]);
                    for(int r=0;r<total;r++)if((Integer)call("leagueOpponentForRound",new Class[]{int.class},r)<0){bye=r;break;}
                    check(bye>=0,"odd division has rest round");set("matchday",bye);
                    call("advanceByeRound",new Class[0]);
                    check(((int[])get("played"))[count-1]==0 && (Integer)get("matchday")==bye+1,"bye advances without invented result");
                    call("resetLeagueStandings",new Class[0]);
                }
                int rounds=(Integer)call("seasonRounds",new Class[0]);int byes=0;
                for(int round=0;round<rounds;round++) {
                    set("matchday",round);
                    Object[] games=(Object[])call("fixturesForRound",new Class[]{int.class},round);
                    HashSet<Integer> seen=new HashSet<>();
                    for(Object game:games) {
                        int home=game.getClass().getField("home").getInt(game),away=game.getClass().getField("away").getInt(game);
                        if(!seen.add(home)||!seen.add(away))throw new AssertionError(id+" repeated club in round "+round);
                        call("updateTable",new Class[]{int.class,int.class,int.class,int.class},home,away,1,0);
                    }
                    if(!seen.contains(count-1)) {
                        byes++;if((Integer)call("leagueOpponentForRound",new Class[]{int.class},round)!=-999)throw new AssertionError("Invented bye opponent");
                    }
                    set("matchday",round+1);call("prepareSplitIfNeeded",new Class[0]);
                }
                // This fast-forward fixture seeds cup results too; the watched cup flow has its own test.
                Object cup=get("leagueCup");
                if(cup!=null) {
                    while(!(Boolean)cup.getClass().getMethod("complete").invoke(cup)) {
                        Object tie=cup.getClass().getMethod("current").invoke(cup);
                        tie.getClass().getMethod("recordRegulation",int.class,int.class).invoke(tie,1,0);
                    }
                    set("currentDate",((java.time.LocalDate)get("careerSeasonStart")).plusWeeks(rounds));
                }
                int expected=id.equals("sco:1")?38:id.equals("sco:2")?36:(count-1)*2;
                int expectedResults=0;
                for(int club=0;club<worldCount;club++) {
                    int members=((int[])worldClass.getMethod("members",int.class).invoke(world,memberships[club])).length;
                    int games=id.startsWith("sco:")?(memberships[club]==1?38:36):(members-1)*2;
                    if(((int[])get("played"))[club]!=games)throw new AssertionError(id+" wrong season appearances for "+club);
                    expectedResults+=games;
                }
                check(byes==rounds-expected,id+" byes and season lengths correct");
                call("saveCurrentGame",new Class[0]);call("loadSave",new Class[]{int.class},0);
                check(((String[])get("clubNames")).length==18,"legacy world restored after "+id);
                call("loadSave",new Class[]{int.class},1);
                check((Integer)get("matchday")==rounds,id+" completed season reloads");
                Object ledger=get("leagueResults");
                check((Integer)ledger.getClass().getMethod("size").invoke(ledger)==expectedResults/2,id+" all results survive reload");
                if(id.equals("sco:1")||(linked&&id.equals("sco:2")))check(((int[])get("splitOrder")).length==12,"Scottish split survives reload");
                call("showLeagueTable",new Class[0]);
            });
            if(id.equals("eng:2")||id.equals("be:2")||id.equals("fr:2")||id.equals("sco:1"))capture("division-"+id.replace(':','-')+"-table");
            if(linked)ui(()->{
                int[] upper=(int[])worldClass.getMethod("members",int.class).invoke(world,1);
                int[] lower=(int[])worldClass.getMethod("members",int.class).invoke(world,2);
                boolean[] reserves=(boolean[])worldClass.getField("reserves").get(world);
                int promoted=-1;for(int club:lower)if(!reserves[club]){promoted=club;break;}
                Object moved=worldClass.getMethod("moveBetweenTiers",int[].class,int[].class,int.class).invoke(world,new int[]{promoted},new int[]{upper[0]},promoted);
                check(worldClass.getField("tier").getInt(moved)==1,"promoted manager follows club");
                check(java.util.Arrays.equals((String[])worldClass.getField("clubIds").get(world),(String[])worldClass.getField("clubIds").get(moved)),"tier movement preserves every club identity");
                check(((int[])worldClass.getMethod("members",int.class).invoke(moved,1)).length==upper.length,"tier movement preserves division sizes");
                Object restored=worldClass.getMethod("restore",String.class).invoke(null,worldClass.getMethod("snapshot").invoke(moved));
                check(java.util.Arrays.equals((int[])worldClass.getField("clubTiers").get(moved),(int[])worldClass.getField("clubTiers").get(restored)),"membership movement survives snapshot");
                int other=3-worldClass.getField("tier").getInt(world);
                call("showLeagueResults",new Class[]{int.class,int.class},0,other);

            });
            if(linked) {
                waitForIdleSync();
                check(node("League results")!=null,"other division results screen opens");
                if(id.equals("eng:2"))capture("linked-eng-2-other-results");
                ui(()->call("showLeagueTable",new Class[]{int.class},3-worldClass.getField("tier").getInt(world)));
            }
            if(linked&&(id.equals("eng:2")||id.equals("sco:2")))capture("linked-"+id.replace(':','-')+"-other-table");
            if(id.equals("eng:2"))page("division-eng-2-results","showLeagueResults",new Class[]{int.class},0);
            if(linked&&id.equals("sco:2"))verifyScottishPromotion();
            if(linked&&id.equals("de:2"))verifyGermanPromotion();
            if(linked&&id.equals("tr:2"))verifyTurkishPromotion();
            if(linked&&(id.equals("pt:2")||id.equals("es:2")||id.equals("be:2")||id.equals("fr:2")||id.equals("eng:2")||id.equals("it:2")||id.equals("nl:2")))verifyEligiblePromotion();
        }
        ui(()->call("loadSave",new Class[]{int.class},0));
        check(true,"all twenty standalone and twenty linked division careers verified");
    }

    private Class<?> gameClass(String name)throws Exception{return activity.getClass().getClassLoader().loadClass("com.projectmister.game."+name);}
    private byte[] backupBytes(Map<String,?> values)throws Exception{return (byte[])gameClass("SaveBackup").getMethod("encode",Map.class).invoke(null,values);}
    private void checkBackupReadable()throws Exception {
        SharedPreferences prefs=getTargetContext().getSharedPreferences("project_mister",Context.MODE_PRIVATE);
        Object checked=gameClass("BackupRestore").getMethod("validate",byte[].class).invoke(null,(Object)backupBytes(prefs.getAll()));
        check(checked.equals(prefs.getAll()),"backup preserves current linked postseason and classic saves");
    }
    private void checkModernBackupCompleteness()throws Exception {
        SharedPreferences prefs=getTargetContext().getSharedPreferences("project_mister",Context.MODE_PRIVATE);
        Map<String,?> original=new HashMap<>(prefs.getAll());
        for(String field:new String[]{"league_dates","league_decisions"}) {
            Map<String,Object> broken=new HashMap<>(original);broken.remove("save_2_"+field);
            try{gameClass("BackupRestore").getMethod("restore",SharedPreferences.class,byte[].class).invoke(null,prefs,backupBytes(broken));throw new AssertionError("Missing modern state accepted: "+field);}
            catch(InvocationTargetException expected){check(expected.getCause() instanceof IOException,"Missing "+field+" rejected before replacement");}
            check(prefs.getAll().equals(original),"Missing "+field+" leaves every career and setting unchanged");
        }
    }
    private void verifyBackups()throws Exception {
        SharedPreferences prefs=getTargetContext().getSharedPreferences("project_mister",Context.MODE_PRIVATE);
        Map<String,?> original=new HashMap<>(prefs.getAll());byte[] bytes=backupBytes(original);
        checkBackupReadable();
        Class<?> restore=gameClass("BackupRestore");
        byte[] damaged=bytes.clone();damaged[20]^=1;
        for(byte[] bad:new byte[][]{damaged,backupBytes(Collections.singletonMap("save_0_exists",true))}) {
            try{restore.getMethod("restore",SharedPreferences.class,byte[].class).invoke(null,prefs,bad);throw new AssertionError("Invalid backup accepted");}
            catch(InvocationTargetException expected){check(expected.getCause() instanceof IOException,"invalid backup rejected before replacement");}
            check(prefs.getAll().equals(original),"invalid backup leaves every career and setting unchanged");
        }
        Map<String,Object> invalidClub=new HashMap<>(original);invalidClub.put("save_0_club",9999);
        try{restore.getMethod("restore",SharedPreferences.class,byte[].class).invoke(null,prefs,backupBytes(invalidClub));throw new AssertionError("Invalid club accepted");}
        catch(InvocationTargetException expected){check(expected.getCause() instanceof IOException,"invalid club identity rejected");}
        check(prefs.getAll().equals(original),"invalid club restore leaves careers unchanged");
        ui(()->call("showBackupTools",new Class[0]));capture("career-backup-tools");
        ui(()->call("onActivityResult",new Class[]{int.class,int.class,Intent.class},8102,Activity.RESULT_CANCELED,null));
        check(prefs.getAll().equals(original),"canceling document picker changes no data");
        File backup=new File(getTargetContext().getCacheDir(),"qa-careers.bossxi");backup.delete();
        Intent selected=new Intent().setData(android.net.Uri.fromFile(backup));
        ui(()->call("onActivityResult",new Class[]{int.class,int.class,Intent.class},8101,Activity.RESULT_OK,selected));
        long until=SystemClock.uptimeMillis()+15000;boolean exported=false;
        while(SystemClock.uptimeMillis()<until){
            if(backup.isFile())try(InputStream in=new FileInputStream(backup)){
                byte[] content=(byte[])gameClass("SaveBackup").getMethod("read",InputStream.class).invoke(null,in);
                Object map=gameClass("SaveBackup").getMethod("decode",byte[].class).invoke(null,(Object)content);
                if(map.equals(original)){exported=true;break;}
            }catch(Exception incomplete){}SystemClock.sleep(100);
        }
        check(exported,"document export writes complete readable backup");waitForIdleSync();
        prefs.edit().putBoolean("audio_crowd",!prefs.getBoolean("audio_crowd",true)).commit();
        ui(()->call("onActivityResult",new Class[]{int.class,int.class,Intent.class},8102,Activity.RESULT_OK,selected));
        node("Replace current careers?");capture("career-backup-confirmation");tap("Cancel");
        check(!prefs.getAll().equals(original),"canceling restore preserves current settings");
        ui(()->call("onActivityResult",new Class[]{int.class,int.class,Intent.class},8102,Activity.RESULT_OK,selected));
        tap("Replace & restore");
        until=SystemClock.uptimeMillis()+15000;
        while(SystemClock.uptimeMillis()<until&&!prefs.getAll().equals(original))SystemClock.sleep(100);
        check(prefs.getAll().equals(original),"confirmed restore replaces all careers and settings exactly");waitForIdleSync();
        ui(()->{
            check((Boolean)call("loadSave",new Class[]{int.class},1),"restored linked career opens");
            check((Boolean)call("loadSave",new Class[]{int.class},0),"restored classic career opens");
            call("showDashboard",new Class[0]);
        });
        backup.delete();check(true,"career backup export, validation, cancellation, restore and load verified");
    }

    private void verifyEligiblePromotion() throws Exception {
        final int[] club={0},games={0},rounds={0},size={0};final String[][] identities={null};final String[] country={null};
        ui(()->{
            Object world=get("division");Class<?> type=world.getClass();country[0]=(String)type.getField("country").get(world);
            int[] upper=(int[])type.getMethod("members",int.class).invoke(world,1),lower=(int[])type.getMethod("members",int.class).invoke(world,2);
            boolean[] reserve=(boolean[])type.getField("reserves").get(world);
            int[] totals=(int[])get("points");for(int i=0;i<upper.length;i++)totals[upper[i]]=100-i;
            java.util.ArrayList<Integer> eligible=new java.util.ArrayList<>();
            for(int i=0;i<lower.length;i++){totals[lower[i]]=80-i;if(!reserve[lower[i]])eligible.add(lower[i]);}
            // Parent clubs remain safely above relegation in this transition test.
            String[] ids=(String[])type.getField("clubIds").get(world);
            for(int id:upper)if(ids[id].equals("pt:fc-porto")||ids[id].equals("pt:sl-benfica")||ids[id].equals("pt:sporting-cp")||ids[id].equals("es:celta")||ids[id].equals("es:real-sociedad")||ids[id].equals("be:club-brugge")||ids[id].equals("be:krc-genk")||ids[id].equals("be:kaa-gent")||ids[id].equals("be:rsc-anderlecht"))totals[id]+=100;
            if(country[0].equals("NL")) {
                Object old=get("leagueResults"),ledger=gameClass("LeagueResults").getConstructor(int.class,boolean.class).newInstance(ids.length,true);
                java.lang.reflect.Method record=ledger.getClass().getMethod("record",int.class,int.class,int.class,int.class,int.class);
                for(int round=0;round<38;round++)for(Object result:(java.util.List<?>)old.getClass().getMethod("round",int.class).invoke(old,round)) {
                    Class<?> rc=result.getClass();int h=rc.getField("home").getInt(result),a=rc.getField("away").getInt(result);
                    int hi=-1,ai=-1;for(int i=0;i<lower.length;i++){if(lower[i]==h)hi=i;if(lower[i]==a)ai=i;}
                    record.invoke(ledger,round,h,a,hi>=0?(hi<ai?60-hi:0):rc.getField("homeGoals").getInt(result),ai>=0?(ai<hi?60-ai:0):rc.getField("awayGoals").getInt(result));
                }
                set("leagueResults",ledger);
                for(int id:upper)if(ids[id].equals("nl:ajax")||ids[id].equals("nl:az")||ids[id].equals("nl:fc-utrecht")||ids[id].equals("nl:psv"))totals[id]+=100;
            }
            set("promotion",null);club[0]=eligible.get(country[0].equals("BE")?1:country[0].equals("FR")?3:(country[0].equals("ENG")||country[0].equals("IT"))?4:2);set("selectedClub",club[0]);call("initialiseTacticsForClub",new Class[0]);
            check((Boolean)call("preparePromotion",new Class[0]),country[0]+" eligible standings seed promotion");
            int[] entrants=(int[])get("promotion").getClass().getMethod("lowerEntrants").invoke(get("promotion"));
            for(int id:entrants)check(!reserve[id],country[0]+" reserve excluded from promotion");
            if(country[0].equals("NL")){club[0]=entrants[7];set("selectedClub",club[0]);call("initialiseTacticsForClub",new Class[0]);}
            identities[0]=ids.clone();games[0]=((int[])get("played"))[club[0]];rounds[0]=(Integer)get("matchday");
            Object ledger=get("leagueResults");size[0]=(Integer)ledger.getClass().getMethod("size").invoke(ledger);
        });
        for(int i=0;i<(country[0].equals("PT")?2:country[0].equals("IT")?5:country[0].equals("NL")?6:4);i++){
            ui(()->{
                call("advancePromotion",new Class[0]);set("livePaused",true);
                check((Boolean)get("livePlayoff"),country[0]+" manager watches playoff leg");
                set("liveHomeGoals",(Integer)get("liveHome")==club[0]?2:0);set("liveAwayGoals",(Integer)get("liveAway")==club[0]?2:0);
                call("finishLiveMatch",new Class[0]);call("loadSave",new Class[]{int.class},1);
                check(get("promotion").getClass().getMethod("country").invoke(get("promotion")).equals(country[0]),country[0]+" promotion restores");
                checkBackupReadable();
                check(((int[])get("played"))[club[0]]==games[0]&&(Integer)get("matchday")==rounds[0],country[0]+" league table unchanged by playoffs");
                Object ledger=get("leagueResults");check((Integer)ledger.getClass().getMethod("size").invoke(ledger)==size[0],country[0]+" league history unchanged");
            });
        }
        ui(()->{check((Boolean)get("promotion").getClass().getMethod("complete").invoke(get("promotion")),country[0]+" playoffs finish");call("showSeasonReview",new Class[0]);});
        capture(country[0].toLowerCase(java.util.Locale.ROOT)+"-promotion-review");
        ui(()->{
            call("continueDivisionSeason",new Class[0]);call("loadSave",new Class[]{int.class},1);
            Object world=get("division");Class<?> type=world.getClass();
            check(type.getField("tier").getInt(world)==1,country[0]+" winner promoted");
            check(java.util.Arrays.equals(identities[0],(String[])type.getField("clubIds").get(world)),country[0]+" stable identities preserved");
            check(((int[])type.getMethod("members",int.class).invoke(world,1)).length==(country[0].equals("ES")||country[0].equals("ENG")||country[0].equals("IT")?20:18),country[0]+" top division size preserved");
            check((Integer)get("matchday")==0&&get("promotion")==null,country[0]+" next season survives reload");
            Object archive=get("seasonHistory");Class<?> archiveClass=archive.getClass();
            java.util.List<?> seasons=(java.util.List<?>)archiveClass.getMethod("entries").invoke(archive);
            check(seasons.size()==1,country[0]+" completed season archived once");
            Object season=seasons.get(0);Class<?> entryClass=season.getClass();
            check(entryClass.getField("clubId").get(season).equals(identities[0][club[0]])&&entryClass.getField("played").getInt(season)==games[0],country[0]+" archive retains identity and final league games");
            check(entryClass.getField("tier").getInt(season)==2&&entryClass.getField("nextTier").getInt(season)==1,country[0]+" archive retains promotion");
            String saved=(String)archiveClass.getMethod("snapshot").invoke(archive);
            Object date=get("careerSeasonStart");call("continueDivisionSeason",new Class[0]);
            check(date.equals(get("careerSeasonStart"))&&saved.equals(archiveClass.getMethod("snapshot").invoke(get("seasonHistory"))),country[0]+" repeated rollover cannot duplicate archive or skip season");
            checkBackupReadable();call("showSeasonHistory",new Class[0]);

            check(playerInt(call("findPlayer",new Class[]{int.class},club[0]*20),"team")==club[0],country[0]+" player remains at club");
        });
        capture(country[0].toLowerCase(java.util.Locale.ROOT)+"-season-history");
        check(true,country[0]+" watched playoffs and season transition verified");
    }

    private void verifyTurkishPromotion() throws Exception {
        final String[][] identities={null};final int[] club={0},games={0},ledgerSize={0};
        ui(()->{
            Object world=get("division");Class<?> type=world.getClass();
            int[] upper=(int[])type.getMethod("members",int.class).invoke(world,1),lower=(int[])type.getMethod("members",int.class).invoke(world,2);
            int[] totals=(int[])get("points");for(int i=0;i<upper.length;i++)totals[upper[i]]=100-i;
            for(int i=0;i<lower.length;i++)totals[lower[i]]=80-i;
            set("promotion",null);club[0]=lower[3];set("selectedClub",club[0]);call("initialiseTacticsForClub",new Class[0]);
            Object ledger=get("leagueResults");ledgerSize[0]=(Integer)ledger.getClass().getMethod("size").invoke(ledger);
            // Historical saves with an unresolved seventh/eighth boundary must not invent a qualifier.
            totals[lower[7]]=totals[lower[6]];
            set("leagueResults",ledger.getClass().getConstructor(int.class,boolean.class).newInstance(upper.length+lower.length,false));
            check(!(Boolean)call("preparePromotion",new Class[0]),"Turkish missing-history qualification tie blocks promotion");
            set("leagueResults",ledger);totals[lower[7]]=73;
            check((Boolean)call("preparePromotion",new Class[0]),"resolved Turkish standings seed playoffs");
            identities[0]=((String[])type.getField("clubIds").get(world)).clone();games[0]=((int[])get("played"))[club[0]];
        });
        for(int game=0;game<4;game++) {
            final int step=game;
            ui(()->{
                call("advancePromotion",new Class[0]);set("livePaused",true);
                check((Boolean)get("livePlayoff"),"manager watches Turkish postseason match "+step);
                Object campaign=get("promotion"),tie=campaign.getClass().getMethod("current").invoke(campaign);
                if(step==0)check(tie.getClass().getField("legs").getInt(tie)==1,"Turkish eliminator is one match");
                if(step==2)check((Integer)tie.getClass().getMethod("playedLegs").invoke(tie)==1,"Turkish first-leg result survives reload");
                if(step==3) {
                    check(tie.getClass().getField("neutral").getBoolean(tie),"Turkish final uses neutral ground");
                    check(call("liveTieSummary",new Class[0]).equals("FINAL • NEUTRAL VENUE"),"neutral final has correct live label");
                }
                set("liveHomeGoals",(Integer)get("liveHome")==club[0]?2:0);set("liveAwayGoals",(Integer)get("liveAway")==club[0]?2:0);
                call("refreshLiveHeader",new Class[0]);
            });
            if(game==3)capture("turkish-neutral-final");
            ui(()->{
                call("finishLiveMatch",new Class[0]);call("loadSave",new Class[]{int.class},1);
                check(get("promotion").getClass().getMethod("country").invoke(get("promotion")).equals("TR"),"Turkish campaign reloads with correct country");
                check(((int[])get("played"))[club[0]]==games[0]&&(Integer)get("matchday")==38,"Turkish playoffs leave league appearances and round unchanged");
                Object ledger=get("leagueResults");check((Integer)ledger.getClass().getMethod("size").invoke(ledger)==ledgerSize[0],"Turkish playoffs do not enter league ledger");
            });
        }
        ui(()->{
            check((Boolean)get("promotion").getClass().getMethod("complete").invoke(get("promotion")),"Turkish five-match campaign completes");
            call("showSeasonReview",new Class[0]);
        });
        capture("turkish-promotion-review");
        ui(()->{
            call("continueDivisionSeason",new Class[0]);call("loadSave",new Class[]{int.class},1);Object world=get("division");Class<?> type=world.getClass();
            check(type.getField("tier").getInt(world)==1,"Turkish playoff winner promoted to Super Lig");
            check(((int[])type.getMethod("members",int.class).invoke(world,1)).length==18&&((int[])type.getMethod("members",int.class).invoke(world,2)).length==20,"Turkish tier sizes preserved");
            check(java.util.Arrays.equals(identities[0],(String[])type.getField("clubIds").get(world)),"Turkish promotion preserves stable club IDs");
            check((Integer)get("matchday")==0&&get("promotion")==null,"Turkish season transition survives reload");
            check(playerInt(call("findPlayer",new Class[]{int.class},club[0]*20),"team")==club[0],"Turkish player remains at original club");
        });
        check(true,"Turkish watched playoffs and season transition verified");
    }

    private void verifyGermanPromotion() throws Exception {
        final String[][] identities={null};final int[] club={0},games={0};
        ui(()->{
            Object world=get("division");Class<?> type=world.getClass();
            int[] upper=(int[])type.getMethod("members",int.class).invoke(world,1),lower=(int[])type.getMethod("members",int.class).invoke(world,2);
            int[] totals=(int[])get("points");for(int i=0;i<upper.length;i++)totals[upper[i]]=100-i;
            for(int i=0;i<lower.length;i++)totals[lower[i]]=80-i;
            set("promotion",null);club[0]=lower[2];set("selectedClub",club[0]);call("initialiseTacticsForClub",new Class[0]);
            check((Boolean)call("preparePromotion",new Class[0]),"resolved German standings seed playoff");
            identities[0]=((String[])type.getField("clubIds").get(world)).clone();games[0]=((int[])get("played"))[club[0]];
            call("startLiveMatchday",new Class[]{boolean.class},true);set("livePaused",true);
            check((Boolean)get("livePlayoff"),"manager can watch German first leg");
            set("liveHomeGoals",(Integer)get("liveHome")==club[0]?2:0);set("liveAwayGoals",(Integer)get("liveAway")==club[0]?2:0);
            call("finishLiveMatch",new Class[0]);call("loadSave",new Class[]{int.class},1);
            Object campaign=get("promotion"),tie=campaign.getClass().getMethod("current").invoke(campaign);
            check(campaign.getClass().getMethod("country").invoke(campaign).equals("DE"),"German campaign reloads as correct country");
            check((Integer)tie.getClass().getMethod("playedLegs").invoke(tie)==1,"German first leg survives reload");
            call("startLiveMatchday",new Class[]{boolean.class},true);set("livePaused",true);set("liveMinute",85);
            float intent=(Float)call("teamIntent",new Class[]{int.class},(Integer)get("liveHome")==club[0]?(Integer)get("liveAway"):(Integer)get("liveHome"));
            check(intent>0,"opponent chases aggregate deficit in return leg");
            call("finishLiveMatch",new Class[0]);
            check(((int[])get("played"))[club[0]]==games[0]&&(Integer)get("matchday")==34,"German playoff leaves league table untouched");
            check((Boolean)get("promotion").getClass().getMethod("complete").invoke(get("promotion")),"German two-leg playoff completes");
        });
        capture("german-promotion-review");
        ui(()->{
            call("continueDivisionSeason",new Class[0]);call("loadSave",new Class[]{int.class},1);Object world=get("division");Class<?> type=world.getClass();
            check(type.getField("tier").getInt(world)==1,"winning German manager promoted to Bundesliga");
            check(((int[])type.getMethod("members",int.class).invoke(world,1)).length==18&&((int[])type.getMethod("members",int.class).invoke(world,2)).length==18,"German tier sizes preserved");
            check(java.util.Arrays.equals(identities[0],(String[])type.getField("clubIds").get(world)),"German promotion retains stable club IDs");
            check((Integer)get("matchday")==0&&get("promotion")==null,"German next-season transition persists");
            check(playerInt(call("findPlayer",new Class[]{int.class},club[0]*20),"team")==club[0],"German player remains at original club");
        });
        check(true,"German watched playoff and season transition verified");
    }

    private void verifyScottishPromotion() throws Exception {
        final int[] leagueGames={0},playerId={0},oldTeam={0};
        final String[][] identities={null};
        ui(()->{
            Object world=get("division");Class<?> type=world.getClass();
            int[] upper=(int[])type.getMethod("members",int.class).invoke(world,1),lower=(int[])type.getMethod("members",int.class).invoke(world,2);
            // Synthetic distinct final standings isolate the postseason transaction from league-rank tests.
            int[] totals=(int[])get("points");for(int i=0;i<upper.length;i++)totals[upper[i]]=100-i;
            for(int i=0;i<lower.length;i++)totals[lower[i]]=80-i;
            set("scottishSplitProvisional",false);set("promotion",null);set("selectedClub",lower[3]);
            call("initialiseTacticsForClub",new Class[0]);
            check((Boolean)call("preparePromotion",new Class[0]),"resolved Scottish standings seed playoffs");
            identities[0]=((String[])type.getField("clubIds").get(world)).clone();
            playerId[0]=lower[3]*20;Object player=call("findPlayer",new Class[]{int.class},playerId[0]);oldTeam[0]=playerInt(player,"team");
            leagueGames[0]=((int[])get("played"))[lower[3]];
            call("startLiveMatchday",new Class[]{boolean.class},true);set("livePaused",true);
            check((Boolean)get("liveMatchActive")&&(Boolean)get("livePlayoff"),"manager can watch a promotion leg");
            int[] colours=(int[])get("primaryColours");
            check(colours[(Integer)get("liveHome")]!=(Integer)get("liveAwayKitColour"),"watched match resolves identical team colours");
            set("liveHomeGoals",2);set("liveAwayGoals",1);call("refreshLiveHeader",new Class[0]);
            check(call("liveTieSummary",new Class[0]).equals("LEG 1 • AGG 2–1"),"first-leg aggregate follows live score");
        });
        capture("promotion-live-leg");
        ui(()->{
            call("finishLiveMatch",new Class[0]);
            check(((int[])get("played"))[(Integer)get("selectedClub")]==leagueGames[0],"playoff does not change league appearances");
            check((Integer)get("matchday")==38,"playoff does not advance regular league round");
            call("loadSave",new Class[]{int.class},1);Object campaign=get("promotion");
            check(campaign!=null,"unfinished playoff survives reload");
            Object tie=campaign.getClass().getMethod("current").invoke(campaign);
            check((Integer)tie.getClass().getMethod("playedLegs").invoke(tie)==1,"first leg survives reload without replay");
            call("startLiveMatchday",new Class[]{boolean.class},true);set("livePaused",true);
            check(call("liveTieSummary",new Class[0]).equals("LEG 2 • AGG 1–2"),"return-leg aggregate uses reversed home-away order");
            call("finishLiveMatch",new Class[0]);
            for(int leg=0;leg<6 && !(Boolean)campaign.getClass().getMethod("complete").invoke(campaign);leg++)call("simulatePromotionLeg",new Class[0]);
            check((Boolean)campaign.getClass().getMethod("complete").invoke(campaign),"Scottish ladder finishes");
            call("showSeasonReview",new Class[0]);
        });
        capture("promotion-final-review");
        ui(()->{
            call("continueDivisionSeason",new Class[0]);check((Integer)get("matchday")==0,"promoted season begins at round zero");
            check(get("promotion")==null,"promotion applied once and cleared");
            call("loadSave",new Class[]{int.class},1);Object world=get("division");Class<?> type=world.getClass();
            check(java.util.Arrays.equals(identities[0],(String[])type.getField("clubIds").get(world)),"season movement preserves all club IDs");
            check(((int[])type.getMethod("members",int.class).invoke(world,1)).length==12&&((int[])type.getMethod("members",int.class).invoke(world,2)).length==10,"Scottish divisions retain correct sizes");
            check(playerInt(call("findPlayer",new Class[]{int.class},playerId[0]),"team")==oldTeam[0],"promotion keeps player's club identity");
            check((Integer)get("matchday")==0&&get("promotion")==null,"promotion transition survives reload");
        });
        check(true,"Scottish watched playoff and season transition verified");
    }

    private void preserveFailureContext()throws Exception {
        if(output==null)return;
        try(FileOutputStream f=new FileOutputStream(new File(output,"report.txt"))){f.write(report.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}
        Object campaign=get("europeanCampaign");if(campaign==null)return;
        Field ready=campaign.getClass().getDeclaredField("materialized");ready.setAccessible(true);if(ready.getBoolean(campaign))return;
        Object qualifying=campaign.getClass().getMethod("qualifying").invoke(campaign);if(qualifying==null||!(Boolean)qualifying.getClass().getMethod("complete").invoke(qualifying))return;
        Field seed=campaign.getClass().getDeclaredField("drawSeed"),previous=campaign.getClass().getDeclaredField("previous");seed.setAccessible(true);previous.setAccessible(true);
        // Preserve the draw input without snapshot(): materialisation is the
        // operation that failed. This contains only the simulated career field.
        java.util.Base64.Encoder encoder=java.util.Base64.getEncoder();
        StringBuilder input=new StringBuilder("UC2|").append(campaign.getClass().getField("year").getInt(campaign)).append('|').append(seed.getLong(campaign)).append('\n');
        input.append(encoder.encodeToString(((String)qualifying.getClass().getMethod("snapshot").invoke(qualifying)).getBytes(java.nio.charset.StandardCharsets.UTF_8))).append('\n');
        input.append(encoder.encodeToString(((String)previous.get(campaign)).getBytes(java.nio.charset.StandardCharsets.UTF_8))).append("\n-");
        for(Object archive:(List<?>)campaign.getClass().getMethod("qualifyingArchives").invoke(campaign))input.append('\n').append(encoder.encodeToString(((String)archive).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        try(FileOutputStream f=new FileOutputStream(new File(output,"uefa-draw-repro.txt"))){f.write(input.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}
        Bundle checkpoint=new Bundle();checkpoint.putString("stream","CHECKPOINT preserved UEFA draw input\n");sendStatus(0,checkpoint);
    }

    private void taggedViews(View view,String tag,List<View> out){if(tag.equals(view.getTag()))out.add(view);if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)taggedViews(((ViewGroup)view).getChildAt(i),tag,out);}
    private void prepareFlowScene(Object pitch,int team)throws Exception{
        int home=(Integer)get("liveHome"),away=(Integer)get("liveAway");boolean forward=team==home;set("livePaused",false);set("livePossessionTeam",team);set("liveCarrierSlot",5);set("livePendingPossessionTeam",-1);set("livePendingCarrierSlot",-1);set("liveLooseBall",false);set("livePendingSetPiece","");set("liveHomePassesComplete",0);set("liveAwayPassesComplete",0);set("liveHomeTackles",0);set("liveAwayTackles",0);set("liveHomeFouls",0);set("liveAwayFouls",0);
        objectCall(objectGet(pitch,"restart"),"step",new Class[]{float.class,boolean.class},1f,true);
        objectSet(pitch,"looseRecovery",false);objectSet(pitch,"deadBallStage",0);objectSet(pitch,"parryPending",false);
        for(String field:new String[]{"homeVX","homeVY","awayVX","awayVY","homeRunTime","awayRunTime"})Arrays.fill((float[])objectGet(pitch,field),0);
        for(int side:new int[]{home,away}){float[] xs=(float[])objectGet(pitch,side==home?"homeX":"awayX"),ys=(float[])objectGet(pitch,side==home?"homeY":"awayY");for(int i=0;i<11;i++){xs[i]=side==team?(forward?.2f:.8f):(forward?.9f:.1f);ys[i]=.15f+i*.065f;}if(side==team){xs[5]=forward?.4f:.6f;ys[5]=.45f;xs[9]=forward?.55f:.45f;ys[9]=.45f;}}
        objectCall(pitch,"holdBallAt",new Class[]{int.class,int.class},team,5);
        ((Map<?,?>)get("matchCompletedPasses")).clear();((Map<?,?>)get("matchMissedPasses")).clear();
        Object receiver=call("playerForPitchSlot",new Class[]{int.class,int.class},team,9);objectSet(receiver,"pace",90);objectSet(receiver,"fitness",100);
    }
    @SuppressWarnings("unchecked") private void verifyMatchFlow()throws Exception{
        SharedPreferences prefs=getTargetContext().getSharedPreferences("project_mister",Context.MODE_PRIVATE);byte[] original=backupBytes(prefs.getAll());
        try{
            ui(()->{seedRulesCareer("ENG",1,true);call("startLiveMatchday",new Class[0]);call("stopLiveMatchTicker",new Class[0]);});
            for(boolean forward:new boolean[]{true,false})ui(()->{
                int home=(Integer)get("liveHome"),away=(Integer)get("liveAway"),team=forward?home:away,other=forward?away:home;Object pitch=get("livePitchView");prepareFlowScene(pitch,team);
                Object passer=call("playerForPitchSlot",new Class[]{int.class,int.class},team,5);int passerId=playerInt(passer,"id");
                objectCall(pitch,"animateBallToSlot",new Class[]{int.class,int.class,int.class,int.class,float.class},team,5,team,9,.85f);objectCall(pitch,"countPassOnReception",new Class[]{int.class,int.class},team,passerId);
                float sx=(Float)objectGet(pitch,"ballStartX"),sy=(Float)objectGet(pitch,"ballStartY"),tx=(Float)objectGet(pitch,"ballTargetX"),ty=(Float)objectGet(pitch,"ballTargetY");
                check(((Map<?,?>)get("matchCompletedPasses")).isEmpty(),"pass is not completed at release "+forward);
                for(int frame=0;frame<102;frame++){
                    if(frame==10)((float[])objectGet(pitch,forward?"homeX":"awayX"))[9]+=forward?.08f:-.08f;
                    objectCall(pitch,"stepSimulation",new Class[]{float.class},1f/120);
                    if((Float)objectGet(pitch,"ballTravel")<1f){
                        check(Math.abs((Float)objectGet(pitch,"ballTargetX")-tx)<.000001&&Math.abs((Float)objectGet(pitch,"ballTargetY")-ty)<.000001,"pass endpoint stays fixed while receiver changes position "+forward+"/"+frame);
                        float bx=(Float)objectGet(pitch,"ballX"),by=(Float)objectGet(pitch,"ballY");check(Math.abs((bx-sx)*(ty-sy)-(by-sy)*(tx-sx))<.00001,"ball follows the released ground trajectory "+forward+"/"+frame);
                        check(((Map<?,?>)get("matchCompletedPasses")).isEmpty(),"airborne pass has no fabricated completion "+forward+"/"+frame);
                    }
                }
                for(int i=0;i<90&&(Boolean)objectGet(pitch,"awaitingReception");i++)objectCall(pitch,"stepSimulation",new Class[]{float.class},1f/120);
                check(!(Boolean)objectGet(pitch,"awaitingReception")&&!(Boolean)get("liveLooseBall")&&(Boolean)objectCall(pitch,"ballIsAt",new Class[]{int.class,int.class,float.class},team,9,.02f),"receiver reaches the fixed destination before controlling the ball "+forward);
                check(((Map<Integer,Integer>)get("matchCompletedPasses")).getOrDefault(passerId,0)==1&&(Integer)get(forward?"liveHomePassesComplete":"liveAwayPassesComplete")==1,"physical reception counts one player and team completion "+forward);
                for(int i=0;i<30;i++)objectCall(pitch,"stepSimulation",new Class[]{float.class},1f/120);
                check(((Map<Integer,Integer>)get("matchCompletedPasses")).getOrDefault(passerId,0)==1,"one reception cannot count twice "+forward);

                prepareFlowScene(pitch,team);objectCall(pitch,"animateBallToSlot",new Class[]{int.class,int.class,int.class,int.class,float.class},team,5,team,9,.60f);objectCall(pitch,"countPassOnReception",new Class[]{int.class,int.class},team,passerId);
                Object receiver=call("playerForPitchSlot",new Class[]{int.class,int.class},team,9);objectSet(receiver,"pace",1);objectSet(receiver,"fitness",35);((float[])objectGet(pitch,forward?"homeX":"awayX"))[9]=forward?.95f:.05f;
                for(int i=0;i<200;i++)objectCall(pitch,"stepSimulation",new Class[]{float.class},1f/120);
                check((Boolean)get("liveLooseBall")&&!(Boolean)objectGet(pitch,"trackTarget")&&!(Boolean)objectGet(pitch,"awaitingReception"),"unreachable pass stays unowned instead of snapping to a receiver "+forward);
                check(((Map<?,?>)get("matchCompletedPasses")).isEmpty()&&((Map<Integer,Integer>)get("matchMissedPasses")).getOrDefault(passerId,0)==1,"missed reception records a missed pass without a completion "+forward);
                float bx=(Float)objectGet(pitch,"ballX"),by=(Float)objectGet(pitch,"ballY");for(int i=0;i<20;i++)objectCall(pitch,"stepSimulation",new Class[]{float.class},1f/120);
                check(Math.abs((Float)objectGet(pitch,"ballX")-bx)<.00001&&Math.abs((Float)objectGet(pitch,"ballY")-by)<.00001,"missed pass waits for a real recovery "+forward);

                prepareFlowScene(pitch,team);
                for(int seed=0;seed<20;seed++){
                    ((Random)get("random")).setSeed(seed);call("performDuelAction",new Class[]{int.class,int.class},team,5);
                    check((Integer)get("livePendingPossessionTeam")==team&&(Integer)get("liveHomeTackles")==0&&(Integer)get("liveAwayTackles")==0&&(Integer)get("liveHomeFouls")==0&&(Integer)get("liveAwayFouls")==0,"distant defender cannot be awarded a tackle or foul "+forward+"/"+seed);
                }
                float[] opponentX=(float[])objectGet(pitch,forward?"awayX":"homeX"),opponentY=(float[])objectGet(pitch,forward?"awayY":"homeY");opponentX[1]=forward?.415f:.585f;opponentY[1]=.45f;
                Object defender=call("playerForPitchSlot",new Class[]{int.class,int.class},other,1);objectSet(defender,"defending",100);objectSet(defender,"physical",100);objectSet(passer,"technique",30);objectSet(passer,"pace",30);((Random)get("random")).setSeed(0);call("performDuelAction",new Class[]{int.class,int.class},team,5);
                check((Integer)get("livePendingPossessionTeam")==other&&(Integer)get(forward?"liveAwayTackles":"liveHomeTackles")==1,"nearby defender can still win a physical challenge "+forward);check(((Map<Integer,Integer>)get("matchTackles")).getOrDefault(playerInt(defender,"id"),0)>0,"physical challenge appears in the player contribution record "+forward);
            });
            check(true,"fixed pass flights physical receptions and contact-range challenges verified");
        }finally{ui(()->{call("stopLiveMatchTicker",new Class[0]);call("stopLiveMatchAudio",new Class[0]);set("matchInProgress",false);gameClass("BackupRestore").getMethod("restore",SharedPreferences.class,byte[].class).invoke(null,prefs,original);check((Boolean)call("loadSave",new Class[]{int.class},0),"original career retained after match-flow checks");});}
    }
    @SuppressWarnings("unchecked") private void verifyPresentation()throws Exception{
        SharedPreferences prefs=getTargetContext().getSharedPreferences("project_mister",Context.MODE_PRIVATE);byte[] original=backupBytes(prefs.getAll());
        try{
            ui(()->{
                seedRulesCareer("ENG",1,true);set("managerFirstName","Luis");set("managerLastName","Jardim");
                for(int round=0;round<7;round++){set("matchday",round);for(Object fixture:(Object[])call("fixturesForRound",new Class[]{int.class},round)){int h=fixture.getClass().getField("home").getInt(fixture),a=fixture.getClass().getField("away").getInt(fixture);int gf=round%3==0?2:round%3==1?1:0,ga=round%3==0?0:round%3==1?1:2;if(a==0){int swap=gf;gf=ga;ga=swap;}call("updateTable",new Class[]{int.class,int.class,int.class,int.class},h,a,gf,ga);}}
                set("matchday",7);call("showLeagueTable",new Class[0]);
            });capture("presentation-five-match-table");
            ui(()->{
                android.widget.HorizontalScrollView scroll=activity.getWindow().getDecorView().findViewWithTag("boss-league-scroll");check(scroll.getChildAt(0).getWidth()-scroll.getWidth()<=Math.round(40*activity.getResources().getDisplayMetrics().density),"table fits phone with at most forty dp overflow");
                check(screenText().contains("LAST 5")&&screenText().contains("GF:GA")&&!screenText().contains("LAST 10"),"condensed standings show five-match form and combined goals");
                List<View> strips=new ArrayList<>();taggedViews(activity.getWindow().getDecorView(),"boss-table-form",strips);check(strips.size()==20,"every club has a form strip");for(View strip:strips)check(((ViewGroup)strip).getChildCount()==5,"form strip contains exactly five recorded or empty slots");
                ViewGroup row=activity.getWindow().getDecorView().findViewWithTag("boss-league-row-0");ViewGroup form=row.findViewWithTag("boss-table-form");check("Loss".contentEquals(form.getChildAt(0).getContentDescription())&&"Win".contentEquals(form.getChildAt(4).getContentDescription()),"form selects the most recent five results oldest to newest");
                seedRulesCareer("ENG",1,true);call("startLiveMatchday",new Class[0]);set("livePaused",true);
            });capture("presentation-stadium-live");
            try(InputStream in=getTargetContext().getAssets().open("match/stadium-daylight-v16.png")){android.graphics.BitmapFactory.Options image=new android.graphics.BitmapFactory.Options();image.inJustDecodeBounds=true;android.graphics.BitmapFactory.decodeStream(in,null,image);check(image.outWidth>=1024&&image.outHeight>=512,"stadium photographic backdrop decodes on Android");}
            ui(()->{View pitch=(View)get("livePitchView");check(pitch.getWidth()>0&&pitch.getHeight()>0,"live stadium has measured dimensions");Object plane=objectGet(pitch,"projection");check(plane!=null,"live players use the shared perspective projection");int team=(Integer)get("liveHome");set("pendingGoalTeam",team);set("pendingGoalSlot",9);call("completePendingGoal",new Class[0]);call("recordCommentary",new Class[]{int.class,String.class},team,"The players return to their positions.");check(((android.widget.TextView)get("liveCommentaryText")).getText().toString().startsWith("GOAL!"),"goal remains highlighted over later commentary");});
            boolean light=false,dark=false;Set<Integer> colours=new HashSet<>();long until=SystemClock.uptimeMillis()+1500;
            while(SystemClock.uptimeMillis()<until&&!(light&&dark)){final int[] phase={-1},colour={0};ui(()->{call("refreshLiveHeader",new Class[0]);phase[0]=(Integer)get("liveGoalFlashPhase");android.graphics.drawable.GradientDrawable bg=(android.graphics.drawable.GradientDrawable)((View)get("liveCommentaryText")).getBackground();int[] palette=bg.getColors();colour[0]=palette==null?0:palette[0];});if(phase[0]==0)dark=true;if(phase[0]==1)light=true;colours.add(colour[0]);SystemClock.sleep(70);}
            check(light&&dark&&colours.size()>1,"goal commentary visibly alternates club and light colours");capture("presentation-flashing-goal");SystemClock.sleep(3800);
            ui(()->{call("refreshLiveHeader",new Class[0]);check((Integer)get("liveGoalFlashPhase")==-1&&((android.widget.TextView)get("liveCommentaryText")).getText().toString().contains("return to their positions"),"flashing stops and normal commentary resumes");set("liveHalfTimeTacticsActive",true);call("showLiveTacticsScreen",new Class[0]);});capture("presentation-half-time");verifyTacticsMarkers();
            ui(()->{
                ViewGroup bench=activity.getWindow().getDecorView().findViewWithTag("boss-live-bench");check(bench.getChildCount()==((List<?>)get("liveBenchIds")).size(),"every bench player has a visible control");for(int i=0;i<bench.getChildCount();i++){android.graphics.Rect visible=new android.graphics.Rect();View child=bench.getChildAt(i);check(child.getGlobalVisibleRect(visible)&&visible.width()>=child.getWidth()-1&&visible.height()>=child.getHeight()-1,"bench player fully visible without horizontal scrolling "+i+" (visible "+visible.width()+"x"+visible.height()+", control "+child.getWidth()+"x"+child.getHeight()+")");}
                android.os.Handler handler=(android.os.Handler)get("goalFlashHandler");check(!handler.hasCallbacks((Runnable)get("goalFlashTick")),"goal animation releases callbacks when tactics opens");
                View formation=activity.getWindow().getDecorView().findViewWithTag("boss-tactic-Formation");check(formation!=null&&formation.performClick(),"formation button opens its real choice dialog");
            });tap("4-4-2");ui(()->check(get("liveFormation").equals("4-4-2"),"formation choice updates live tactics"));verifyTacticsMarkers();
            final int[] off={-1},on={-1};ui(()->{on[0]=((List<Integer>)get("liveBenchIds")).get(0);View bench=activity.getWindow().getDecorView().findViewWithTag("boss-bench-"+on[0]);check(bench.performClick(),"bench selection responds to touch");});
            ui(()->{ViewGroup pitch=activity.getWindow().getDecorView().findViewWithTag("live-tactics-pitch");off[0]=((List<Integer>)get(get("selectedClub").equals(get("liveHome"))?"liveHomeLineupIds":"liveAwayLineupIds")).get(5);check(pitch.getChildAt(6).performClick(),"starter opens substitution confirmation");});tap("Confirm");ui(()->{check((Integer)get("liveSubsUsed")==1&&((List<Integer>)get(get("selectedClub").equals(get("liveHome"))?"liveHomeLineupIds":"liveAwayLineupIds")).contains(on[0])&&!((List<Integer>)get(get("selectedClub").equals(get("liveHome"))?"liveHomeLineupIds":"liveAwayLineupIds")).contains(off[0]),"confirmed bench player replaces the selected starter");});capture("presentation-substitution");ui(()->{check(activity.getWindow().getDecorView().findViewWithTag("boss-tactics-return").performClick(),"start second half responds to touch");check((Boolean)get("liveHalfTimeBreakTaken")&&!(Boolean)get("liveTacticsSessionActive"),"half-time controls resume the actual second half");check((Boolean)objectCall(get("livePitchView"),"restartInProgress",new Class[0]),"second half starts with a physical kickoff reset");});
            for(String country:new String[]{"PT","ENG","ES","IT","DE","FR","NL","BE","SCO","TR"})for(int tier=1;tier<=2;tier++){
                final int level=tier;ui(()->{
                    call("stopLiveMatchTicker",new Class[0]);set("matchInProgress",false);set("liveTacticsSessionActive",false);seedRulesCareer(country,level,true);
                    for(String field:new String[]{"leagueCup","domesticCup","nationalCups","scottishLeagueCup","modernPortugueseCup","europeanCampaign"})set(field,null);
                    Object[] managed=(Object[])call("fixturesForTier",new Class[]{int.class,int.class},level,0);check(managed.length>0,country+level+" has opening fixtures");int club=managed[0].getClass().getField("home").getInt(managed[0]);set("selectedClub",club);call("initialiseTacticsForClub",new Class[0]);
                    Set<String> allowed=new HashSet<>();String[] names=(String[])get("clubNames");Object division=get("division");int[] tiers=(int[])division.getClass().getField("clubTiers").get(division);for(int i=0;i<names.length;i++)if(tiers[i]==level)allowed.add((String)gameClass("GameNames").getMethod("display",String.class).invoke(null,names[i]));
                    Object[] all=(Object[])call("fixturesForRound",new Class[]{int.class},0);call("startLiveMatchday",new Class[0]);check((Boolean)get("matchInProgress"),country+level+" starts actual managed match");set("livePaused",true);set("liveHomeGoals",2);set("liveAwayGoals",1);call("finishLiveMatch",new Class[0]);
                    List<View> rows=new ArrayList<>();taggedViews(activity.getWindow().getDecorView(),"boss-matchday-result",rows);check(rows.size()==managed.length,country+level+" post-match results contain only current division fixture count");for(View result:rows){ViewGroup row=(ViewGroup)result;check(allowed.contains(((android.widget.TextView)row.getChildAt(0)).getText().toString())&&allowed.contains(((android.widget.TextView)row.getChildAt(2)).getText().toString()),country+level+" result clubs belong to managed division");}
                    List<?> stored=(List<?>)objectCall(get("leagueResults"),"results",new Class[0]);check(stored.size()==all.length,country+level+" hidden division fixtures still simulate and save");
                });if(country.equals("ENG"))capture("presentation-full-time-tier-"+level);
            }
            check(true,"all five pitch table result tactics and goal-commentary requests verified");
        }finally{ui(()->{call("stopLiveMatchTicker",new Class[0]);call("stopLiveMatchAudio",new Class[0]);set("matchInProgress",false);set("liveTacticsSessionActive",false);gameClass("BackupRestore").getMethod("restore",SharedPreferences.class,byte[].class).invoke(null,prefs,original);check((Boolean)call("loadSave",new Class[]{int.class},0),"original career retained after presentation checks");});}
    }

    @SuppressWarnings("unchecked") private void verifyRefinements()throws Exception{
        SharedPreferences prefs=getTargetContext().getSharedPreferences("project_mister",Context.MODE_PRIVATE);byte[] original=backupBytes(prefs.getAll());
        try{
            ui(()->{seedRulesCareer("ES",1,true);set("managerFirstName","Luis");set("managerLastName","Jardim");call("saveCurrentGame",new Class[0]);call("showDashboard",new Class[0]);});capture("refine-brand-header");
            ui(()->{
                View logo=activity.getWindow().getDecorView().findViewWithTag("boss-centered-logo"),header=activity.getWindow().getDecorView().findViewWithTag("boss-brand-header");int[] lp=new int[2],hp=new int[2];logo.getLocationOnScreen(lp);header.getLocationOnScreen(hp);check(Math.abs(lp[0]+logo.getWidth()/2f-hp[0]-header.getWidth()/2f)<2,"logo horizontally centred in the main header");
                String raw=((String[])get("clubNames"))[(Integer)get("selectedClub")],alias=(String)gameClass("GameNames").getMethod("display",String.class).invoke(null,raw);check(!alias.equals(raw)&&screenText().contains(alias),"alternate club name visible in existing career UI");
                View pulse=activity.getWindow().getDecorView().findViewWithTag("boss-pulse-league");check(pulse!=null&&pulse.isClickable(),"league-position card is interactive");View page=(View)pulse.getParent();((android.widget.ScrollView)page.getParent()).scrollTo(0,Math.max(0,pulse.getTop()-Math.round(24*activity.getResources().getDisplayMetrics().density)));
            });SystemClock.sleep(350);capture("refine-club-pulse");tap("LEAGUE POSITION  ↗");capture("refine-league-table");
            ui(()->{
                ViewGroup table=activity.getWindow().getDecorView().findViewWithTag("boss-league-grid");check(table!=null&&screenText().contains("Spain 1st Division"),"generic first-division table title");int crests=0;for(int i=0;i<table.getChildCount();i++)if(table.getChildAt(i) instanceof ViewGroup){ViewGroup row=(ViewGroup)table.getChildAt(i);if(row.findViewWithTag("boss-table-crest")!=null)crests++;}check(crests==20,"all twenty table clubs have crests");
                ViewGroup page=activity.getWindow().getDecorView().findViewWithTag("boss-premium-page");View scroller=activity.getWindow().getDecorView().findViewWithTag("boss-league-scroll");int tableIndex=page.indexOfChild(scroller);boolean below=false;for(int i=0;i<page.getChildCount();i++)if(page.getChildAt(i) instanceof android.widget.Button&&((android.widget.Button)page.getChildAt(i)).getText().toString().equals("Spain 2nd Division")){check(i>tableIndex,"division switch is below league table");below=true;}check(below,"second division navigation available");check(screenText().contains("LAST 5")&&screenText().contains("PTS"),"dense table has form and points columns");
                ((android.widget.HorizontalScrollView)scroller).fullScroll(View.FOCUS_RIGHT);
            });capture("refine-table-form");tap("Spain 2nd Division");capture("refine-second-division");
            ui(()->{call("showInbox",new Class[0]);List<Object> news=(List<Object>)get("inbox");Object story=news.get(0);story.getClass().getField("source").set(story,"Sofia Martins • The Saturday Stand • fictional journalist");story.getClass().getField("body").set(story,"A question for the manager. This is a fictional press interview.");call("saveCurrentGame",new Class[0]);check((Boolean)call("loadSave",new Class[]{int.class},2),"new update reloads an existing career");call("showInbox",new Class[0]);check(!screenText().toLowerCase(Locale.ROOT).contains("fictional"),"old and new news bylines have no fictional suffixes");});capture("refine-inbox");
            page("refine-stadium","showStadiumCentre",new Class[0]);ui(()->call("showStadiumPart",new Class[]{int.class},101));tap("Personalise stand");
            ui(()->{((android.widget.EditText)activity.getWindow().getDecorView().findViewWithTag("boss-stand-name")).setText("Supporters End");((android.widget.EditText)activity.getWindow().getDecorView().findViewWithTag("boss-stand-banner")).setText("OUR CLUB OUR HOME");});
            tap("Seat colour: Red");tap("Purple");tap("Seat pattern: Club stripes");tap("Checkerboard");capture("refine-stand-designer");tap("Save stand design");capture("refine-personalised-stand");tap("View stand architecture");capture("refine-stand-architecture");tap("Back to stand");tap("Close");
            ui(()->{call("saveCurrentGame",new Class[0]);check((Boolean)call("loadSave",new Class[]{int.class},2),"personalised career reloads");Object designs=get("standDesigns"),north=designs.getClass().getMethod("get",int.class).invoke(designs,101),east=designs.getClass().getMethod("get",int.class).invoke(designs,102);check(north.getClass().getField("name").get(north).equals("Supporters End")&&north.getClass().getField("banner").get(north).equals("OUR CLUB OUR HOME"),"stand name and banner survive reload");check(north.getClass().getField("colour").getInt(north)==6&&north.getClass().getField("pattern").getInt(north)==2&&east.getClass().getField("colour").getInt(east)==0,"stand palette and pattern remain independent");set("stadiumNorthLevel",4);call("saveCurrentGame",new Class[0]);call("loadSave",new Class[]{int.class},2);check(((String)objectCall(get("standDesigns"),"snapshot",new Class[0])).contains("6,2"),"upgrading architecture retains stand design");call("showStadiumCentre",new Class[0]);call("confirmStandOpenDay",new Class[]{int.class},101);});
            final int[] cash={0};ui(()->cash[0]=(Integer)get("financeBalanceK"));tap("Host open day");ui(()->{check((Integer)get("financeBalanceK")==cash[0]-10,"supporter event charges its displayed cost once");Object north=get("standDesigns").getClass().getMethod("get",int.class).invoke(get("standDesigns"),101);check(north.getClass().getField("openDay").get(north).equals(java.time.YearMonth.from((java.time.LocalDate)get("currentDate")).toString()),"open day has saved monthly cooldown");call("confirmStandOpenDay",new Class[]{int.class},101);});tap("Close");ui(()->check((Integer)get("financeBalanceK")==cash[0]-10,"repeated monthly event has no extra cost or reward"));
            ui(()->{
                call("startLiveMatchday",new Class[0]);call("stopLiveMatchTicker",new Class[0]);set("livePaused",false);Object pitch=get("livePitchView");int frames=0;while((Boolean)objectCall(pitch,"restartInProgress",new Class[0])&&frames++<3000)objectCall(pitch,"stepSimulation",new Class[]{float.class},1f/120);check(frames<3000,"restart completes before physics scenarios");
                int home=(Integer)get("liveHome"),away=(Integer)get("liveAway");String[] clubs=(String[])get("clubNames");String homeAlias=(String)gameClass("GameNames").getMethod("display",String.class).invoke(null,clubs[home]),awayAlias=(String)gameClass("GameNames").getMethod("display",String.class).invoke(null,clubs[away]);for(boolean compact:new boolean[]{true,false}){ViewGroup card=(ViewGroup)call("matchStatsCard",new Class[]{boolean.class},compact),header=(ViewGroup)card.getChildAt(0);String shownHome=((android.widget.TextView)header.getChildAt(0)).getText().toString(),shownAway=((android.widget.TextView)header.getChildAt(1)).getText().toString();check(shownHome.equals(homeAlias)&&shownAway.equals(awayAlias)&&!shownHome.equals(clubs[home])&&!shownAway.equals(clubs[away]),"alternate club names in "+(compact?"live":"expanded")+" match statistics");}float[] hx=(float[])objectGet(pitch,"homeX"),hy=(float[])objectGet(pitch,"homeY"),ax=(float[])objectGet(pitch,"awayX"),ay=(float[])objectGet(pitch,"awayY");
                for(int i=0;i<11;i++){hx[i]=.25f;hy[i]=.75f;ax[i]=.65f;ay[i]=.75f;}hx[9]=.90f;hy[9]=.50f;ax[0]=.70f;ay[0]=.78f;set("livePossessionTeam",home);set("liveCarrierSlot",9);objectCall(pitch,"holdBallAt",new Class[]{int.class,int.class},home,9);
                Object q=call("shotQuality",new Class[]{int.class,int.class},home,9);check(q.getClass().getField("openGoal").getBoolean(q)&&q.getClass().getField("saveChance").getDouble(q)==0,"out-of-position keeper cannot remotely save a shot");
                ((java.util.Random)get("random")).setSeed(1);call("performShotAction",new Class[]{int.class,int.class},home,9);check((Integer)get("pendingGoalTeam")==home,"accurate unopposed open-goal shot enters the goal sequence");
                set("pendingGoalTeam",-1);set("pendingContactCue",null);set("liveNeedsKickoffReset",false);set("livePendingSetPiece","");set("livePendingPossessionTeam",-1);objectCall(pitch,"placeBall",new Class[]{float.class,float.class},.5f,.20f);set("liveLooseBall",true);set("liveLooseBallX",.5f);set("liveLooseBallY",.20f);call("resolveLooseBall",new Class[0]);
                int unmoved=0;for(int i=0;i<2200&&(Boolean)objectGet(pitch,"looseRecovery");i++){float bx=(Float)objectGet(pitch,"ballX"),by=(Float)objectGet(pitch,"ballY");objectCall(pitch,"stepSimulation",new Class[]{float.class},1f/120);if((Boolean)objectGet(pitch,"looseRecovery")){check(Math.abs((Float)objectGet(pitch,"ballX")-bx)<.00001&&Math.abs((Float)objectGet(pitch,"ballY")-by)<.00001,"unowned ball stays still before physical recovery");unmoved++;}}
                check(unmoved>20&&!(Boolean)objectGet(pitch,"looseRecovery")&&!(Boolean)get("liveLooseBall"),"player runs to a loose ball and takes control on contact");
                objectCall(pitch,"placeBall",new Class[]{float.class,float.class},.5f,.015f);call("scheduleSetPiece",new Class[]{String.class,int.class,float.class,float.class},"THROW_IN",home,.5f,.015f);call("performPendingSetPiece",new Class[0]);check((Boolean)objectCall(pitch,"deadBallInProgress",new Class[0]),"throw-in waits for a real taker");
                int waiting=0;for(int i=0;i<3000&&(Boolean)objectCall(pitch,"deadBallInProgress",new Class[0]);i++){boolean collecting=(Integer)objectGet(pitch,"deadBallStage")==1;float bx=(Float)objectGet(pitch,"ballX"),by=(Float)objectGet(pitch,"ballY");objectCall(pitch,"stepSimulation",new Class[]{float.class},1f/120);if(collecting&&(Integer)objectGet(pitch,"deadBallStage")==1){check(Math.abs((Float)objectGet(pitch,"ballX")-bx)<.00001&&Math.abs((Float)objectGet(pitch,"ballY")-by)<.00001,"out-of-play ball does not return without a player");waiting++;}}
                check(waiting>0&&(Boolean)objectGet(pitch,"deadBallReady"),"taker physically reaches and sets the dead ball");float bx=(Float)objectGet(pitch,"ballX"),by=(Float)objectGet(pitch,"ballY");call("performPendingSetPiece",new Class[0]);check(Math.hypot((Float)objectGet(pitch,"ballX")-bx,(Float)objectGet(pitch,"ballY")-by)<.002,"restart delivery starts at actual contact point");
                set("liveLooseBallX",.075f);set("liveLooseBallY",.50f);hx[0]=.07f;hy[0]=.50f;for(int i=1;i<11;i++){hx[i]=.4f;hy[i]=.5f;}check((Integer)call("nearestLooseBallSlot",new Class[]{int.class},home)==0,"keeper is eligible to recover the nearest ball in his own box");set("livePaused",true);
            });capture("refine-live-contact");check(true,"all eight identity presentation stadium and match refinements verified");
        }finally{ui(()->{call("stopLiveMatchTicker",new Class[0]);call("stopLiveMatchAudio",new Class[0]);set("matchInProgress",false);set("liveTacticsSessionActive",false);gameClass("BackupRestore").getMethod("restore",SharedPreferences.class,byte[].class).invoke(null,prefs,original);call("loadSave",new Class[]{int.class},0);});}
    }

    @SuppressWarnings("unchecked") public void onStart(){
        Bundle result=new Bundle();
        try {
            Intent launch=new Intent().setClassName("com.projectmister.game","com.projectmister.game.MainActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity=startActivitySync(launch);waitForIdleSync();
            output=new File(getTargetContext().getExternalFilesDir(null),"qa");output.mkdirs();
            capture("00-startup");
            String mode=args==null?"full":args.getString("mode","full");
            if(mode.equals("migration-export")) {
                SharedPreferences prefs=getTargetContext().getSharedPreferences("project_mister",Context.MODE_PRIVATE);
                byte[] bytes=backupBytes(prefs.getAll());
                gameClass("BackupRestore").getMethod("validate",byte[].class).invoke(null,(Object)bytes);
                try(OutputStream out=new FileOutputStream(new File(getTargetContext().getExternalFilesDir(null),"migration.bossxi"))){out.write(bytes);}
                check(true,"migration backup exported before uninstall");
            } else if(mode.equals("migration-prepare")) {
                check(getTargetContext().getSharedPreferences("project_mister",Context.MODE_PRIVATE).getAll().isEmpty(),"different-key installation starts with empty storage");
            } else if(mode.equals("migration-restore")) {
                SharedPreferences prefs=getTargetContext().getSharedPreferences("project_mister",Context.MODE_PRIVATE);
                byte[] bytes;
                try(InputStream in=new FileInputStream(new File(getTargetContext().getExternalFilesDir(null),"migration.bossxi"))){bytes=(byte[])gameClass("SaveBackup").getMethod("read",InputStream.class).invoke(null,in);}
                gameClass("BackupRestore").getMethod("restore",SharedPreferences.class,byte[].class).invoke(null,prefs,bytes);
                check(Arrays.equals(bytes,backupBytes(prefs.getAll())),"different-key restore preserves every stored field");
                ui(()->{
                    check((Boolean)call("loadSave",new Class[]{int.class},0),"migrated classic career opens");
                    check(get("managerFirstName").equals("Upgrade"),"migrated original manager identity retained");
                    check((Boolean)call("loadSave",new Class[]{int.class},1),"migrated linked career opens");
                    check(get("division")!=null,"migrated competition snapshot retained");
                    call("showDashboard",new Class[0]);
                });
                capture("different-key-restored-career");
                check(true,"different signing key uninstall-install career migration verified");
            } else if(mode.equals("seed")||mode.equals("seed-feedback")) {
                ui(()->{
                    set("selectedSlot",0);set("selectedClub",0);call("initialiseNewManagerDefaults",new Class[0]);
                    set("managerFirstName","Upgrade");set("managerLastName","Test");
                    call("resetCareerState",new Class[0]);call("generatePlayers",new Class[0]);
                    call("initialiseTacticsForClub",new Class[0]);call("initialiseClassicCareerSystems",new Class[0]);
                    if(mode.equals("seed-feedback")){
                        // This gate upgrades from 2.3.13, which can seed both formats.
                        // Keep the original seed compatible with the older baseline gate.
                        set("fixtureVersion",0);call("saveCurrentGame",new Class[0]);
                        seedRulesCareer("PT",1,true);set("selectedSlot",1);set("managerFirstName","UpgradeLinked");call("saveCurrentGame",new Class[0]);
                        call("loadSave",new Class[]{int.class},0);
                    }else call("saveCurrentGame",new Class[0]);
                });
                // Instrumentation.finish ends the process without a normal Activity stop.
                // Flush pending apply() writes before installing the candidate over it.
                SharedPreferences saved=getTargetContext().getSharedPreferences("project_mister",Context.MODE_PRIVATE);
                check(saved.edit().commit(),"baseline save flushed to disk");
                check("Upgrade".equals(saved.getString("save_0_manager_first","")),"baseline persisted manager");
                ui(()->{set("managerFirstName","");call("loadSave",new Class[]{int.class},0);});
                check("Upgrade".equals(get("managerFirstName")),"baseline save reloads");
                check(true,"baseline career seeded");
            } else if(mode.equals("release")) {
                check((getTargetContext().getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)==0,"installed bundle application is non-debuggable");
                ui(()->check((Boolean)call("loadSave",new Class[]{int.class},0),"release loads legacy save"));
                check("Upgrade".equals(get("managerFirstName")),"release preserves legacy manager");
                page("release-dashboard","showDashboard",new Class[0]);
                page("release-squad","showTeamPlayers",new Class[]{int.class},0);
                page("release-player","showPlayerProfile",new Class[]{int.class,int.class},0,0);
                page("release-staff","showStaffHub",new Class[0]);
                page("release-staff-profile","showStaffProfile",new Class[]{String.class},"Assistant Manager");
                page("release-tactics","showTactics",new Class[0]);
                page("release-finance","showFinances",new Class[0]);
                page("release-stadium","showStadiumCentre",new Class[0]);
                ui(()->{call("startLiveMatchday",new Class[0]);set("livePaused",true);});
                capture("release-match");
                ui(()->{set("liveMinute",44);set("liveMinuteFloat",44.98f);set("livePaused",false);});
                awaitBoolean("liveHalfTimeTacticsActive");capture("release-halftime");verifyTacticsMarkers();
                ui(()->{call("stopLiveMatchTicker",new Class[0]);call("stopLiveMatchAudio",new Class[0]);set("matchInProgress",false);
                    check((Boolean)call("loadSave",new Class[]{int.class},1),"release loads linked-country save");});
                Object savedWorld=get("division");
                check(savedWorld!=null&&savedWorld.getClass().getField("linked").getBoolean(savedWorld),"linked country survives release upgrade");
                page("release-linked-table","showLeagueTable",new Class[0]);
                check(true,"release bundle upgrade and feature navigation verified");
            } else if(mode.equals("matchflow")) {
                verifyMatchFlow();
            } else if(mode.equals("presentation")) {
                verifyPresentation();
            } else if(mode.equals("refinements")) {
                verifyRefinements();
            } else if(mode.equals("natural-match")) {
                verifyNaturalMatch();
            } else if(mode.equals("fixes")) {
                verifyRequestedFixes();
            } else if(mode.equals("premium")) {
                verifyPremiumUI();
            } else if(mode.equals("feedback")) {
                verifyFeedback();
            } else if(mode.equals("europe")) {
                verifyEuropeanCareer(args.getString("country","PT"));
            } else if(mode.equals("national-cups")) {
                verifyGermanCupSeasons();
            } else if(mode.equals("expanded-cups")) {
                verifyExpandedCups(args.getString("country","ENG"));
            } else if(mode.equals("rules")) {
                verifyRules();
            } else if(mode.equals("cup-players")) {
                verifyCupPlayers();
            } else if(mode.equals("uefa-players")) {
                verifyUEFAPlayers();
            } else if(mode.equals("divisions")) {
                verifyDivisionCareers();
                verifyBackups();
            } else if(mode.equals("compact")) {
                ui(()->{call("loadSave",new Class[]{int.class},0);call("startLiveMatchday",new Class[0]);set("livePaused",true);});
                ui(()->{int selected=(Integer)get("selectedClub");for(Object p:(List<?>)get("players"))if(playerInt(p,"team")==selected){objectSet(p,"currentAbility",200);objectSet(p,"fitness",100);}});
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
                check((Integer)get("fixtureVersion")==0,"old save fixture order preserved");
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
                verifyCalendarIntegrity();
                verifyWorldMarket();
                verifyPortugueseLeagueCup();
                verifyDomesticCupSeason();
                page("14-board","showClubOffice",new Class[0]);
                page("15-inbox","showInbox",new Class[0]);
                try(InputStream input=getTargetContext().getAssets().open("competitions/2026-27.json")) {
                    Class<?> catalogClass=activity.getClassLoader().loadClass("com.projectmister.game.CompetitionCatalog");
                    Object catalog=catalogClass.getMethod("read",InputStream.class).invoke(null,input);
                    List<?> divisions=(List<?>)catalogClass.getField("divisions").get(catalog);
                    check(divisions.size()==20,"catalog loads all twenty divisions on Android");
                    int clubs=0;for(Object d:divisions)clubs+=((List<?>)d.getClass().getField("clubs").get(d)).size();
                    check(clubs==365,"catalog contains 365 clubs on Android");
                }
                page("28-league-table","showLeagueTable",new Class[0]);
                tap("Qualification & relegation rules");
                check(node("2026/27 finish")!=null,"next-season European qualification context");
                capture("29-qualification-guide");
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
                awaitNaturalMatchPhase(true,2);capture("21-natural-halftime");
                ui(()->call("returnFromLiveTactics",new Class[0]));
                awaitNaturalMatchPhase(false,2);capture("22-natural-fulltime");
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
                ui(()->{
                    set("selectedSlot",2);call("resetCareerState",new Class[0]);
                    check((Integer)get("fixtureVersion")==2,"new career uses shared schedule");
                    Object schedule=call("leagueSchedule",new Class[0]);
                    Method fixture=schedule.getClass().getMethod("fixture",int.class,int.class);
                    for(int round=0;round<34;round++) {
                        Object pairing=fixture.invoke(schedule,round,0);
                        int home=pairing.getClass().getField("home").getInt(pairing);
                        int away=pairing.getClass().getField("away").getInt(pairing);
                        check((Integer)call("leagueOpponentForRound",new Class[]{int.class},round)==(home==0?away:home),"calendar shares opponent round "+round);
                        check((Boolean)call("selectedHomeForRound",new Class[]{int.class},round)==(home==0),"calendar shares venue round "+round);
                    }
                    call("saveCurrentGame",new Class[0]);set("fixtureVersion",0);
                    call("loadSave",new Class[]{int.class},2);
                    check((Integer)get("fixtureVersion")==2,"fixture version survives reload");
                    call("loadSave",new Class[]{int.class},0);
                    check((Integer)get("fixtureVersion")==0,"switching slots restores legacy schedule");
                });
            }
            try(FileOutputStream f=new FileOutputStream(new File(output,"report.txt"))){f.write(report.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}
            result.putString("stream",report.toString());finish(Activity.RESULT_OK,result);
        }catch(Throwable e){
            android.util.Log.e("BOSSXI_QA","Feature regression failed",e);
            try{preserveFailureContext();}catch(Throwable diagnostic){android.util.Log.w("BOSSXI_QA","Could not preserve failure context",diagnostic);}
            result.putString("stream",report+"FAIL "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);
        }
    }
}
