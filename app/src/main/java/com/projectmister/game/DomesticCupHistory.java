package com.projectmister.game;

import java.nio.charset.StandardCharsets;
import java.util.*;

/** Completed editions retained without remapping the career's stable club indices. */
public final class DomesticCupHistory {
    private final List<String> editions;
    public DomesticCupHistory(){editions=Collections.emptyList();}
    private DomesticCupHistory(List<String> editions){this.editions=Collections.unmodifiableList(new ArrayList<>(editions));}
    public List<String> editions(){return editions;}
    public DomesticCupHistory append(DomesticCup cup) {
        if(cup==null||!cup.complete()||editions.size()>=100)throw new IllegalArgumentException("Cup is incomplete or history is full");
        if(!editions.isEmpty()) {
            String previous=editions.get(editions.size()-1),header=previous.split("\n",2)[0];String[] h=header.split(";");
            int year=h[0].equals("TACA26")?2026:Integer.parseInt(h[1]);
            if(cup.season<=year)throw new IllegalArgumentException("Duplicate or older cup edition");
        }
        List<String> next=new ArrayList<>(editions);next.add(cup.snapshot());DomesticCupHistory history=new DomesticCupHistory(next);
        if(history.snapshot().length()>2*1024*1024)throw new IllegalArgumentException("Cup archive is full");return history;
    }
    public String snapshot(){StringBuilder out=new StringBuilder("TCH1");for(String edition:editions)out.append('\n').append(Base64.getEncoder().encodeToString(edition.getBytes(StandardCharsets.UTF_8)));return out.toString();}
    public static DomesticCupHistory restore(String text,int worldSize,boolean[] reserves,int activeYear) {
        if(text==null||text.isEmpty())return new DomesticCupHistory();
        if(text.length()>2*1024*1024)throw new IllegalArgumentException("Oversized cup archive");
        String[] rows=text.split("\n",-1);if(!rows[0].equals("TCH1")||rows.length>101)throw new IllegalArgumentException("Invalid cup archive");
        DomesticCupHistory history=new DomesticCupHistory();
        for(int i=1;i<rows.length;i++) {
            DomesticCup cup=DomesticCup.restore(new String(Base64.getDecoder().decode(rows[i]),StandardCharsets.UTF_8),worldSize);cup.validateEntrants(reserves);
            if(cup.season>=activeYear)throw new IllegalArgumentException("Archive overlaps active cup");history=history.append(cup);
        }
        if(!history.snapshot().equals(text))throw new IllegalArgumentException("Non-canonical cup archive");return history;
    }
}
