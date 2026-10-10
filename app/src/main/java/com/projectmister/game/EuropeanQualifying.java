package com.projectmister.game;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

/** Concurrent two-leg qualifiers. Losers follow Article 3; eliminated clubs are never reinstated. */
public final class EuropeanQualifying {
    public static final class Draw {
        public final EuropeanAdmissions.Path path;public final int round;public final List<Integer> byes;public final List<KnockoutTie> ties;private final List<KnockoutTie> mutableTies;
        Draw(EuropeanAdmissions.Path path,int round,List<Integer> byes,List<KnockoutTie> ties){this.path=path;this.round=round;this.byes=Collections.unmodifiableList(byes);mutableTies=ties;this.ties=Collections.unmodifiableList(ties);}
    }
    public static final class Event {
        public final Draw draw;public final int index;public final KnockoutTie tie;public final LocalDate date;
        Event(Draw draw,int index,LocalDate date){this.draw=draw;this.index=index;tie=draw.ties.get(index);this.date=date;}
        public boolean contains(int club){return tie.firstHome==club||tie.firstAway==club;}
        public EuropeanLeaguePhase.Competition competition(){return EuropeanAdmissions.competition(draw.path);}
    }
    private final EuropeanAdmissions admissions;
    private final long seed;
    private final List<LocalDate> domestic,calendar;
    private final List<Draw> draws=new ArrayList<>();
    private final List<List<List<Integer>>> pools=new ArrayList<>();
    private final EnumMap<EuropeanLeaguePhase.Competition,List<Integer>> fields=new EnumMap<>(EuropeanLeaguePhase.Competition.class);
    private int round=1;
    public EuropeanQualifying(EuropeanAdmissions admissions,long seed){this(admissions,seed,Collections.emptyList());}
    public EuropeanQualifying(EuropeanAdmissions admissions,long seed,List<LocalDate> domestic){
        if(admissions==null)throw new IllegalArgumentException("Missing annual UEFA access");this.admissions=admissions;this.seed=seed;this.domestic=EuropeanCalendar.reserved(admissions.year,domestic);calendar=EuropeanCalendar.qualifying(admissions.year,this.domestic);
        for(EuropeanAdmissions.Path p:EuropeanAdmissions.Path.values()){List<List<Integer>> rounds=new ArrayList<>();for(int i=0;i<6;i++)rounds.add(new ArrayList<>());pools.add(rounds);}
        for(EuropeanLeaguePhase.Competition c:EuropeanLeaguePhase.Competition.values())fields.put(c,new ArrayList<>());
        for(EuropeanAdmissions.Entry e:admissions.entries())if(e.round==5)fields.get(e.competition()).add(e.club);else pools.get(e.path.ordinal()).get(e.round).add(e.club);
        drawRound();
    }
    public EuropeanAdmissions admissions(){return admissions;}
    public List<LocalDate> domesticCalendar(){return domestic;}
    public List<LocalDate> calendar(){return calendar;}
    public List<Draw> draws(){advance();return Collections.unmodifiableList(draws);}
    public static String roundName(int round){return round==4?"Qualifying play-off":"Qualifying round "+round;}
    public static String pathName(EuropeanAdmissions.Path path){return path==EuropeanAdmissions.Path.CL_CH||path==EuropeanAdmissions.Path.EL_CH||path==EuropeanAdmissions.Path.CO_CH?"Champions path":"League/main path";}
    public LocalDate date(Draw draw,int leg){if(leg<0||leg>1)throw new IllegalArgumentException("Invalid qualifying leg");return calendar.get(((draw.round-1)*2+leg)*2+(draw.path.ordinal()<2?0:1));}
    public Event next(int managedClub){advance();Event result=null;for(Draw draw:draws)if(draw.round==round)for(int i=0;i<draw.ties.size();i++){KnockoutTie t=draw.ties.get(i);if(t.winner()>=0)continue;Event e=new Event(draw,i,date(draw,Math.min(t.playedLegs(),1)));if(result==null||e.date.isBefore(result.date)||e.date.equals(result.date)&&e.contains(managedClub)&&!result.contains(managedClub))result=e;}return result;}
    public boolean complete(){advance();return round==5;}
    public List<Integer> field(EuropeanLeaguePhase.Competition competition){if(!complete())throw new IllegalStateException("Qualifying is not complete");return Collections.unmodifiableList(fields.get(competition));}
    private void advance(){
        while(round<5){for(Draw draw:draws)if(draw.round==round)for(KnockoutTie tie:draw.ties)if(tie.winner()<0)return;
            for(Draw draw:draws)if(draw.round==round){for(int club:draw.byes)winner(draw.path,round,club);for(KnockoutTie tie:draw.ties){winner(draw.path,round,tie.winner());EuropeanAdmissions.Destination d=EuropeanAdmissions.loserDestination(draw.path,round);if(d!=null)admit(d.path,d.round,tie.loser());}}
            round++;if(round<5)drawRound();
        }
        Set<Integer> all=new HashSet<>();for(List<Integer> field:fields.values()){if(field.size()!=36)throw new IllegalStateException("Incomplete UEFA qualified field: "+field.size());for(int club:field)if(!all.add(club))throw new IllegalStateException("Club qualified for multiple UEFA league phases");}
    }
    private void winner(EuropeanAdmissions.Path path,int round,int club){admit(path,round+1,club);}
    private void admit(EuropeanAdmissions.Path path,int round,int club){(round==5?fields.get(EuropeanAdmissions.competition(path)):pools.get(path.ordinal()).get(round)).add(club);}
    private void drawRound(){
        for(EuropeanAdmissions.Path path:EuropeanAdmissions.Path.values()) {
            ArrayList<Integer> field=new ArrayList<>(pools.get(path.ordinal()).get(round));if(field.isEmpty())continue;
            field.sort((a,b)->{int c=Integer.compare(admissions.coefficients[b],admissions.coefficients[a]);return c!=0?c:Integer.compare(a,b);});List<Integer> byes=new ArrayList<>();if(field.size()%2==1)byes.add(field.remove(0));
            int half=field.size()/2;List<Integer> high=new ArrayList<>(field.subList(0,half)),low=new ArrayList<>(field.subList(half,field.size()));Random random=new Random(seed+round*7919L+path.ordinal()*104729L);Collections.shuffle(high,random);Collections.shuffle(low,random);int[] opponents=new int[half];Arrays.fill(opponents,-1);int[] nodes={0};
            if(!pair(0,high,low,new boolean[half],opponents,nodes))throw new IllegalStateException("No association-safe qualifying draw: "+path+" Q"+round);
            ArrayList<KnockoutTie> ties=new ArrayList<>();for(int i=0;i<half;i++){int h=high.get(i),l=low.get(opponents[i]);boolean highHome=random.nextBoolean();ties.add(new KnockoutTie(highHome?h:l,highHome?l:h,2,h,false,KnockoutTie.Rule.EXTRA_TIME_PENALTIES));}
            draws.add(new Draw(path,round,byes,ties));
        }
    }
    private boolean pair(int at,List<Integer> high,List<Integer> low,boolean[] used,int[] opponents,int[] nodes){if(at==high.size())return true;if(++nodes[0]>200000)throw new IllegalStateException("Qualifying draw search exhausted");String association=EuropeanDomesticSeason.association(admissions.identities[high.get(at)]);for(int i=0;i<low.size();i++)if(!used[i]&&!association.equals(EuropeanDomesticSeason.association(admissions.identities[low.get(i)]))){used[i]=true;opponents[at]=i;if(pair(at+1,high,low,used,opponents,nodes))return true;used[i]=false;}return false;}
    public int competitionForClub(int club){EuropeanAdmissions.Entry e=admissions.entry(club);if(e==null)return -1;if(e.round==5)return e.competition().ordinal();advance();for(int i=draws.size()-1;i>=0;i--){Draw draw=draws.get(i);for(int bye:draw.byes)if(bye==club)return EuropeanAdmissions.competition(draw.path).ordinal();for(KnockoutTie tie:draw.ties)if(tie.firstHome==club||tie.firstAway==club){if(tie.winner()!=club&&tie.winner()>=0){EuropeanAdmissions.Destination d=EuropeanAdmissions.loserDestination(draw.path,draw.round);return d==null?-1:EuropeanAdmissions.competition(d.path).ordinal();}return EuropeanAdmissions.competition(draw.path).ordinal();}}return e.competition().ordinal();}
    public void validate(String[] ids,boolean[] reserves,int year,LocalDate now){if(admissions.year!=year||!Arrays.equals(ids,admissions.identities)||!Arrays.equals(reserves,admissions.reserves)||now==null)throw new IllegalArgumentException("Changed qualifying career registry");for(Draw draw:draws)for(KnockoutTie tie:draw.ties)for(int leg=0;leg<tie.playedLegs();leg++)if(date(draw,leg).isAfter(now))throw new IllegalArgumentException("Future UEFA qualifying result");Event event=next(-1);if(event!=null&&event.date.isBefore(now))throw new IllegalArgumentException("Overdue UEFA qualifying fixture");}
    public int[] associationMatchPoints(){int[] points=new int[admissions.identities.length];for(Draw draw:draws)for(KnockoutTie tie:draw.ties)for(int leg=0;leg<tie.playedLegs();leg++){int h=tie.homeForLeg(leg),a=h==tie.firstHome?tie.firstAway:tie.firstHome;int[] score=tie.regulationScore(leg),extra=tie.extraTimeScore();if(leg==1&&extra[0]>=0){score[0]+=extra[0];score[1]+=extra[1];}EuropeanPerformance.award(points,h,a,score[0],score[1],1000);}return points;}
    private static String encode(String value){return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));}
    private static String decode(String value){return new String(Base64.getDecoder().decode(value),StandardCharsets.UTF_8);}
    public String snapshot(){advance();StringBuilder out=new StringBuilder(domestic.isEmpty()?"UQ1|":"UQ2|").append(seed);if(!domestic.isEmpty()){out.append('|');for(int i=0;i<domestic.size();i++){if(i>0)out.append(',');out.append(domestic.get(i));}}out.append('\n').append(encode(admissions.snapshot()));for(int d=0;d<draws.size();d++)for(int t=0;t<draws.get(d).ties.size();t++){KnockoutTie tie=draws.get(d).ties.get(t);if(tie.playedLegs()>0)out.append("\nT|").append(d).append('|').append(t).append('|').append(tie.snapshot());}return out.toString();}
    public static EuropeanQualifying restore(String text){
        if(text==null||text.length()>1024*1024)throw new IllegalArgumentException("Oversized UEFA qualifying state");String[] rows=text.split("\n",-1),h=rows[0].split("\\|",-1);if(!(h.length==2&&h[0].equals("UQ1")||h.length==3&&h[0].equals("UQ2"))||rows.length<2||rows.length>1024)throw new IllegalArgumentException("Invalid UEFA qualifying state");ArrayList<LocalDate> domestic=new ArrayList<>();if(h.length==3)for(String date:h[2].split(",",-1))domestic.add(LocalDate.parse(date));EuropeanQualifying out=new EuropeanQualifying(EuropeanAdmissions.restore(decode(rows[1])),Long.parseLong(h[1]),domestic);int previous=-1;
        for(int i=2;i<rows.length;i++){String[] t=rows[i].split("\\|",4);if(t.length!=4||!t[0].equals("T"))throw new IllegalArgumentException("Invalid qualifying result");out.advance();int d=Integer.parseInt(t[1]),at=Integer.parseInt(t[2]),key=d*1024+at;if(key<=previous||d<0||d>=out.draws.size()||at<0||at>=out.draws.get(d).ties.size())throw new IllegalArgumentException("Qualifying result outside draw");Draw draw=out.draws.get(d);KnockoutTie expected=draw.ties.get(at),saved=KnockoutTie.restore(t[3]);if(saved.firstHome!=expected.firstHome||saved.firstAway!=expected.firstAway||saved.higherSeed!=expected.higherSeed||saved.legs!=2||saved.neutral||saved.rule!=KnockoutTie.Rule.EXTRA_TIME_PENALTIES||saved.playedLegs()==0)throw new IllegalArgumentException("Changed qualifying tie");draw.mutableTies.set(at,saved);previous=key;}
        // Draw.ties is an unmodifiable view; restoration updates its owned backing list above.
        if(!text.equals(out.snapshot()))throw new IllegalArgumentException("Non-canonical UEFA qualifying save");return out;
    }
}
