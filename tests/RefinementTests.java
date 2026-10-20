package com.projectmister.game;
public class RefinementTests {
    private static int checks;
    private static void check(boolean value,String name){if(!value)throw new AssertionError(name);checks++;}
    public static void main(String[] args){
        check(GameNames.display("SL Benfica").equals("Lisbon Eagles"),"Portuguese alternate identity");
        check(GameNames.display("Real Madrid 2–1 FC Barcelona").equals("Madrid Royals 2–1 Barcelona Blaugrana"),"old news is presented with alternate names");
        check(GameNames.display("Roma Wolves").equals("Roma Wolves"),"alias rendering is idempotent");
        check(GameNames.display("Interview with Luis").equals("Interview with Luis"),"Inter alias does not change interview text");
        check(GameNames.display("SL Benfica B").equals("Lisbon Eagles B"),"reserve identity stays distinguishable");
        check(GameNames.display("My Custom Club").equals("My Custom Club"),"custom name retained");
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
