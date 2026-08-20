// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link KeyboardUtils} class.
 */
class KeyboardUtilsTest {
    /**
     * Checks that definition of extended characters is correct
     */
    @Test
    void testExtendedCharacters() {
        Map<Integer, Character> map = new LinkedHashMap<>();
        KeyboardUtils.addLatinCharacters(map);
        KeyboardUtils.addSymbolCharacters(map);
        KeyboardUtils.addArabicCharacters(map);
        KeyboardUtils.addArmenianCharacters(map);
        KeyboardUtils.addCyrillicCharacters(map);
        KeyboardUtils.addGeorgianCharacters(map);
        KeyboardUtils.addGreekCharacters(map);
        KeyboardUtils.addHebrewCharacters(map);
        KeyboardUtils.addJapaneseCharacters(map);
        KeyboardUtils.addThaiCharacters(map);
        for (Entry<Integer, Character> e : map.entrySet()) {
            assertEquals(e.getKey().intValue(), KeyboardUtils.EXTENDED_KEYCODE_FLAG | (int) e.getValue());
        }
    }

    /**
     * Unit test of {@link KeyboardUtils#getCharactersForKey} - E00 character
     */
    @Test
    void testGetCharactersForKeyE00() {
        char deadCircumflex = (char) KeyEvent.VK_DEAD_CIRCUMFLEX;
        char deadCaron = (char) KeyEvent.VK_DEAD_CARON;
        char deadCircumflex2 = 0x2C6;
        char deadCaron2 = 0x2C7;
        testgetCharactersForKeyE00("ar", 'ذ', '>');
        if (PlatformManager.isPlatformUnixoid()) {
            testgetCharactersForKeyE00("fr_FR", '²', '$', 'œ');
            testgetCharactersForKeyE00("fr_CA", '#', '$', 'œ', '/');
        } else {
            testgetCharactersForKeyE00("fr_FR", '²', '$');
            testgetCharactersForKeyE00("fr_CA", '#', '$', '/');
        }
        testgetCharactersForKeyE00("sq", '\\');
        testgetCharactersForKeyE00("it", '\\');
        testgetCharactersForKeyE00("pt", '\\');
        testgetCharactersForKeyE00("pt_BR", '\'');
        testgetCharactersForKeyE00("de", deadCircumflex, deadCircumflex2);
        testgetCharactersForKeyE00("cs", ';');
        testgetCharactersForKeyE00("he", ';');
        testgetCharactersForKeyE00("iw", ';');
        testgetCharactersForKeyE00("hu", '0');
        testgetCharactersForKeyE00("pl");
        testgetCharactersForKeyE00("bs", '¸');
        testgetCharactersForKeyE00("hr", '¸');
        testgetCharactersForKeyE00("sl", '¸');
        testgetCharactersForKeyE00("sr", '¸');
        testgetCharactersForKeyE00("ro", ']');
        testgetCharactersForKeyE00("da", '½');
        testgetCharactersForKeyE00("fo", '½');
        testgetCharactersForKeyE00("nl", '@');
        testgetCharactersForKeyE00("et", deadCaron, deadCaron2);
        testgetCharactersForKeyE00("is", '°');
        testgetCharactersForKeyE00("es", '|');
        testgetCharactersForKeyE00("es_ES", 'º');
        testgetCharactersForKeyE00("tr", '"', '*');
        testgetCharactersForKeyE00("de_LU", deadCircumflex, deadCircumflex2, '²', '§');
        if (PlatformManager.isPlatformUnixoid()) {
            testgetCharactersForKeyE00("fr_LU", '$', 'œ', '²', '§');
            testgetCharactersForKeyE00("fr_CH", '²', '$', 'œ', '§');
        } else {
            testgetCharactersForKeyE00("fr_LU", '$', '²', '§');
            testgetCharactersForKeyE00("fr_CH", '²', '$', '§');
        }
        testgetCharactersForKeyE00("de_CH", deadCircumflex, deadCircumflex2, '§');
        testgetCharactersForKeyE00("de_LI", deadCircumflex, deadCircumflex2, '§');
        testgetCharactersForKeyE00("fi_FI", '§');
        testgetCharactersForKeyE00("sv_SE", '§');
        testgetCharactersForKeyE00("no_NO", '|');
        testgetCharactersForKeyE00("sv_NO", '|');
    }

    private static void testgetCharactersForKeyE00(String locale, Character... expected) {
        if (locale.contains("_")) {
            String[] l = locale.split("_", -1);
            testgetCharactersForKeyE00(new Locale(l[0], l[1]), expected);
        } else {
            testgetCharactersForKeyE00(new Locale(locale), expected);
        }
    }

    private static void testgetCharactersForKeyE00(Locale locale, Character... expected) {
        List<Character> realExpected = new ArrayList<>(Arrays.asList(expected));
        // Add characters common to all cases
        if (PlatformManager.isPlatformOsx()) {
            realExpected.add('§');
        }
        char deadGrave = (char) KeyEvent.VK_DEAD_GRAVE;
        char deadGrave2 = 0x2CB;
        realExpected.addAll(Arrays.asList('`', deadGrave, deadGrave2));
        assertEquals(realExpected, KeyboardUtils.getCharactersForKey('E', 0, locale));
    }
}
