package com.projectmister.game;
public class RefinementTests {
    private static int checks;
    private static void check(boolean value,String name){if(!value)throw new AssertionError(name);checks++;}
    public static void main(String[] args)throws Exception{
        check(GameNames.display("SL Benfica").equals("SL Benfika"),"Portuguese alternate identity");
        check(GameNames.display("Real Madrid 2–1 FC Barcelona").equals("Real Madrida 2–1 FC Barceluna"),"old news is presented with alternate names");
        check(GameNames.display("Roma Wolves").equals("AS Romae"),"older club alias follows the closer display identity");
        check(GameNames.display("Interview with Luis").equals("Interview with Luis"),"Inter alias does not change interview text");
        check(GameNames.display("SL Benfica B").equals("SL Benfika B"),"reserve identity stays distinguishable");
        check(GameNames.display("My Custom Club").equals("My Custom Club"),"custom name retained");
        GameNames.protectPerson("Milan Novak");GameNames.protectPerson("Marco Milan");check(GameNames.display("Milan Novak • GK").equals("Milan Novak • GK"),"player first name is not replaced with club identity");check(GameNames.display("Marco Milan plays the pass").equals("Marco Milan plays the pass"),"player surname is not replaced in commentary");check(GameNames.display("Milan come into the week").equals("AC Milano come into the week"),"club references still change beside protected personal names");
        check(GameNames.display("FC Porto").equals("FC Portu")&&GameNames.display("Arsenal").equals("Arsenel"),"closer recognisable Portuguese and English names");
        check(GameNames.display("Lisbon Eagles v North London Gunners").equals("SL Benfika v Arsenel"),"older saved display names follow updated identities");
        java.lang.reflect.Field nameField=GameNames.class.getDeclaredField("NAMES");nameField.setAccessible(true);
        @SuppressWarnings("unchecked") java.util.Map<String,String> names=(java.util.Map<String,String>)nameField.get(null);
        for(String alias:new java.util.HashSet<>(names.values())){check(GameNames.display(alias).equals(alias),"new alternate name remains stable on repeat rendering: "+alias);check(GameNames.display("Club update: "+alias+" • match report").contains(alias),"alternate name remains stable inside news: "+alias);}
        for(String country:new String[]{"PT","ES","ENG","DE","IT","FR","NL","BE","SCO","TR"})for(int tier=1;tier<=2;tier++)check(GameNames.division(country,tier).endsWith(tier==1?" 1st Division":" 2nd Division"),"generic division "+country+tier);
        check(GameNames.display("Liga Portugal Betclic").equals("Portugal 1st Division"),"legacy top division label");
        check(GameNames.display("Liga Portugal Meu Super").equals("Portugal 2nd Division"),"legacy second division label");
        StandDesigns designs=new StandDesigns();StandDesigns.Design north=designs.get(101);north.name="Eagles End";north.banner="FOR THE CLUB";north.colour=2;north.pattern=3;north.openDay="2026-10";
        StandDesigns saved=StandDesigns.restore(designs.snapshot());check(saved.get(101).name.equals("Eagles End")&&saved.get(101).banner.equals("FOR THE CLUB"),"stand text roundtrip");
        check(saved.get(101).colour==2&&saved.get(101).pattern==3&&saved.get(101).openDay.equals("2026-10"),"stand options and monthly cooldown roundtrip");
        check(saved.get(102).name.isEmpty()&&saved.get(102).colour==0,"each stand independent");
        check(StandDesigns.restore("").get(104).name.isEmpty(),"older saves get default designs");
        check(StandDesigns.text("  A\nB  ").equals("AB"),"stand labels exclude control characters");
        check(StandDesigns.text("a".repeat(100)).length()==36,"stand text bounded");
        boolean invalid=false;try{StandDesigns.restore(designs.snapshot().replace(",2,3,",",99,3,"));}catch(IllegalArgumentException expected){invalid=true;}check(invalid,"invalid palette rejected before restore");
        java.util.Map<String,Object> backup=new java.util.HashMap<>();backup.put("save_0_stad_designs",designs.snapshot());StandDesigns backed=StandDesigns.restore((String)SaveBackup.decode(SaveBackup.encode(backup)).get("save_0_stad_designs"));check(backed.get(101).banner.equals(north.banner)&&backed.get(101).openDay.equals(north.openDay),"stand design survives complete backup serialization");
        float[] x={.5f},y={.5f},vx={0},vy={0};for(int i=0;i<3000;i++)MatchMotion.arrive(x,y,vx,vy,0,.015f,.015f,.21f,.44f,1f/120,.012f);check(Math.hypot(x[0]-.015f,(y[0]-.015f)*.648f)<.009,"restart taker reaches the actual corner");
        x[0]=y[0]=.5f;vx[0]=vy[0]=0;for(int i=0;i<3000;i++)MatchMotion.arrive(x,y,vx,vy,0,.015f,.015f,.21f,.44f,1f/120);check(x[0]>=.035f&&y[0]>=.035f,"normal play keeps its original bounds");
        ShotQuality open=new ShotQuality(true,.90f,.50f,.72f,.75f,true,90,90,0),guarded=new ShotQuality(true,.90f,.50f,.96f,.50f,true,90,90,0);
        check(open.openGoal&&!open.keeperCanReach&&open.saveChance==0,"beaten keeper cannot make remote save");
        check(open.onTarget>.8,"close central open goal rewards accurate finishing");
        check(guarded.keeperCanReach&&guarded.saveChance>.4,"correctly positioned keeper can intervene");
        ShotQuality empty=new ShotQuality(true,.90f,.50f,.96f,.50f,false,90,90,0);check(empty.saveChance==0,"absent keeper cannot save");
        ShotQuality wide=new ShotQuality(true,.90f,.15f,.72f,.75f,true,90,90,0),far=new ShotQuality(true,.60f,.50f,.72f,.75f,true,90,90,0);
        check(wide.angle<open.angle&&wide.onTarget<open.onTarget,"narrow angles reduce shot quality");check(far.distance>open.distance&&far.onTarget<open.onTarget,"distance reduces accuracy");ShotQuality distant=new ShotQuality(true,.5f,.5f,.96f,.5f,true,75,75,0);check(distant.onTarget<.4&&distant.saveChance>.8,"halfway-line shots remain low probability against a set keeper");
        ShotQuality mirror=new ShotQuality(false,.10f,.50f,.28f,.75f,true,90,90,0);check(Math.abs(mirror.onTarget-open.onTarget)<.0001&&mirror.saveChance==open.saveChance,"both ends have symmetric geometry");
        check(ShotQuality.screens(.7f,.5f,.985f,.46f,.85f,.48f),"defender in shot lane detected");check(!ShotQuality.screens(.7f,.5f,.985f,.46f,.6f,.48f),"defender behind shooter cannot block");
        check(InboxNews.pressText("Maya • The Touchline • fictional journalist").equals("Maya • The Touchline"),"old journalist suffix removed");
        check(InboxNews.pressText("Reply below. This is a fictional press interview.").equals("Reply below."),"old interview disclaimer removed");
        System.out.println("PASS "+checks+" identity, stand design, news presentation and shot geometry checks");
    }
}
