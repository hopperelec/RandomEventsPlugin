package uk.co.hopperelec.mc.randomevents;

import org.jetbrains.annotations.NotNull;

public class TimeInSeconds {
    protected short unsignedTime;

    public TimeInSeconds() {
        this(0);
    }
    public TimeInSeconds(int signedTime) {
        set(signedTime);
    }

    public int asInt() {
        return unsignedTime+Short.MAX_VALUE;
    }
    public double asDouble() {
        return asInt();
    }

    public void decrement() {
        unsignedTime--;
    }
    public void set(int signedTime) {
        unsignedTime = (short)(signedTime-Short.MAX_VALUE);
    }
    public void set(@NotNull TimeInSeconds newValue) {
        unsignedTime = newValue.unsignedTime;
    }
    public double dividedBy(@NotNull TimeInSeconds operand) {
        return asDouble()/operand.asDouble();
    }
    public boolean moreThan(@NotNull TimeInSeconds operand) {
        return unsignedTime > operand.unsignedTime;
    }
}
