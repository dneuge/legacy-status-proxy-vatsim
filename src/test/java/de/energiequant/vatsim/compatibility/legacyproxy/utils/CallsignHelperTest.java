package de.energiequant.vatsim.compatibility.legacyproxy.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class CallsignHelperTest {
    @CsvSource({
        "KATL, true", //
        "katl, true", //
        "KATL_, true", //
        "KATL_TWR, true", //
        "KATL_1_TWR, true", //
        "KATL_, true", //
        "K012, true", //
        "K0123, false", //
        "KAT, false", //
        "ATL, false", //
        "KATL1, false", //
        "EATL, false", //
        "EKAT, false", //
    })
    @ParameterizedTest
    public void testIsUsIcaoCallsign_always_returnsExpectedResult(String callsign, boolean expectedResult) {
        // Arrange (nothing to do)

        // Act
        boolean result = CallsignHelper.isUsIcaoCallsign(callsign);

        // Assert
        assertThat(result).isEqualTo(expectedResult);
    }
}
