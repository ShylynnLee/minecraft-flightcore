package com.flightcore.block;

/** 电线类型：不同金属，导电速率不同。 */
public enum WireType {
    IRON("iron", 1_200),
    COPPER("copper", 6_000),
    GOLD("gold", 30_000);

    private final String name;
    private final int rate;

    WireType(String name, int rate) {
        this.name = name;
        this.rate = rate;
    }

    public String getName() {
        return name;
    }

    /** 每秒（每 tick）最大传导 FE */
    public int getRate() {
        return rate;
    }
}
