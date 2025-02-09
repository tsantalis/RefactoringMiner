/*
 * Copyright 2010, Red Hat, Inc. and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.zanata.rest.dto.stats;

import org.junit.Test;
import org.zanata.common.LocaleId;
import org.zanata.common.TransUnitCount;
import org.zanata.common.TransUnitWords;
import org.zanata.rest.dto.stats.TranslationStatistics;
import org.zanata.rest.dto.stats.TranslationStatistics.StatUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.equalTo;

public class TranslationStatisticsTest {

    TranslationStatistics stats;
    TransUnitCount unitCount;
    TransUnitWords wordCount;
    private LocaleId localeId = LocaleId.DE;

    @Test
    public void setGetUnitCount() {
        unitCount = new TransUnitCount(5, 5, 5);

        stats = new TranslationStatistics(unitCount, localeId.getId());

        assertThat(stats.getLocale(), equalTo(localeId.getId()));
        assertThat(stats.getUnit(), equalTo(StatUnit.MESSAGE));

        assertThat((int) stats.getApproved(), equalTo(unitCount.getApproved()));
        assertThat((int) stats.getRejected(), equalTo(unitCount.getRejected()));
        assertThat((int) stats.getTotal(), equalTo(unitCount.getTotal()));
        assertThat((int) stats.getTranslated(),
                equalTo(unitCount.getTranslated() + unitCount.getApproved()));
        assertThat((int) stats.getUntranslated(),
                equalTo(unitCount.getUntranslated()));
        assertThat((int) stats.getDraft(), equalTo(unitCount.getNeedReview()
                + unitCount.getRejected()));
    }

    @Test
    public void setGetWordCount() {
        wordCount = new TransUnitWords(5, 5, 5);
        stats = new TranslationStatistics(wordCount, localeId.getId());

        assertThat(stats.getLocale(), equalTo(localeId.getId()));
        assertThat(stats.getUnit(), equalTo(StatUnit.WORD));

        assertThat((int) stats.getApproved(), equalTo(wordCount.getApproved()));
        assertThat((int) stats.getRejected(), equalTo(wordCount.getRejected()));
        assertThat((int) stats.getTotal(), equalTo(wordCount.getTotal()));
        assertThat((int) stats.getTranslated(),
                equalTo(wordCount.getTranslated() + wordCount.getApproved()));
        assertThat((int) stats.getUntranslated(),
                equalTo(wordCount.getUntranslated()));
        assertThat((int) stats.getDraft(), equalTo(wordCount.getNeedReview()
                + wordCount.getRejected()));
    }

    @Test
    public void noArgConstructorSetsZeroStats() {
        stats = new TranslationStatistics();
        assertThat(stats.getUnit(), equalTo(StatUnit.MESSAGE));

        assertThat((int) stats.getApproved(), equalTo(0));
        assertThat((int) stats.getDraft(), is(0));
        assertThat((int) stats.getUntranslated(), is(0));
    }

    @Test
    public void add() {
        stats = new TranslationStatistics();
        unitCount = new TransUnitCount(5, 5, 5);

        TranslationStatistics otherStats =
                new TranslationStatistics(unitCount, localeId.getId());

        stats.add(otherStats);
        assertThat((int) stats.getApproved(), equalTo(unitCount.getApproved()));
        assertThat((int) stats.getRejected(), equalTo(unitCount.getRejected()));
        assertThat((int) stats.getTotal(), equalTo(unitCount.getTotal()));
        assertThat((int) stats.getTranslated(), equalTo(unitCount.getApproved()
                + unitCount.getTranslated()));
        assertThat((int) stats.getUntranslated(),
                equalTo(unitCount.getUntranslated()));
        assertThat((int) stats.getDraft(), equalTo(unitCount.getNeedReview()
                + unitCount.getRejected()));
    }

    @Test
    public void getPercentTranslated() {
        unitCount = new TransUnitCount(5, 5, 5);
        stats = new TranslationStatistics(unitCount, localeId.getId());

        double percentTranslated =
                unitCount.getApproved() * 100d / unitCount.getTotal();

        assertThat(stats.getPercentTranslated(), equalTo(percentTranslated));
    }

    @Test
    public void getPercentDraft() {
        unitCount = new TransUnitCount(5, 5, 5);
        stats = new TranslationStatistics(unitCount, localeId.getId());

        double percentDraft =
                (unitCount.getNeedReview() + unitCount.getRejected()) * 100d
                        / unitCount.getTotal();

        assertThat(stats.getPercentDraft(), equalTo(percentDraft));
    }

    @Test
    public void getPercentUntranslated() {
        unitCount = new TransUnitCount(5, 5, 5);
        stats = new TranslationStatistics(unitCount, localeId.getId());

        double percentUntranslated =
                unitCount.getUntranslated() * 100d / unitCount.getTotal();

        assertThat(stats.getPercentUntranslated(), equalTo(percentUntranslated));
    }
}
