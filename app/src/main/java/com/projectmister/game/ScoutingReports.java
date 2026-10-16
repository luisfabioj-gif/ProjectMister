package com.projectmister.game;
import java.time.LocalDate;
import java.util.*;

/** Date-based assignments and immutable, dated observations, independent of attribute masking. */
public final class ScoutingReports {
    public static final int MAX_REPORTS=512;
    public static final class Entry {
        public final String key;
        public LocalDate requested,due,reported;
        private List<String[]> report=Collections.emptyList();
        private Entry(String key){this.key=key;}
        public List<String[]> report(){ArrayList<String[]> copy=new ArrayList<>();for(String[] r:report)copy.add(r.clone());return copy;}
        public String title(){return report.isEmpty()?"Player assessment":report.get(0)[1];}
        public boolean pending(){return due!=null;}
    }
    private final TreeMap<String,Entry> entries=new TreeMap<>();
    public Entry get(String key){return entries.get(key);}
    public List<Entry> entries(){ArrayList<Entry> out=new ArrayList<>(entries.values());out.sort((a,b)->{LocalDate x=a.reported==null?a.requested:a.reported,y=b.reported==null?b.requested:b.reported;int c=y.compareTo(x);return c!=0?c:a.key.compareTo(b.key);});return Collections.unmodifiableList(out);}
    public List<Entry> due(LocalDate now){ArrayList<Entry> out=new ArrayList<>();for(Entry e:entries.values())if(e.due!=null&&!e.due.isAfter(now))out.add(e);return out;}
    public boolean assign(String key,LocalDate now){
        validateKey(key,Integer.MAX_VALUE);Entry e=entries.get(key);if(e!=null&&e.pending())return false;
        if(e==null){makeRoom();e=new Entry(key);entries.put(key,e);}e.requested=now;e.due=now.plusDays(7);return true;
    }
    public void record(String key,LocalDate day,List<String[]> report){
        validateKey(key,Integer.MAX_VALUE);validateRows(report);Entry e=entries.get(key);
        if(e==null){makeRoom();e=new Entry(key);e.requested=day;entries.put(key,e);}
        if(e.due!=null&&day.isBefore(e.due))throw new IllegalArgumentException("Scouting is not complete");
        e.due=null;e.reported=day;ArrayList<String[]> rows=new ArrayList<>();for(String[] row:report)rows.add(row.clone());e.report=rows;
    }
    private void makeRoom(){if(entries.size()<MAX_REPORTS)return;Entry oldest=null;for(Entry e:entries.values())if(!e.pending()&&(oldest==null||e.reported.isBefore(oldest.reported)))oldest=e;if(oldest==null)throw new IllegalStateException("All scouts are assigned");entries.remove(oldest.key);}
    public String snapshot(){
        StringBuilder b=new StringBuilder("SR1");for(Entry e:entries.values()){
            StringBuilder rows=new StringBuilder();for(String[] r:e.report){if(rows.length()>0)rows.append('\n');rows.append(PlayerIdentities.text(r[0])).append(',').append(PlayerIdentities.text(r[1]));}
            b.append('\n').append(e.key).append('|').append(e.requested).append('|').append(e.due==null?"-":e.due).append('|').append(e.reported==null?"-":e.reported).append('|').append(PlayerIdentities.text(rows.toString()));
        }return b.toString();
    }
    public static ScoutingReports restore(String raw,int playerCount){
        ScoutingReports book=new ScoutingReports();if(raw==null||raw.isEmpty())return book;
        if(raw.length()>4*1024*1024)throw new IllegalArgumentException("Oversized scouting archive");
        String[] lines=raw.split("\n",-1);if(!lines[0].equals("SR1")||lines.length>MAX_REPORTS+1)throw new IllegalArgumentException("Invalid scouting archive");
        for(int i=1;i<lines.length;i++){
            String[] f=lines[i].split("\\|",-1);if(f.length!=5)throw new IllegalArgumentException("Invalid scouting entry");validateKey(f[0],playerCount);
            Entry e=new Entry(f[0]);e.requested=LocalDate.parse(f[1]);e.due=f[2].equals("-")?null:LocalDate.parse(f[2]);e.reported=f[3].equals("-")?null:LocalDate.parse(f[3]);
            if(e.requested.isBefore(LocalDate.of(2020,1,1))||e.requested.isAfter(LocalDate.of(2200,12,31))||e.due!=null&&!e.due.equals(e.requested.plusDays(7)))throw new IllegalArgumentException("Invalid scouting date");
            String rows=PlayerIdentities.untext(f[4]);ArrayList<String[]> report=new ArrayList<>();if(!rows.isEmpty())for(String row:rows.split("\n",-1)){String[] r=row.split(",",-1);if(r.length!=2)throw new IllegalArgumentException("Invalid report row");report.add(new String[]{PlayerIdentities.untext(r[0]),PlayerIdentities.untext(r[1])});}
            if(e.reported==null?!report.isEmpty():report.isEmpty())throw new IllegalArgumentException("Incomplete report");
            if(!report.isEmpty())validateRows(report);if(e.due==null&&e.reported==null)throw new IllegalArgumentException("Empty scouting entry");e.report=report;
            if(book.entries.put(e.key,e)!=null)throw new IllegalArgumentException("Duplicate scouting assignment");
        }
        if(!book.snapshot().equals(raw))throw new IllegalArgumentException("Noncanonical scouting archive");return book;
    }
    private static void validateKey(String key,int players){
        if(key==null||key.length()>140)throw new IllegalArgumentException("Invalid scouting identity");
        if(key.matches("P[0-9]{1,6}")){int id=Integer.parseInt(key.substring(1));if(id>=players||!key.equals("P"+id))throw new IllegalArgumentException("Unknown scouted player");}
        else if(key.startsWith("M"))new WorldMarket.Candidate(key.substring(1));else throw new IllegalArgumentException("Invalid scouting identity");
    }
    private static void validateRows(List<String[]> rows){if(rows.size()<3||rows.size()>40)throw new IllegalArgumentException("Incomplete report");int bytes=0;for(String[] r:rows){if(r==null||r.length!=2||r[0].isEmpty()||r[0].length()>80||r[1].isEmpty()||r[1].length()>2000||r[0].chars().anyMatch(Character::isISOControl)||r[1].chars().anyMatch(Character::isISOControl))throw new IllegalArgumentException("Invalid report text");bytes+=PlayerIdentities.text(r[0]).length()+PlayerIdentities.text(r[1]).length()+2;}if(bytes>5500)throw new IllegalArgumentException("Oversized report text");}
}
