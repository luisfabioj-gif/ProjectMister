package com.projectmister.game;
import java.util.*;

/** Version-one deterministic fictional recruits; source identity survives catalog changes. */
public final class WorldMarket {
    public static final int MAX_HIRES=512;
    public static final java.time.LocalDate FREE_AGENT_CONTRACT_END=java.time.LocalDate.of(2026,6,30);
    public static final class Candidate {
        public final String key,name,position,nationality;
        public final int age,overall,fee,wage;
        public final boolean free;
        public Candidate(String key) {this(key,null);}
        public Candidate(String key,String requestedNationality) {
            if(key==null||!key.matches("[a-z0-9:-]{1,100}~[012]~[0-9]{1,2}"))throw new IllegalArgumentException("Invalid market identity");
            String[] p=key.split("~");int slot=Integer.parseInt(p[2]);free=p[0].equals("free");
            int tier=Integer.parseInt(p[1]);if(free?tier!=0||slot>=60:tier<1||slot>=20)throw new IllegalArgumentException("Invalid market slot");
            this.key=key;Random r=new Random(0xB05511L+key.hashCode());
            String[] first={"João","Miguel","André","Luca","Marco","Daniel","Adam","Noah","Yusuf","Emre","Samuel","Ibrahim","Amadou","Kenji","Gabriel","Lucas","Hugo","Nicolas","Daan","Leon"};
            String[] last={"Silva","Costa","Santos","Rossi","Martin","Bennett","Wilson","Kaya","Demir","Diallo","Mensah","Sato","Almeida","Fernandes","Dubois","de Vries","Jansen","Weber","Fischer","Romero"};
            // Preserve the old attribute RNG sequence when repairing cultural identities.
            r.nextInt(first.length);r.nextInt(last.length);
            String[] freeNations={"England","Brazil","France","Portugal","Spain","Germany","Italy","Netherlands","Nigeria","Senegal","Japan","Argentina","Scotland","Turkey","Belgium","Morocco","Denmark","Sweden","Poland","Ghana"};
            nationality=requestedNationality!=null?requestedNationality:free?freeNations[Math.floorMod(key.hashCode(),freeNations.length)]:PlayerNames.home(p[0].contains(":")?p[0].substring(0,p[0].indexOf(':')):p[0]);
            name=PlayerNames.name(nationality,0xB05511L+key.hashCode());
            String[] positions={"GK","GK","RB","CB","CB","CB","LB","LB","DM","CM","CM","CM","AM","RM","LM","RW","LW","ST","ST","ST"};
            position=positions[slot%20];age=18+r.nextInt(17);overall=(free?48:tier==1?62:52)+r.nextInt(24);
            fee=free?0:Math.max(1,(overall-45)*(overall-45)/95);wage=Math.max(3,(overall-42)*(overall-42)/55);
        }
    }
    public static List<String> restore(String text) {
        List<String> out=new ArrayList<>();if(text.isEmpty())return out;
        if(text.length()>60000)throw new IllegalArgumentException("Oversized market save");
        for(String key:text.split(",",-1)){new Candidate(key);if(out.contains(key)||out.size()>=MAX_HIRES)throw new IllegalArgumentException("Invalid market recruitment history");out.add(key);}return out;
    }
    public static String snapshot(List<String> keys){String value=String.join(",",keys);restore(value);return value;}
}
