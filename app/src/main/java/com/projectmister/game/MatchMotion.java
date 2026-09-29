package com.projectmister.game;

/** Original bounded arrival steering, in pitch-length units (105 x 68 metre aspect). */
final class MatchMotion {
    static void arrive(float[] x,float[] y,float[] vx,float[] vy,int i,float tx,float ty,float speed,float acceleration,float dt){
        float dx=tx-x[i],dy=(ty-y[i])*68f/105f;
        float distance=(float)Math.hypot(dx,dy);
        float desired=Math.min(speed,(float)Math.sqrt(2*acceleration*distance));
        desired*=Math.min(1,distance/.035f);
        float wantX=distance<.0001f?0:dx/distance*desired;
        float wantY=distance<.0001f?0:dy/distance*desired;
        float oldY=vy[i]*68f/105f;
        float ax=(wantX-vx[i])/.22f,ay=(wantY-oldY)/.22f;
        float force=(float)Math.hypot(ax,ay);
        if(force>acceleration){ax*=acceleration/force;ay*=acceleration/force;}
        vx[i]+=ax*dt;oldY+=ay*dt;
        float magnitude=(float)Math.hypot(vx[i],oldY);
        if(magnitude>speed){vx[i]*=speed/magnitude;oldY*=speed/magnitude;}
        vy[i]=oldY*105f/68f;
        x[i]=MatchMath.clamp(x[i]+vx[i]*dt,.035f,.965f);
        y[i]=MatchMath.clamp(y[i]+vy[i]*dt,.035f,.965f);
    }
}
