package com.projectmister.game;
import java.time.*;
import java.util.*;
public final class PostseasonCalendarTests {
    private static void check(boolean value,String text){if(!value)throw new AssertionError(text);}
    public static void main(String[] args) {
        for(int year=2026;year<2040;year++) {
            PromotionCampaign promotion=new ScotlandPromotion(12,11,10,13,14,15);PostseasonCalendar calendar=new PostseasonCalendar(year);
            LocalDate lastLeague=LocalDate.of(year+1,5,10),current=LocalDate.of(year+1,6,1);
            List<LocalDate> blocked=Arrays.asList(LocalDate.of(year+1,6,3),LocalDate.of(year+1,6,8));
            int legs=0;
            while(!promotion.complete()) {
                LocalDate next=calendar.next(promotion,current,lastLeague,blocked);check(!next.isBefore(current),"Postseason never rewinds");
                for(LocalDate occupied:blocked)check(Math.abs(java.time.temporal.ChronoUnit.DAYS.between(next,occupied))>=2,"Postseason avoids cup and UEFA dates");
                check(next.equals(calendar.next(promotion,current,lastLeague,blocked)),"Pending fixture date retained");
                calendar.validate(promotion,year,current,20);calendar=PostseasonCalendar.restore(calendar.snapshot());current=next;
                KnockoutTie tie=promotion.current();tie.recordRegulation(1,0);if(tie.phase()==KnockoutTie.Phase.EXTRA_TIME)tie.recordExtraTime(0,0);if(tie.phase()==KnockoutTie.Phase.PENALTIES)tie.recordPenalties(5,4);
                calendar.validate(promotion,year,current,20);check(calendar.snapshot().equals(PostseasonCalendar.restore(calendar.snapshot()).snapshot()),"Played fixture dates restore exactly");legs++;
            }
            check(legs==6&&calendar.fixtures().size()==6&&!current.isAfter(LocalDate.of(year+1,6,30)),"Complete three-round playoff fits the career season");
            boolean rejected=false;try{PostseasonCalendar.restore(calendar.snapshot().replace("|"+calendar.fixtures().get(0).home+"|","|999|" )).validate(promotion,year,current,20);}catch(IllegalArgumentException expected){rejected=true;}check(rejected,"Altered venue rejected");
        }
        System.out.println("PASS: 14 complete dated playoff campaigns, recovery, pending/played reloads and altered-venue rejection");
    }
}
