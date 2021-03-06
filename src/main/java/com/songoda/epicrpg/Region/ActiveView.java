package com.songoda.epicrpg.Region;

public class ActiveView {

    private final Region region;
    private final long start;

    public ActiveView(Region region) {
        this.region = region;
        this.start = System.currentTimeMillis();
    }

    public Region getRegion() {
        return region;
    }

    public long getStart() {
        return start;
    }
}
