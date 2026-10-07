package com.projectmister.game;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;

/** Additional real club identities, kept in each new career's immutable index order. */
public final class DomesticCupCatalog {
    final JSONArray clubs,first,byes,late;
    private DomesticCupCatalog(JSONObject o)throws Exception {
        if(o.getInt("schema")!=1||o.getInt("season")!=2026)throw new IllegalArgumentException("Unsupported cup catalog");
        clubs=o.getJSONArray("clubs");first=o.getJSONArray("firstRound");byes=o.getJSONArray("byes");late=o.getJSONArray("lateEntrants");
        if(clubs.length()!=113||first.length()!=47||byes.length()!=19||late.length()!=5)throw new IllegalArgumentException("Incomplete cup catalog");
        Set<String> seen=new HashSet<>();int[] levels=new int[6];
        for(int i=0;i<clubs.length();i++) {
            JSONObject c=clubs.getJSONObject(i);int level=c.getInt("level");
            if(level<3||level>5||!c.getString("id").matches("pt:cup-[a-z0-9-]+")||c.getString("name").trim().isEmpty()||!seen.add(c.getString("id")))throw new IllegalArgumentException("Invalid cup club");
            levels[level]++;
        }
        if(levels[3]!=19||levels[4]!=52||levels[5]!=42)throw new IllegalArgumentException("Wrong lower-tier field");
    }
    public static DomesticCupCatalog read(InputStream in)throws Exception {
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;
        while((n=in.read(b))!=-1){if(out.size()+n>128*1024)throw new IllegalArgumentException("Oversized cup catalog");out.write(b,0,n);}
        return new DomesticCupCatalog(new JSONObject(new String(out.toByteArray(),StandardCharsets.UTF_8)));
    }
    public DomesticCup create(CareerDivision world,long seed)throws Exception {
        if(!world.country.equals("PT")||!world.linked)throw new IllegalArgumentException("Cup requires linked Portuguese world");
        Map<String,Integer> ids=new HashMap<>();for(int i=0;i<world.clubIds.length;i++)ids.put(world.clubIds[i],i);
        int[] opening=new int[94],rest=new int[19],europe=new int[5];Set<Integer> deferred=new HashSet<>();
        for(int i=0;i<47;i++){JSONArray pair=first.getJSONArray(i);if(pair.length()!=2)throw new IllegalArgumentException("Invalid first tie");for(int j=0;j<2;j++)opening[2*i+j]=resolve(ids,pair.getString(j));}
        for(int i=0;i<19;i++)rest[i]=resolve(ids,byes.getString(i));
        for(int i=0;i<5;i++){europe[i]=resolve(ids,late.getString(i));deferred.add(europe[i]);}
        ArrayList<Integer> lower=new ArrayList<>(),upper=new ArrayList<>();
        for(int i=0;i<world.clubIds.length;i++)if(!world.reserves[i]&&!deferred.contains(i)) {
            if(world.clubTiers[i]==1)upper.add(i);else if(world.clubTiers[i]==2)lower.add(i);
        }
        DomesticCup cup=new DomesticCup(world.names.length,seed,opening,rest,array(lower),array(upper),europe);
        cup.validateEntrants(world.reserves);return cup;
    }
    private static int resolve(Map<String,Integer> ids,String id){Integer c=ids.get(id);if(c==null)throw new IllegalArgumentException("Missing cup club: "+id);return c;}
    private static int[] array(List<Integer> ids){int[] result=new int[ids.size()];for(int i=0;i<result.length;i++)result[i]=ids.get(i);return result;}
}
