package com.projectmister.game;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Saved simulated promotion dates, scheduled alongside domestic/European fixtures. */
public final class PostseasonCalendar {
    public final int year;
    private final ArrayList<Fixture> fixtures=new ArrayList<>();
    public static final class Fixture {
        public final int tie,leg,home,away;public final LocalDate date;
        Fixture(int tie,int leg,int home,int away,LocalDate date){this.tie=tie;this.leg=leg;this.home=home;this.away=away;this.date=date;}
    }
    public PostseasonCalendar(int year){if(year<2026||year>2200)throw new IllegalArgumentException("Invalid postseason year");this.year=year;}
    public List<Fixture> fixtures(){return Collections.unmodifiableList(fixtures);}
    public LocalDate next(PromotionCampaign campaign,LocalDate current,LocalDate lastLeague,Collection<LocalDate> blocked) {
        if(campaign==null||campaign.complete())return null;KnockoutTie tie=campaign.current();int index=campaign.ties().indexOf(tie),leg=tie.playedLegs();
        if(leg>=tie.legs)throw new IllegalStateException("Resolve the current playoff leg first");
        for(Fixture fixture:fixtures)if(fixture.tie==index&&fixture.leg==leg){if(fixture.home!=tie.home()||fixture.away!=tie.away())throw new IllegalStateException("Changed playoff draw");return fixture.date;}
        LocalDate date=lastLeague.plusDays(3);if(!fixtures.isEmpty()&&date.isBefore(fixtures.get(fixtures.size()-1).date.plusDays(2)))date=fixtures.get(fixtures.size()-1).date.plusDays(2);if(date.isBefore(current))date=current;
        for(;!date.isAfter(LocalDate.of(year+1,6,30));date=date.plusDays(1)) {
            boolean safe=true;for(LocalDate other:blocked)if(Math.abs(ChronoUnit.DAYS.between(date,other))<2){safe=false;break;}
            if(safe){fixtures.add(new Fixture(index,leg,tie.home(),tie.away(),date));return date;}
        }
        throw new IllegalStateException("No promotion date before this season ends");
    }
    public void validate(PromotionCampaign campaign,int season,LocalDate current,int clubs) {
        if(campaign==null||year!=season||current==null)throw new IllegalArgumentException("Unexpected postseason calendar");LocalDate previous=null;int key=-1;List<KnockoutTie> ties=campaign.ties();
        for(Fixture fixture:fixtures){int nextKey=fixture.tie*2+fixture.leg;if(nextKey<=key||fixture.tie<0||fixture.tie>=ties.size()||fixture.home<0||fixture.away<0||fixture.home>=clubs||fixture.away>=clubs||fixture.home==fixture.away||fixture.date.isBefore(LocalDate.of(year,8,1))||fixture.date.isAfter(LocalDate.of(year+1,6,30))||previous!=null&&ChronoUnit.DAYS.between(previous,fixture.date)<2)throw new IllegalArgumentException("Invalid postseason fixture");KnockoutTie tie=ties.get(fixture.tie);if(fixture.leg<0||fixture.leg>=tie.legs||tie.homeForLeg(fixture.leg)!=fixture.home||fixture.away!=(fixture.home==tie.firstHome?tie.firstAway:tie.firstHome))throw new IllegalArgumentException("Changed promotion fixture");boolean played=tie.playedLegs()>fixture.leg;if(played&&fixture.date.isAfter(current)||!played&&fixture.date.isBefore(current))throw new IllegalArgumentException("Conflicting postseason result date");previous=fixture.date;key=nextKey;}
        for(int i=0;i<ties.size();i++)for(int leg=0;leg<ties.get(i).playedLegs();leg++){boolean found=false;for(Fixture fixture:fixtures)if(fixture.tie==i&&fixture.leg==leg)found=true;if(!found)throw new IllegalArgumentException("Missing played promotion date");}
    }
    public String snapshot(){StringBuilder out=new StringBuilder("PC1|").append(year);for(Fixture fixture:fixtures)out.append('\n').append(fixture.tie).append('|').append(fixture.leg).append('|').append(fixture.home).append('|').append(fixture.away).append('|').append(fixture.date);return out.toString();}
    public static PostseasonCalendar restore(String text) {
        if(text==null||text.length()>4096)throw new IllegalArgumentException("Invalid postseason dates");String[] rows=text.split("\n",-1),header=rows[0].split("\\|",-1);if(header.length!=2||!header[0].equals("PC1")||rows.length>33)throw new IllegalArgumentException("Invalid postseason schema");PostseasonCalendar out=new PostseasonCalendar(Integer.parseInt(header[1]));
        for(int i=1;i<rows.length;i++){String[] row=rows[i].split("\\|",-1);if(row.length!=5)throw new IllegalArgumentException("Invalid postseason date row");out.fixtures.add(new Fixture(Integer.parseInt(row[0]),Integer.parseInt(row[1]),Integer.parseInt(row[2]),Integer.parseInt(row[3]),LocalDate.parse(row[4])));}
        if(!out.snapshot().equals(text))throw new IllegalArgumentException("Non-canonical postseason dates");return out;
    }
}
