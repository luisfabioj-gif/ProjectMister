package com.projectmister.game;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** KNVB 2026/27 European-ticket playoff, articles 2–5. Future dates are projections. */
public final class DutchEuropeanPlayoff {
    public final int year,cupWinner;
    private final int[] leagueOrder,entrants;
    private final LocalDate semiDate,finalDate;
    private final List<KnockoutTie> ties=new ArrayList<>();

    public DutchEuropeanPlayoff(int year,int[] order,int cupWinner,List<LocalDate> unavailable) {
        this(year,order,cupWinner,unavailable,null);
    }
    public DutchEuropeanPlayoff(int year,int[] order,int cupWinner,List<LocalDate> unavailable,LocalDate earliest) {
        if(year<2026||year>2200||order==null||order.length<8||order.length>24||cupWinner<0||unavailable==null||unavailable.contains(null))throw new IllegalArgumentException("Incomplete Dutch playoff inputs");
        this.year=year;this.cupWinner=cupWinner;leagueOrder=order.clone();Set<Integer> seen=new HashSet<>();
        for(int club:order)if(club<0||club>=CareerLimits.MAX_CLUBS||!seen.add(club))throw new IllegalArgumentException("Invalid Dutch final order");
        int cupRank=index(cupWinner);ArrayList<Integer> field=new ArrayList<>();
        int start=cupRank>=0&&cupRank<3?4:3;
        for(int at=start;at<order.length&&field.size()<4;at++)if(order[at]!=cupWinner)field.add(order[at]);
        if(field.size()!=4)throw new IllegalArgumentException("Incomplete European playoff field");
        entrants=field.stream().mapToInt(Integer::intValue).toArray();
        semiDate=safe(LocalDate.of(year+1,5,27),unavailable,earliest);finalDate=safe(LocalDate.of(year+1,5,30),unavailable,semiDate.plusDays(3));
        ties.add(tie(entrants[0],entrants[3]));ties.add(tie(entrants[1],entrants[2]));
    }
    private DutchEuropeanPlayoff(int year,int[] order,int cupWinner,LocalDate semiDate,LocalDate finalDate) {
        this(year,order,cupWinner,Collections.emptyList());
        // Restore only dates within the season; the original conflict reservations can change later.
        if(semiDate.isBefore(LocalDate.of(year+1,5,20))||finalDate.isAfter(LocalDate.of(year+1,6,30))||ChronoUnit.DAYS.between(semiDate,finalDate)<3)throw new IllegalArgumentException("Invalid playoff dates");
        restoredSemi=semiDate;restoredFinal=finalDate;
    }
    private LocalDate restoredSemi,restoredFinal;
    private static LocalDate safe(LocalDate target,List<LocalDate> blocked,LocalDate earliest) {
        if(earliest!=null&&target.isBefore(earliest))target=earliest;
        for(int offset=0;offset<=28;offset++){LocalDate candidate=target.plusDays(offset);if(candidate.getMonthValue()>6)break;boolean safe=true;for(LocalDate date:blocked)if(Math.abs(ChronoUnit.DAYS.between(candidate,date))<2){safe=false;break;}if(safe)return candidate;}
        throw new IllegalStateException("No Dutch playoff date with recovery before season end");
    }
    private int index(int club){for(int i=0;i<leagueOrder.length;i++)if(leagueOrder[i]==club)return i;return -1;}
    private KnockoutTie tie(int a,int b){int home=index(a)<index(b)?a:b,away=home==a?b:a;return new KnockoutTie(home,away,1,home,false,KnockoutTie.Rule.EXTRA_TIME_PENALTIES);}
    private void prepareFinal(){if(ties.size()==2&&ties.get(0).winner()>=0&&ties.get(1).winner()>=0)ties.add(tie(ties.get(0).winner(),ties.get(1).winner()));}
    public int[] leagueOrder(){return leagueOrder.clone();}
    public int[] entrants(){return entrants.clone();}
    public List<KnockoutTie> ties(){prepareFinal();return Collections.unmodifiableList(ties);}
    public KnockoutTie current(){prepareFinal();for(KnockoutTie tie:ties)if(tie.phase()!=KnockoutTie.Phase.COMPLETE)return tie;return null;}
    public int currentIndex(){KnockoutTie current=current();return current==null?-1:ties.indexOf(current);}
    public boolean complete(){prepareFinal();return ties.size()==3&&ties.get(2).winner()>=0;}
    public int winner(){return complete()?ties.get(2).winner():-1;}
    public String roundName(){return currentIndex()<2?"European ticket semi-final":"European ticket final";}
    public LocalDate semiDate(){return restoredSemi==null?semiDate:restoredSemi;}
    public LocalDate finalDate(){return restoredFinal==null?finalDate:restoredFinal;}
    public LocalDate nextDate(){return complete()?null:currentIndex()<2?semiDate():finalDate();}
    public void validate(String[] identities,boolean[] reserves,int season,LocalDate current) {
        if(year!=season||identities==null||reserves==null||identities.length!=reserves.length||current==null)throw new IllegalArgumentException("Wrong Dutch playoff season");
        for(int club:leagueOrder)if(club>=identities.length||reserves[club]||!identities[club].startsWith("nl:"))throw new IllegalArgumentException("Invalid Dutch playoff club");
        if(cupWinner>=identities.length||reserves[cupWinner]||!identities[cupWinner].startsWith("nl:"))throw new IllegalArgumentException("Invalid Dutch cup winner");
        for(int i=0;i<ties().size();i++)if(ties.get(i).playedLegs()>0&&(i<2?semiDate():finalDate()).isAfter(current))throw new IllegalArgumentException("Dutch playoff result in the future");
        if(!complete()&&nextDate().isBefore(current))throw new IllegalArgumentException("Missed Dutch playoff fixture");
    }
    public String snapshot(){prepareFinal();StringBuilder out=new StringBuilder("NE1|").append(year).append('|').append(cupWinner).append('|').append(semiDate()).append('|').append(finalDate());for(int club:leagueOrder)out.append('|').append(club);for(KnockoutTie tie:ties)out.append('\n').append(Base64.getEncoder().encodeToString(tie.snapshot().getBytes(StandardCharsets.UTF_8)));return out.toString();}
    public static DutchEuropeanPlayoff restore(String text) {
        if(text==null||text.length()>4096)throw new IllegalArgumentException("Invalid Dutch playoff snapshot");String[] rows=text.split("\n",-1),header=rows[0].split("\\|",-1);
        if(header.length<13||header.length>29||!header[0].equals("NE1")||rows.length<3||rows.length>4)throw new IllegalArgumentException("Invalid Dutch playoff schema");
        int[] order=new int[header.length-5];for(int i=0;i<order.length;i++)order[i]=Integer.parseInt(header[i+5]);
        DutchEuropeanPlayoff out=new DutchEuropeanPlayoff(Integer.parseInt(header[1]),order,Integer.parseInt(header[2]),LocalDate.parse(header[3]),LocalDate.parse(header[4]));
        for(int i=1;i<rows.length;i++){out.prepareFinal();if(i>out.ties.size())throw new IllegalArgumentException("Premature Dutch playoff final");KnockoutTie loaded=KnockoutTie.restore(new String(Base64.getDecoder().decode(rows[i]),StandardCharsets.UTF_8)),expected=out.ties.get(i-1);if(loaded.firstHome!=expected.firstHome||loaded.firstAway!=expected.firstAway||loaded.legs!=1||loaded.neutral||loaded.rule!=expected.rule||loaded.higherSeed!=expected.higherSeed)throw new IllegalArgumentException("Changed Dutch playoff draw");out.ties.set(i-1,loaded);}
        if(!out.snapshot().equals(text))throw new IllegalArgumentException("Non-canonical Dutch playoff snapshot");return out;
    }
}
