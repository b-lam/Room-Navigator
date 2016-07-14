package uwaterloo.ca.lab4_202_13;

/**
 * Created by Brandon on 6/16/2016.
 */
public final class Constant {

    static final int STATE_INITIAL = 0; //Initial state
    static final int STATE_DROP = 1; //Negative slope
    static final int STATE_RISE = 2; //Positive slop
    static final int STATE_PEAK = 3; //Max value of cycle

    static final float ALPHA = 0.97f;

    static float stepDistance = 0.83f;


}
