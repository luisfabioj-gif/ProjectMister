package com.projectmister.game;

/** Pitch geometry in metres. A goalkeeper cannot save a shot outside physical reach. */
public final class ShotQuality {
    public final float distance, angle, aimY, contactX, contactY;
    public final boolean openGoal, keeperCanReach;
    public final double onTarget, saveChance;
    public ShotQuality(boolean home,float x,float y,float keeperX,float keeperY,boolean keeperPresent,int finishing,int keeperAbility,int screens){
        float goal=home?.985f:.015f,dir=home?1:-1;
        float depth=Math.max(.5f,(goal-x)*dir*105),lateral=(y-.5f)*68;
        distance=(float)Math.hypot(depth,lateral);
        angle=(float)Math.abs(Math.atan2(3.66-lateral,depth)-Math.atan2(-3.66-lateral,depth));
        aimY=keeperY>=.5f?.458f:.542f;
        float dx=(goal-x)*105,dy=(aimY-y)*68,kx=(keeperX-x)*105,ky=(keeperY-y)*68;
        float t=(kx*dx+ky*dy)/Math.max(.01f,dx*dx+dy*dy);
        float bounded=Math.max(0,Math.min(1,t));
        float separation=(float)Math.hypot(kx-dx*bounded,ky-dy*bounded);
        float travel=(float)Math.hypot(dx,dy)/24f;
        float reach=1.65f+Math.max(0,travel*bounded-.10f)*(3.6f+keeperAbility/100f);
        keeperCanReach=keeperPresent&&t>=0&&t<=1.08f&&separation<=reach;
        openGoal=!keeperCanReach&&screens==0;
        contactX=x+(goal-x)*bounded;contactY=y+(aimY-y)*bounded;
        float aperture=Math.min(1,angle/.65f),skill=Math.max(0,Math.min(1,finishing/100f));
        onTarget=bound(.45+.22*skill+.23*aperture-distance*.0025-Math.max(0,distance-22)*.008-Math.min(3,screens)*.035,.18,.96);
        saveChance=keeperCanReach?bound(.79+(keeperAbility-finishing)*.0025-aperture*.14+Math.min(3,screens)*.02+Math.max(0,distance-18)*.006-(separation/Math.max(1,reach))*.14,.24,.94):0;
    }
    public static boolean screens(float sx,float sy,float gx,float gy,float dx,float dy){
        float vx=(gx-sx)*105,vy=(gy-sy)*68,px=(dx-sx)*105,py=(dy-sy)*68;
        float t=(px*vx+py*vy)/Math.max(.01f,vx*vx+vy*vy);
        return t>.04f&&t<.94f&&Math.hypot(px-t*vx,py-t*vy)<1.3;
    }
    private static double bound(double n,double min,double max){return Math.max(min,Math.min(max,n));}
}
