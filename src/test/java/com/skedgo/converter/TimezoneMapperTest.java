package com.skedgo.converter;

import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.Assert.assertEquals;


public class TimezoneMapperTest {
    private static final Instant JANUARY = Instant.parse("2026-01-15T12:00:00Z");
    private static final Instant JULY = Instant.parse("2026-07-15T12:00:00Z");

    @Test
    public void go() {
        Assert.assertEquals(TimezoneMapper.latLngToTimezoneString(65.012197, 25.471152), "Europe/Helsinki");
        assertEquals(TimezoneMapper.latLngToTimezoneString(41.8788764, -87.6359149), "America/Chicago");
        assertEquals(TimezoneMapper.latLngToTimezoneString(42.75676, -0.092723), "Europe/Paris");
    }

    /** Political zones, including ones 2-3 h from their nautical zone or across the date line, are kept. */
    @Test
    public void landPoints_politicalZoneKept() {
        Object[][] places = {
                {"Madrid", 40.42, -3.70, "Europe/Madrid"},
                {"Vigo", 42.24, -8.72, "Europe/Madrid"},             // +1 standard, nautical -1: 2 h
                {"Paris", 48.85, 2.35, "Europe/Paris"},
                {"London", 51.50, -0.12, "Europe/London"},
                {"Anchorage", 61.22, -149.90, "America/Anchorage"},
                {"Adak", 51.88, -176.66, "America/Adak"},            // -10, nautical -12: 2 h
                {"Mendoza", -32.89, -68.83, "America/Argentina/Mendoza"}, // -3, nautical -5: 2 h
                {"Santiago", -33.45, -70.65, "America/Santiago"},
                {"Lhasa", 29.65, 91.11, "Asia/Shanghai"},            // +8, nautical +6: 2 h
                {"Ngari", 31.50, 80.10, "Asia/Shanghai"},            // +8, nautical +5: 3 h
                {"Delhi", 28.60, 77.20, "Asia/Kolkata"},
                {"Tokyo", 35.68, 139.69, "Asia/Tokyo"},
                {"Apia", -13.83, -171.76, "Pacific/Apia"},           // +13 = nautical -11 on the clock
                {"Kiritimati", 1.87, -157.43, "Pacific/Kiritimati"}, // +14 = nautical -10 on the clock
                {"Tongatapu", -21.14, -175.20, "Pacific/Tongatapu"}, // +13 vs nautical -12: 1 h
        };
        for (Object[] place : places)
            assertEquals((String) place[0], place[3], TimezoneMapper.latLngToTimezoneString((double) place[1], (double) place[2]));
    }

    /** The result does not depend on the date it is asked: Madrid keeps daylight saving in July. */
    @Test
    public void madrid_summerAndWinterOffsets() {
        ZoneId madrid = ZoneId.of(TimezoneMapper.latLngToTimezoneString(40.42, -3.70));
        assertEquals(ZoneOffset.ofHours(1), madrid.getRules().getOffset(JANUARY));
        assertEquals(ZoneOffset.ofHours(2), madrid.getRules().getOffset(JULY));
    }

    /** Open sea the polygons give to a distant land zone gets its nautical zone instead. */
    @Test
    public void oceanGivenFarLandZone_nauticalZone() {
        assertEquals("America/Santiago", TimezoneMapper.latLngToPolygonTimezoneString(-40.0, -140.0));
        assertEquals("Etc/GMT+9", TimezoneMapper.latLngToTimezoneString(-40.0, -140.0));
        assertEquals("Australia/Hobart", TimezoneMapper.latLngToPolygonTimezoneString(-45.0, 90.0));
        assertEquals("Etc/GMT-6", TimezoneMapper.latLngToTimezoneString(-45.0, 90.0));
    }

    /** Nautical zone: round(lng / 15), each zone 15 degrees wide centred on its meridian. */
    @Test
    public void tzForLng_nauticalZones() {
        assertEquals(0, TimezoneMapper.tzForLng(0.0));
        assertEquals(0, TimezoneMapper.tzForLng(7.4));
        assertEquals(1, TimezoneMapper.tzForLng(7.6));
        assertEquals(-2, TimezoneMapper.tzForLng(-30.0));
        assertEquals(-7, TimezoneMapper.tzForLng(-100.0));
        assertEquals(-12, TimezoneMapper.tzForLng(-179.9));
        assertEquals(12, TimezoneMapper.tzForLng(179.9));
        assertEquals(12, TimezoneMapper.tzForLng(180.0));
        assertEquals(-12, TimezoneMapper.tzForLng(-180.0));
    }

    /** Etc/GMT names have the opposite sign: Etc/GMT+2 is UTC-2. */
    @Test
    public void nauticalZoneId_signAndOffset() {
        for (int hours = -12; hours <= 12; hours++)
            assertEquals("hours " + hours, ZoneOffset.ofHours(hours), ZoneId.of(TimezoneMapper.nauticalZoneId(hours)).getRules().getOffset(JULY));
        assertEquals("Etc/GMT+2", TimezoneMapper.nauticalZoneId(-2));
        assertEquals("Etc/GMT-5", TimezoneMapper.nauticalZoneId(5));
    }
}
