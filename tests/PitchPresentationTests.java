package com.projectmister.game;

public final class PitchPresentationTests {
    private static int checks;
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);checks++;}
    public static void main(String[] args){
        for(boolean tactics:new boolean[]{false,true})for(int[] size:new int[][]{{1280,640},{640,300},{1000,330},{320,200}}){
            PitchProjection p=new PitchProjection(size[0],size[1],tactics);
            float far=p.x(PitchProjection.RIGHT,PitchProjection.TOP)-p.x(PitchProjection.LEFT,PitchProjection.TOP),near=p.x(PitchProjection.RIGHT,PitchProjection.BOTTOM)-p.x(PitchProjection.LEFT,PitchProjection.BOTTOM);
            check(far<near,"far touchline recedes");
            check(Math.abs(p.x(.5f,.5f)-size[0]/2f)<.001,"halfway line stays central");
            for(int x=0;x<=10;x++)for(int y=0;y<=10;y++){
                float gx=PitchProjection.mx(x*10.5f),gy=PitchProjection.my(y*6.8f),sx=p.x(gx,gy),sy=p.y(gx,gy);
                check(sx>=0&&sx<=size[0]&&sy>=0&&sy<=size[1],"whole playing field stays visible");
                check(Math.abs(p.groundX(sx,sy)-gx)<.00001&&Math.abs(p.groundY(sy)-gy)<.00001,"ground and player coordinates share reversible projection");
            }
            check(p.scale(PitchProjection.TOP)<p.scale(PitchProjection.BOTTOM),"near players and ball scale consistently");
            check(PitchProjection.mx(0)==PitchProjection.LEFT&&PitchProjection.mx(105)==PitchProjection.RIGHT,"goal lines align with match geometry");
        }
        GoalFlash goal=new GoalFlash();check(!goal.active(0),"no flash before a goal");
        goal.start(4,"GOAL! Forward finishes.",1000);check(goal.active(1000)&&goal.phase(1000)==0,"goal emphasis starts immediately");
        check(goal.phase(1299)==0&&goal.phase(1300)==1&&goal.phase(1600)==0,"goal alternates at bounded visual intervals");
        check(goal.active(4599)&&!goal.active(4600)&&goal.phase(4600)==-1,"goal emphasis expires at its deadline");
        goal.start(7,"GOAL! Equaliser.",4000);check(goal.team==7&&goal.line.contains("Equaliser")&&goal.phase(4000)==0,"new goal restarts the emphasis with the scorer's side");
        goal.clear();check(!goal.active(4001)&&goal.team==-1,"new match can clear an earlier goal");
        System.out.println("PASS "+checks+" pitch geometry, coordinate alignment and goal-flash lifecycle checks");
    }
}
