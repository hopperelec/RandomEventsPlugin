package uk.co.hopperelec.mc.randomevents.utils;

import org.jetbrains.annotations.NotNull;

import javax.annotation.CheckReturnValue;

public class TimeInSeconds {
    protected short unsignedTime;

    public TimeInSeconds() {
        this(0);
    }
    public TimeInSeconds(int signedTime) {
        set(signedTime);
    }
    public TimeInSeconds(TimeInSeconds newValue) {
        set(newValue);
    }

    @CheckReturnValue
    public int asInt() {
        return unsignedTime+Short.MAX_VALUE;
    }
    @CheckReturnValue
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
    @CheckReturnValue
    public double dividedBy(@NotNull TimeInSeconds operand) {
        return asDouble()/operand.asDouble();
    }
    @CheckReturnValue
    public boolean moreThan(@NotNull TimeInSeconds operand) {
        return unsignedTime > operand.unsignedTime;
    }
}
