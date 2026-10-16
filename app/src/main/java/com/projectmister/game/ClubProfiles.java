package com.projectmister.game;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;

/** Dated public observations, with game estimates explicitly separate from observed facts. */
public final class ClubProfiles {
    public static final class Profile {
        public final String id,name,country,stadium;
        public final int primary,secondary,strength,budgetK,capacity,rosterSize;
        public final long squadValueEUR;
        public final SortedMap<String,Integer> nationalities;
        Profile(JSONObject o)throws Exception {
            id=o.getString("id");name=o.getString("name");country=o.getString("country");
            if(!id.matches("[a-z]{2,4}:[a-z0-9-]+")||name.isEmpty())throw new IllegalArgumentException("Invalid club profile");
            primary=colour(o.getString("primary"));secondary=colour(o.getString("secondary"));
            strength=o.getInt("strengthEstimate");budgetK=o.getInt("transferBudgetEstimateK");
            squadValueEUR=o.getLong("marketValueEUR");rosterSize=o.getInt("rosterSize");
            stadium=o.optString("stadium","");capacity=o.optInt("capacity",0);
            if(strength<1||strength>99||budgetK<0||squadValueEUR<=0||rosterSize<12||rosterSize>100||capacity<0||capacity>150000)throw new IllegalArgumentException("Invalid club observations");
            TreeMap<String,Integer> counts=new TreeMap<>();JSONObject nations=o.getJSONObject("nationalities");
            Iterator<String> keys=nations.keys();int sum=0;
            while(keys.hasNext()){String n=keys.next();int count=nations.getInt(n);if(n.isEmpty()||n.length()>70||count<1||count>100)throw new IllegalArgumentException("Invalid nationality count");counts.put(n,count);sum+=count;}
            if(sum!=rosterSize)throw new IllegalArgumentException("Incomplete nationality observation");
            nationalities=Collections.unmodifiableSortedMap(counts);
        }
        /** Largest-remainder apportionment: closest possible mix for a fixed-size fictional squad. */
        public List<String> nationalitySlots(int size) {
            if(size<1||size>100)throw new IllegalArgumentException("Invalid squad size");
            ArrayList<String> nations=new ArrayList<>(nationalities.keySet());int[] counts=new int[nations.size()];int assigned=0;
            for(int i=0;i<counts.length;i++){counts[i]=nationalities.get(nations.get(i))*size/rosterSize;assigned+=counts[i];}
            ArrayList<Integer> fractions=new ArrayList<>();for(int i=0;i<counts.length;i++)fractions.add(i);
            fractions.sort((a,b)->{int c=Integer.compare(nationalities.get(nations.get(b))*size%rosterSize,nationalities.get(nations.get(a))*size%rosterSize);return c!=0?c:nations.get(a).compareTo(nations.get(b));});
            for(int i=0;i<size-assigned;i++)counts[fractions.get(i)]++;
            ArrayList<String> slots=new ArrayList<>();for(int i=0;i<counts.length;i++)for(int j=0;j<counts[i];j++)slots.add(nations.get(i));
            Collections.shuffle(slots,new Random(0xB055C0L+id.hashCode()));return slots;
        }
    }
    private final Map<String,Profile> clubs;
    public final String verifiedOn;
    private ClubProfiles(JSONObject root)throws Exception {
        if(root.getInt("schema")!=1)throw new IllegalArgumentException("Unsupported club profiles");
        verifiedOn=root.getString("verifiedOn");java.time.LocalDate.parse(verifiedOn);
        TreeMap<String,Profile> values=new TreeMap<>();JSONArray rows=root.getJSONArray("clubs");
        for(int i=0;i<rows.length();i++){Profile p=new Profile(rows.getJSONObject(i));if(values.put(p.id,p)!=null)throw new IllegalArgumentException("Duplicate club profile");}
        clubs=Collections.unmodifiableMap(values);
    }
    public Profile get(String id){return clubs.get(id);}
    public int size(){return clubs.size();}
    public static ClubProfiles read(InputStream in)throws Exception {
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int n;
        while((n=in.read(buffer))!=-1){if(out.size()+n>2*1024*1024)throw new IllegalArgumentException("Oversized club profiles");out.write(buffer,0,n);}
        return new ClubProfiles(new JSONObject(new String(out.toByteArray(),StandardCharsets.UTF_8)));
    }
    private static int colour(String value){if(!value.matches("[A-Fa-f0-9]{6}"))throw new IllegalArgumentException("Invalid club colour");return (int)(0xff000000L|Long.parseLong(value,16));}
}
