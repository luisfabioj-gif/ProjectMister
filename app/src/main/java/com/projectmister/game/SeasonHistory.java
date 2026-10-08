package com.projectmister.game;

import java.io.*;
import java.time.LocalDate;
import java.util.*;

/** Immutable, bounded summaries of completed managed-club seasons. No inferred rankings. */
public final class SeasonHistory {
    private static final int MAGIC=0x42534831, MAX=1000;
    public static final class Entry {
        public final String start,clubId,clubName,league;
        public final int tier,nextTier,played,won,drawn,lost,gf,ga,points;
        public Entry(String start,String clubId,String clubName,String league,int tier,int nextTier,
                     int played,int won,int drawn,int lost,int gf,int ga,int points) {
            LocalDate.parse(start);
            for(String s:new String[]{start,clubId,clubName,league})if(s==null||s.isEmpty()||s.length()>128)throw new IllegalArgumentException("Invalid history text");
            if(tier<1||tier>2||nextTier<1||nextTier>20)throw new IllegalArgumentException("Invalid history tier");
            for(int n:new int[]{played,won,drawn,lost,gf,ga})if(n<0||n>100000)throw new IllegalArgumentException("Invalid history statistics");
            if(won+drawn+lost!=played||points < -100000||points>100000)throw new IllegalArgumentException("Invalid history totals");
            this.start=start;this.clubId=clubId;this.clubName=clubName;this.league=league;
            this.tier=tier;this.nextTier=nextTier;this.played=played;this.won=won;this.drawn=drawn;
            this.lost=lost;this.gf=gf;this.ga=ga;this.points=points;
        }
        public String outcome(){return tier==nextTier?"Stayed in division":nextTier<tier?"Promoted":"Relegated";}
    }
    private final List<Entry> entries;
    public SeasonHistory(){entries=Collections.emptyList();}
    private SeasonHistory(List<Entry> values){entries=Collections.unmodifiableList(new ArrayList<>(values));}
    public List<Entry> entries(){return entries;}
    public SeasonHistory append(Entry entry) {
        if(entries.size()>=MAX)throw new IllegalArgumentException("Season history is full");
        if(!entries.isEmpty()&&!LocalDate.parse(entry.start).isAfter(LocalDate.parse(entries.get(entries.size()-1).start)))
            throw new IllegalArgumentException("Season already archived or out of order");
        List<Entry> next=new ArrayList<>(entries);next.add(entry);return new SeasonHistory(next);
    }
    public String snapshot() {
        try {
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
            out.writeInt(MAGIC);out.writeInt(entries.size());
            for(Entry e:entries){out.writeUTF(e.start);out.writeUTF(e.clubId);out.writeUTF(e.clubName);out.writeUTF(e.league);
                for(int n:new int[]{e.tier,e.nextTier,e.played,e.won,e.drawn,e.lost,e.gf,e.ga,e.points})out.writeInt(n);}
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        }catch(IOException e){throw new IllegalStateException(e);}
    }
    public static SeasonHistory restore(String text) {
        if(text.isEmpty())return new SeasonHistory();
        if(text.length()>4000000)throw new IllegalArgumentException("History too large");
        try {
            DataInputStream in=new DataInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(text)));
            if(in.readInt()!=MAGIC)throw new IOException("Unknown history version");
            int count=in.readInt();if(count<0||count>MAX)throw new IOException("Invalid history count");
            SeasonHistory result=new SeasonHistory();
            for(int i=0;i<count;i++)result=result.append(new Entry(in.readUTF(),in.readUTF(),in.readUTF(),in.readUTF(),
                in.readInt(),in.readInt(),in.readInt(),in.readInt(),in.readInt(),in.readInt(),in.readInt(),in.readInt(),in.readInt()));
            if(in.available()!=0)throw new IOException("Unexpected history data");return result;
        }catch(IOException|RuntimeException e){throw new IllegalArgumentException("Invalid season history",e);}
    }
}
