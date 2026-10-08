package com.projectmister.game;
import java.util.*;

public final class ScottishLeagueCupFactory {
    public static ScottishLeagueCupSeason create(int year,long seed,String[] identities,int[] tiers,int[] levels,boolean[] reserves,int[] previousOrder,int[] european) {
        Map<Integer,Integer> positions=new HashMap<>();if(previousOrder!=null)for(int i=0;i<previousOrder.length;i++)positions.put(previousOrder[i],i);
        ArrayList<Integer> professional=new ArrayList<>(),guests=new ArrayList<>();
        for(int i=0;i<identities.length;i++)if(identities[i].startsWith("sco:")&&!reserves[i]) {
            if(tiers[i]<=2||tiers[i]==3&&levels[i]<=4)professional.add(i);else if(tiers[i]==3&&levels[i]==5)guests.add(i);
        }
        if(professional.size()!=42)throw new IllegalArgumentException("Scottish League Cup requires 42 SPFL clubs");
        Comparator<Integer> sporting=Comparator.comparingInt(c->positions.getOrDefault(c,(tiers[c]<=2?tiers[c]:levels[c])*10000+c));professional.sort(sporting);guests.sort(sporting);
        ArrayList<Integer> exempt=new ArrayList<>();
        if(european!=null)for(int c:european)if(professional.contains(c)&&!exempt.contains(c))exempt.add(c);
        if(exempt.isEmpty()&&year==2026)for(String key:new String[]{"celtic","heart-of-midlothian","rangers","motherwell","hibernian"}) {
            for(int c:professional)if(identities[c].equals("sco:"+key)){exempt.add(c);break;}
        }
        if(exempt.isEmpty())for(int i=0;i<4;i++)exempt.add(professional.get(i));
        if(exempt.size()<2||exempt.size()>6)throw new IllegalArgumentException("Unsupported Scottish European exemptions");
        professional.removeAll(exempt);
        // Invitations use the known 2026 guest clubs where present, then simulated later sporting order.
        if(year==2026) {
            ArrayList<Integer> invited=new ArrayList<>();for(String key:new String[]{"linlithgow-rose","brora-rangers","brechin-city"})for(int c:guests)if(identities[c].equals("sco:cup-"+key))invited.add(c);
            guests.removeAll(invited);invited.addAll(guests);guests=invited;
        }
        int invitations=40-professional.size();if(invitations<0||invitations>guests.size())throw new IllegalArgumentException("Incomplete Scottish invited field");
        professional.addAll(guests.subList(0,invitations));int[] field=new int[40],direct=new int[exempt.size()];for(int i=0;i<40;i++)field[i]=professional.get(i);for(int i=0;i<direct.length;i++)direct[i]=exempt.get(i);
        return new ScottishLeagueCupSeason(year,seed,field,direct);
    }
    private ScottishLeagueCupFactory(){}
}
