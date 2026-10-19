package com.projectmister.game;
import java.util.*;
import java.time.LocalDate;
public final class FeedbackTests {
    private static int checks;
    private static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);checks++;}
    public static void main(String[] args){
        List<Recruitment.Row> rows=Arrays.asList(
            new Recruitment.Row("a","Ana Forward","Porto","PT","Portugal","ST",19,70,91,8,14,45,false,false,""),
            new Recruitment.Row("b","Ben Keeper","Free agent","Unattached","England","GK",31,80,82,0,10,100,true,false,""),
            new Recruitment.Row("c","Carl Defender","London","ENG","England","CB",23,75,88,20,32,0,false,false,""),
            new Recruitment.Row("d","Daniel Midfield","Porto","PT","Portugal","CM",16,60,94,2,4,0,false,true,""));
        Recruitment.Filter f=new Recruitment.Filter();check(Recruitment.select(rows,f,0).size()==4,"all database players accessible");
        check(Recruitment.select(rows,f,0).get(0).key.equals("b"),"CA sort");check(Recruitment.select(rows,f,1).get(0).key.equals("d"),"PA sort");
        check(Recruitment.select(rows,f,2).get(0).key.equals("d"),"age ascending");check(Recruitment.select(rows,f,3).get(0).key.equals("b"),"age descending");check(Recruitment.select(rows,f,4).get(0).key.equals("b"),"signing chance sort");check(Recruitment.select(rows,f,5).get(0).free,"free fee first");check(Recruitment.select(rows,f,6).get(0).key.equals("d"),"wage sort");
        f.query="porto portugal";f.maxAge=21;f.minCurrent=65;f.minPotential=90;f.maxFee=9;f.maxWage=15;f.minChance=40;f.country="PT";f.position="Forwards";f.club="por";check(Recruitment.select(rows,f,0).size()==1&&Recruitment.select(rows,f,0).get(0).key.equals("a"),"all filters compose");
        f.availableOnly=true;check(Recruitment.select(rows,f,0).size()==1,"attainable combined filter");f.minChance=46;check(Recruitment.select(rows,f,0).isEmpty(),"chance threshold applies");
        f=new Recruitment.Filter();f.position="Goalkeepers";check(Recruitment.select(rows,f,0).size()==1,"free agent position filter");f.availableOnly=true;f.maxFee=0;check(Recruitment.select(rows,f,0).size()==1,"free agent zero fee filter");
        f=new Recruitment.Filter();f.availableOnly=true;check(Recruitment.select(rows,f,0).size()==2,"owned and unavailable excluded");
        InboxNews.Item legacy=InboxNews.decode("BOARD\u001fOld board message\u001fKept exactly").get(0);check(legacy.date==null&&legacy.body.equals("Kept exactly"),"legacy undated news retained without invented dates");
        InboxNews.Item interview=new InboxNews.Item("INTERVIEW","A question","How will you prepare?",LocalDate.of(2026,8,9));interview.action="interview:2026-08-03";interview.source="The Touchline • fictional journalist";
        check(InboxNews.reply(interview,"Support"),"interview answer accepted");check(!InboxNews.reply(interview,"Demand"),"interview cannot reward twice");
        String encoded=InboxNews.encode(Arrays.asList(interview,legacy));List<InboxNews.Item> restored=InboxNews.decode(encoded);
        check(restored.size()==2&&restored.get(0).read&&restored.get(0).date.equals(interview.date)&&restored.get(0).response.equals("Support"),"news date read response survive saving");check(InboxNews.encode(restored).equals(encoded),"news exact round trip");check(InboxNews.hasAction(restored,interview.action),"weekly editions deduplicate after reload");
        MatchRestart restart=new MatchRestart();check(!restart.active(),"normal match active");restart.goal();for(int i=0;i<300;i++)restart.step(.01f,true);check(restart.celebrating(),"players celebrate before reset");for(int i=0;i<50;i++)restart.step(.01f,false);check(restart.resetting(),"goal transitions to physical reset");for(int i=0;i<1000;i++)restart.step(.01f,false);check(restart.active(),"time alone never allows restart before players arrive");for(int i=0;i<20;i++)restart.step(.01f,true);check(restart.active(),"settling interval enforced");restart.step(.01f,false);for(int i=0;i<20;i++)restart.step(.01f,true);check(restart.active(),"readiness must be consecutive");for(int i=0;i<10;i++)restart.step(.01f,true);check(!restart.active(),"kickoff after players settle");
        for(boolean home:new boolean[]{true,false})for(int i=0;i<11;i++){float x=MatchRestart.x(home?.75f:.25f,home,i,false);check(home?x<.5f:x>.5f,"kickoff team starts in own half");}
        System.out.println("PASS "+checks+" recruitment, dated inbox migration, one-time interviews and physical restart checks");
    }
}
