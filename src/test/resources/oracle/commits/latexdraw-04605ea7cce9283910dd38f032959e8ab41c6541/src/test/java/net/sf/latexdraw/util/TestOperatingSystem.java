package net.sf.latexdraw.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static net.bytebuddy.matcher.ElementMatchers.is;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;

public class TestOperatingSystem {
	@ParameterizedTest
	@EnumSource(OperatingSystem.class)
	void testGetPS2EPSBinPath(final OperatingSystem os) {
		assertThat(os.getPS2EPSBinPath(), not(is(emptyOrNullString())));
	}

	@ParameterizedTest
	@EnumSource(OperatingSystem.class)
	void testGetDvipsBinPath(final OperatingSystem os) {
		assertThat(os.getDvipsBinPath(), not(is(emptyOrNullString())));
	}

	@ParameterizedTest
	@EnumSource(OperatingSystem.class)
	void testGetLatexBinPath(final OperatingSystem os) {
		assertThat(os.getLatexBinPath(), not(is(emptyOrNullString())));
	}

	@ParameterizedTest
	@EnumSource(OperatingSystem.class)
	void testGetPs2pdfBinPath(final OperatingSystem os) {
		assertThat(os.getPs2pdfBinPath(), not(is(emptyOrNullString())));
	}

	@ParameterizedTest
	@EnumSource(OperatingSystem.class)
	void testGetPdfcropBinPath(final OperatingSystem os) {
		assertThat(os.getPdfcropBinPath(), not(is(emptyOrNullString())));
	}
}
