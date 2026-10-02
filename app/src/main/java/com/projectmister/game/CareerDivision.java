package com.projectmister.game;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashSet;

/** A save owns its club order: future catalog changes cannot remap player/team indices. */
public final class CareerDivision {
    public final String id, country, name;
    public final int tier;
    public final String[] clubIds, names;
    public final int[] strengths, budgets, primary, secondary;
    public final boolean[] reserves;
    public CareerDivision(CompetitionCatalog.Division division) {
        id=division.id;country=division.countryCode;name=division.name;tier=division.tier;
        int n=division.clubs.size();clubIds=new String[n];names=new String[n];reserves=new boolean[n];
        strengths=new int[n];budgets=new int[n];primary=new int[n];secondary=new int[n];
        int[] palette={0xffbb3038,0xff285da8,0xff237c51,0xffa87922,0xff667188,0xff763d83};
        for(int i=0;i<n;i++) {
            CompetitionCatalog.Club club=division.clubs.get(i);clubIds[i]=club.id;names[i]=club.name;reserves[i]=club.reserve;
            int seed=club.id.hashCode()&0x7fffffff;
            // Explicitly fictional simulation values, not claimed real finances or licensed ratings.
            strengths[i]=(tier==1?69:57)+seed%15;budgets[i]=(tier==1?8:2)+seed%(tier==1?25:7);
            primary[i]=palette[seed%palette.length];secondary[i]=0xfff4f5f7;
        }
    }
    private CareerDivision(JSONObject o)throws Exception {
        if(o.getInt("schema")!=1)throw new IllegalArgumentException("Unsupported career database");
        id=o.getString("id");country=o.getString("country");name=o.getString("name");tier=o.getInt("tier");
        if(tier<1||tier>2)throw new IllegalArgumentException("Invalid tier");
        JSONArray a=o.getJSONArray("clubs");int n=a.length();
        if(n<10||n>24)throw new IllegalArgumentException("Invalid club count");
        clubIds=new String[n];names=new String[n];reserves=new boolean[n];strengths=new int[n];budgets=new int[n];primary=new int[n];secondary=new int[n];
        HashSet<String> seen=new HashSet<>();
        for(int i=0;i<n;i++) {
            JSONObject c=a.getJSONObject(i);clubIds[i]=c.getString("id");names[i]=c.getString("name");
            if(!seen.add(clubIds[i])||names[i].isEmpty())throw new IllegalArgumentException("Invalid club identity");
            reserves[i]=c.optBoolean("reserve",false);strengths[i]=c.getInt("strength");budgets[i]=c.getInt("budget");
            primary[i]=c.getInt("primary");secondary[i]=c.getInt("secondary");
        }
    }
    public static CareerDivision restore(String value)throws Exception {return new CareerDivision(new JSONObject(value));}
    public String snapshot() {
        try {
            JSONObject o=new JSONObject().put("schema",1).put("id",id).put("country",country).put("name",name).put("tier",tier);
            JSONArray a=new JSONArray();for(int i=0;i<names.length;i++)a.put(new JSONObject().put("id",clubIds[i]).put("name",names[i])
                    .put("reserve",reserves[i]).put("strength",strengths[i]).put("budget",budgets[i]).put("primary",primary[i]).put("secondary",secondary[i]));
            return o.put("clubs",a).toString();
        }catch(Exception e){throw new IllegalStateException("Cannot encode career database",e);}
    }
    public boolean splitSeason(){return country.equals("SCO")&&tier==1;}
    public int meetings(){return country.equals("SCO")?(tier==1?3:4):2;}
    public int rounds(){return splitSeason()?38:(names.length+names.length%2-1)*meetings();}
}
