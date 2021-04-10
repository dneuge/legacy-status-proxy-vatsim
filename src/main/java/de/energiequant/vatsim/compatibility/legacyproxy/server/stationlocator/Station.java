package de.energiequant.vatsim.compatibility.legacyproxy.server.stationlocator;

public class Station {
    private final String callsign;
    private final double latitude;
    private final double longitude;

    public Station(String callsign, double latitude, double longitude) {
        this.callsign = callsign;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getCallsign() {
        return callsign;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}
