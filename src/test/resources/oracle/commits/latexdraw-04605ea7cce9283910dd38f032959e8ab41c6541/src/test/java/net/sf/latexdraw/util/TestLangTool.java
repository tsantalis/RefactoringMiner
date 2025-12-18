package net.sf.latexdraw.util;

import java.util.Locale;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

@RunWith(Theories.class)
public class TestLangTool {
	@DataPoints
	public static Locale[] locales = {Locale.TAIWAN, new Locale("foo", "Bar")};

	LangService lang;
	Locale locale;

	@Before
	public void setUp() {
		locale = Locale.getDefault();
		lang = new LangService(new SystemService());
	}

	@After
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

	@Theory
	public void testIncorrectLocale(final Locale badLocale) {
		Locale.setDefault(badLocale);
		lang = new LangService(new SystemService());
		assertNotNull(lang.getBundle());
	}
}
