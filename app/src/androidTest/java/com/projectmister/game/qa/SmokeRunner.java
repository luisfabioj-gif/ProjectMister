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
            dismissEmulatorSystemAnr();
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
    private void collectScreenText(View view,StringBuilder text) {
        if(view instanceof android.widget.TextView)text.append(((android.widget.TextView)view).getText()).append('\n');
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)collectScreenText(((ViewGroup)view).getChildAt(i),text);
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
            set("wageBudgetK",1000000);set("currentDate",java.time.LocalDate.of(2026,10,15));check((Integer)call("signMarketPlayer",new Class[]{String.class},"free~0~6")==-1,"registration gate checked at signing");
            call("loadSave",new Class[]{int.class},0);check(((java.util.List<?>)get("players")).size()==base+21,"all worldwide recruits survive reload once");
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
            check(text.toString().contains("Penalties: "+names[15]+" 4–2 "+names[3]),"playoff penalties identify return-leg home and away clubs");
            check(text.toString().contains("Winner: "+names[15]),"playoff calendar shows the stored winner");
            check(text.toString().contains("dates have not been scheduled"),"playoff calendar does not invent match dates");
            check(before.equals(pc.getMethod("snapshot").invoke(p)),"calendar projection preserves saved playoff state");
        });
        capture("30-playoff-calendar");
        ui(()->{set("currentDate",old[0]);set("selectedClub",old[1]);set("promotion",old[2]);call("showDashboard",new Class[0]);});
        check(true,"calendar recorded results and shootout orientation verified");
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
    private void verifyDivisionCareers() throws Exception {
        Object catalog=get("catalog");check(catalog!=null,"competition catalog available");
        List<?> divisions=(List<?>)catalog.getClass().getField("divisions").get(catalog);
        Class<?> worldClass=activity.getClassLoader().loadClass("com.projectmister.game.CareerDivision");
        for(int index=0;index<divisions.size()*2;index++) {
            final boolean linked=index>=divisions.size();
            Object data=divisions.get(index%divisions.size());String id=(String)data.getClass().getField("id").get(data);
            Object world=linked?worldClass.getMethod("countryCareer",catalog.getClass(),data.getClass()).invoke(null,catalog,data):worldClass.getConstructor(data.getClass()).newInstance(data);
            int count=((List<?>)data.getClass().getField("clubs").get(data)).size();final int leagueIndex=index%divisions.size()+1;
            final int worldCount=((String[])worldClass.getField("names").get(world)).length;
            final int[] memberships=(int[])worldClass.getField("clubTiers").get(world);
            ui(()->{
                call("configureDivision",new Class[]{worldClass},world);set("managerLeagueIndex",leagueIndex);
                set("selectedSlot",1);set("selectedClub",count-1);call("initialiseNewManagerDefaults",new Class[0]);
                set("managerLeagueIndex",leagueIndex);set("managerFirstName","Division");set("managerLastName",id);
                call("resetCareerState",new Class[0]);call("generatePlayers",new Class[0]);
                call("initialiseTacticsForClub",new Class[0]);call("initialiseClassicCareerSystems",new Class[0]);
                check(((List<?>)get("players")).size()==worldCount*20,id+" generates full squads");
                call("saveCurrentGame",new Class[0]);call("showMainMenu",new Class[0]);
            });
            if(id.equals("eng:2")||id.equals("be:2")||id.equals("fr:2")||id.equals("sco:1"))capture("division-"+id.replace(':','-')+"-save");
            ui(()->{
                check((Boolean)call("loadSave",new Class[]{int.class},1),id+" save loads");
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
            } else if(mode.equals("seed")) {
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
            } else if(mode.equals("divisions")) {
                verifyDivisionCareers();
                verifyBackups();
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
            result.putString("stream",report+"FAIL "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);
        }
    }
}
