package com.projectmister.game;

import java.time.LocalDate;
import java.util.*;

/** Seeded simulated UEFA draws. Templates encode pots, venues and matchweeks;
 * club placement is solved against association and previous-season constraints. */
public final class EuropeanDraw {
    private final List<EuropeanLeaguePhase.Club> clubs;
    private final int[][] template;
    private final int[][] neighbours;
    private final boolean[][] home=new boolean[36][36],previousHome=new boolean[36][36];
    private final int[] association=new int[36],slotPot=new int[36],assignment=new int[36];
    private final boolean[] used=new boolean[36];
    private final ArrayList<Integer>[] pools;
    private int nodes;

    @SuppressWarnings("unchecked")
    private EuropeanDraw(EuropeanLeaguePhase.Competition competition,List<EuropeanLeaguePhase.Club> field,EuropeanLeaguePhase previous,long seed) {
        if(competition==null||field==null||field.size()!=36)throw new IllegalArgumentException("UEFA draw needs 36 clubs");
        boolean conference=competition==EuropeanLeaguePhase.Competition.CONFERENCE;int pots=conference?6:4;
        for(EuropeanLeaguePhase.Club club:field)if(club==null)throw new IllegalArgumentException("Missing UEFA draw club");
        template=conference?CONFERENCE:CHAMPIONS;clubs=new ArrayList<>(field);
        clubs.sort(Comparator.comparingInt((EuropeanLeaguePhase.Club c)->c.pot).thenComparingInt(c->c.id));
        pools=new ArrayList[pots];for(int p=0;p<pots;p++)pools[p]=new ArrayList<>();
        Map<String,Integer> countries=new HashMap<>();Set<Integer> identities=new HashSet<>();
        for(int c=0;c<36;c++) {
            EuropeanLeaguePhase.Club club=clubs.get(c);
            if(club.pot>=pots||!identities.add(club.id))throw new IllegalArgumentException("Invalid UEFA draw pot or identity");
            pools[club.pot].add(c);association[c]=countries.computeIfAbsent(club.association,k->countries.size());
            slotPot[c]=conference?c/6:c%4;
        }
        for(ArrayList<Integer> pool:pools)if(pool.size()!=36/pots)throw new IllegalArgumentException("Unequal UEFA draw pots");
        for(int country=0;country<countries.size();country++){int count=0;for(int a:association)if(a==country)count++;if(count>18)throw new IllegalArgumentException("Association field cannot satisfy UEFA draw restrictions");}
        ArrayList<Integer>[] linked=new ArrayList[36];for(int c=0;c<36;c++)linked[c]=new ArrayList<>();
        for(int[] f:template){linked[f[1]].add(f[2]);linked[f[2]].add(f[1]);home[f[1]][f[2]]=true;}
        neighbours=new int[36][];for(int c=0;c<36;c++)neighbours[c]=linked[c].stream().mapToInt(Integer::intValue).toArray();
        if(previous!=null) {
            Map<Integer,Integer> index=new HashMap<>();for(int c=0;c<36;c++)index.put(clubs.get(c).id,c);
            for(int i=0;i<previous.fixtureCount();i++){EuropeanLeaguePhase.Fixture f=previous.fixture(i);Integer h=index.get(f.home),a=index.get(f.away);if(h!=null&&a!=null)previousHome[h][a]=true;}
        }
        Random random=new Random(seed);boolean solved=false;
        for(int attempt=0;attempt<8&&!solved;attempt++) {
            Arrays.fill(assignment,-1);Arrays.fill(used,false);nodes=0;
            for(ArrayList<Integer> pool:pools)Collections.shuffle(pool,random);
            solved=assign(0);
        }
        if(!solved)throw new IllegalStateException("UEFA draw could not satisfy the association and prior-season restrictions; preserve the field and request a new simulated draw seed");
    }
    private boolean valid(int slot,int club) {
        int[] counts=new int[36];
        for(int neighbour:neighbours[slot]) {
            int other=assignment[neighbour];
            if(other>=0) {
                if(association[other]==association[club]||++counts[association[other]]>2)return false;
                if(home[slot][neighbour]?previousHome[club][other]:previousHome[other][club])return false;
            }
            // Opponent-country limits also constrain neighbours awaiting their own club.
            int total=1;
            for(int linked:neighbours[neighbour])if(assignment[linked]>=0&&association[assignment[linked]]==association[club])total++;
            if(total>2)return false;
        }
        return true;
    }
    private boolean assign(int depth) {
        if(depth==36)return true;
        if(++nodes>50000)return false;
        int slot=-1,best=37;ArrayList<Integer> candidates=null;
        for(int s=0;s<36;s++)if(assignment[s]<0) {
            ArrayList<Integer> available=new ArrayList<>();
            for(int c:pools[slotPot[s]])if(!used[c]&&valid(s,c))available.add(c);
            if(available.isEmpty())return false;
            if(available.size()<best){slot=s;best=available.size();candidates=available;if(best==1)break;}
        }
        for(int c:candidates){assignment[slot]=c;used[c]=true;if(assign(depth+1))return true;assignment[slot]=-1;used[c]=false;if(nodes>50000)return false;}
        return false;
    }
    public static EuropeanLeaguePhase create(EuropeanLeaguePhase.Competition competition,int year,List<EuropeanLeaguePhase.Club> field,List<LocalDate> dates,long seed,EuropeanLeaguePhase previous) {
        if(previous!=null&&(previous.competition!=competition||previous.season!=year-1))throw new IllegalArgumentException("Wrong previous UEFA edition");
        EuropeanDraw draw=new EuropeanDraw(competition,field,previous,seed);ArrayList<EuropeanLeaguePhase.Fixture> fixtures=new ArrayList<>();
        for(int[] f:draw.template)fixtures.add(new EuropeanLeaguePhase.Fixture(f[0],draw.clubs.get(draw.assignment[f[1]]).id,draw.clubs.get(draw.assignment[f[2]]).id));
        return new EuropeanLeaguePhase(competition,year,draw.clubs,fixtures,dates);
    }

    // Repository-owned synthetic templates. Every round is a perfect matching.
    // Both opening and closing pairs contain one home/one away match; no H/A run exceeds two.
    private static final int[][] CHAMPIONS={
        {0,0,1},
        {0,3,5},
        {0,4,6},
        {0,7,9},
        {0,8,11},
        {0,10,13},
        {0,12,14},
        {0,15,16},
        {0,17,21},
        {0,18,19},
        {0,20,22},
        {0,23,27},
        {0,24,25},
        {0,26,28},
        {0,29,30},
        {0,31,32},
        {0,33,34},
        {0,35,2},
        {1,1,4},
        {1,2,3},
        {1,5,7},
        {1,6,8},
        {1,9,10},
        {1,11,12},
        {1,13,17},
        {1,14,15},
        {1,16,18},
        {1,19,20},
        {1,21,24},
        {1,22,23},
        {1,25,26},
        {1,27,29},
        {1,28,31},
        {1,30,33},
        {1,32,0},
        {1,34,35},
        {2,1,2},
        {2,3,7},
        {2,4,5},
        {2,6,10},
        {2,8,9},
        {2,11,15},
        {2,12,16},
        {2,13,14},
        {2,17,18},
        {2,19,22},
        {2,20,21},
        {2,23,24},
        {2,25,27},
        {2,26,30},
        {2,28,29},
        {2,31,33},
        {2,32,34},
        {2,35,0},
        {3,0,3},
        {3,2,4},
        {3,5,6},
        {3,7,8},
        {3,9,11},
        {3,10,14},
        {3,12,13},
        {3,15,19},
        {3,16,17},
        {3,18,20},
        {3,21,23},
        {3,22,25},
        {3,24,26},
        {3,27,28},
        {3,29,32},
        {3,30,31},
        {3,33,35},
        {3,34,1},
        {4,0,2},
        {4,4,8},
        {4,5,9},
        {4,6,7},
        {4,10,12},
        {4,11,13},
        {4,14,17},
        {4,15,18},
        {4,16,19},
        {4,20,23},
        {4,21,25},
        {4,22,24},
        {4,26,27},
        {4,28,32},
        {4,29,31},
        {4,30,34},
        {4,33,1},
        {4,35,3},
        {5,1,5},
        {5,2,6},
        {5,3,4},
        {5,7,11},
        {5,8,10},
        {5,9,12},
        {5,13,15},
        {5,14,16},
        {5,17,20},
        {5,18,22},
        {5,19,21},
        {5,23,26},
        {5,24,28},
        {5,25,29},
        {5,27,30},
        {5,31,35},
        {5,32,33},
        {5,34,0},
        {6,0,4},
        {6,1,3},
        {6,5,8},
        {6,6,9},
        {6,7,10},
        {6,11,14},
        {6,12,15},
        {6,13,16},
        {6,17,19},
        {6,18,21},
        {6,20,24},
        {6,22,26},
        {6,23,25},
        {6,27,31},
        {6,28,30},
        {6,29,33},
        {6,32,35},
        {6,34,2},
        {7,2,5},
        {7,3,6},
        {7,4,7},
        {7,8,12},
        {7,9,13},
        {7,10,11},
        {7,14,18},
        {7,15,17},
        {7,16,20},
        {7,19,23},
        {7,21,22},
        {7,24,27},
        {7,25,28},
        {7,26,29},
        {7,30,32},
        {7,31,34},
        {7,33,0},
        {7,35,1}
    };
    private static final int[][] CONFERENCE={
        {0,0,12},
        {0,2,14},
        {0,3,15},
        {0,4,5},
        {0,6,7},
        {0,8,9},
        {0,11,35},
        {0,17,22},
        {0,18,30},
        {0,19,1},
        {0,23,16},
        {0,24,25},
        {0,26,20},
        {0,27,21},
        {0,28,10},
        {0,29,34},
        {0,31,13},
        {0,32,33},
        {1,1,6},
        {1,5,29},
        {1,7,31},
        {1,9,2},
        {1,10,11},
        {1,12,24},
        {1,13,18},
        {1,14,8},
        {1,15,27},
        {1,16,17},
        {1,20,32},
        {1,21,3},
        {1,22,23},
        {1,25,19},
        {1,30,0},
        {1,33,26},
        {1,34,4},
        {1,35,28},
        {2,2,3},
        {2,5,17},
        {2,6,30},
        {2,7,0},
        {2,8,32},
        {2,9,33},
        {2,11,4},
        {2,13,25},
        {2,15,20},
        {2,16,10},
        {2,19,12},
        {2,21,14},
        {2,22,34},
        {2,23,35},
        {2,24,18},
        {2,26,27},
        {2,28,29},
        {2,31,1},
        {3,0,24},
        {3,1,13},
        {3,3,8},
        {3,4,28},
        {3,10,22},
        {3,12,6},
        {3,14,26},
        {3,17,11},
        {3,18,19},
        {3,20,21},
        {3,25,7},
        {3,27,9},
        {3,29,23},
        {3,30,31},
        {3,32,2},
        {3,33,15},
        {3,34,16},
        {3,35,5},
        {4,0,1},
        {4,4,16},
        {4,5,10},
        {4,6,18},
        {4,7,19},
        {4,9,21},
        {4,11,23},
        {4,12,13},
        {4,14,15},
        {4,17,29},
        {4,20,2},
        {4,25,30},
        {4,26,8},
        {4,27,32},
        {4,28,22},
        {4,31,24},
        {4,33,3},
        {4,34,35},
        {5,1,25},
        {5,2,26},
        {5,3,27},
        {5,8,20},
        {5,10,34},
        {5,13,7},
        {5,15,9},
        {5,16,28},
        {5,18,0},
        {5,19,31},
        {5,21,33},
        {5,22,4},
        {5,23,5},
        {5,24,6},
        {5,29,11},
        {5,30,12},
        {5,32,14},
        {5,35,17}
    };
}
