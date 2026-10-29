package com.projectmister.game;

/** One ground plane for pitch markings, footballers, the ball and tactics targets. */
public final class PitchProjection {
    public static final float LENGTH=105f, WIDTH=68f;
    public static final float LEFT=.015f, RIGHT=.985f, TOP=.02f, BOTTOM=.98f;
    public final float width,height;
    private final float farY,nearY,nearWidth,perspective;
    public PitchProjection(float width,float height,boolean tactics){
        this.width=width;this.height=height;
        farY=height*(tactics?.20f:.425f);nearY=height*(tactics?.94f:.78f);
        nearWidth=width*(tactics?.925f:.94f);perspective=tactics?1.20f:1.51f;
    }
    private float depth(float y){return (y-TOP)/(BOTTOM-TOP);}
    public float scale(float y){return 1f/(perspective-(perspective-1f)*depth(y));}
    public float x(float x,float y){return width*.5f+((x-LEFT)/(RIGHT-LEFT)-.5f)*nearWidth*scale(y);}
    public float y(float x,float y){return farY+(nearY-farY)*depth(y)*scale(y);}
    public float groundX(float screenX,float screenY){float t=(screenY-farY)/(nearY-farY);float d=t*perspective/(1f+t*(perspective-1f));return LEFT+((screenX-width*.5f)/(nearWidth/(perspective-(perspective-1f)*d))+.5f)*(RIGHT-LEFT);}
    public float groundY(float screenY){float t=(screenY-farY)/(nearY-farY);return TOP+t*perspective/(1f+t*(perspective-1f))*(BOTTOM-TOP);}
    public static float mx(float metres){return LEFT+(RIGHT-LEFT)*metres/LENGTH;}
    public static float my(float metres){return TOP+(BOTTOM-TOP)*metres/WIDTH;}
}
