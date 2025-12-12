package net.sf.latexdraw.util;

import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestLangService {
	public static Stream<Locale> getLocales() {
		return Stream.of(Locale.TAIWAN, new Locale("foo", "Bar"));
	}

	LangService lang;
	Locale locale;

	@BeforeEach
	public void setUp() {
		locale = Locale.getDefault();
		lang = new LangService(new SystemService());
	}

	@AfterEach
	public void tearDown() {
		Locale.setDefault(locale);
	}

	@Test
	public void testGetBundle() {
		assertNotNull(lang.getBundle());
	}

	@Test
	public void testGetSupportedLocales() {
		assertNotNull(lang.getSupportedLocales());
		assertFalse(lang.getSupportedLocales().isEmpty());
	}

	@ParameterizedTest
	@MethodSource(value = "getLocales")
	public void testIncorrectLocale(final Locale badLocale) {
		Locale.setDefault(badLocale);
		lang = new LangService(new SystemService());
		assertNotNull(lang.getBundle());
	}
}
