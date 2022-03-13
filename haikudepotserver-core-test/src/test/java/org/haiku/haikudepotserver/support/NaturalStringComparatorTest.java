/*
 * Copyright 2014-2022, Andrew Lindesay
 * Distributed under the terms of the MIT License.
 */

package org.haiku.haikudepotserver.support;

import org.fest.assertions.Assertions;
import org.junit.jupiter.api.Test;

public class NaturalStringComparatorTest {

    private NaturalStringComparator naturalStringComparator = new NaturalStringComparator();

    @Test
    public void testLeadingZero() {
        Assertions.assertThat(naturalStringComparator.compare("9p0","0p1")).isGreaterThan(0);
        Assertions.assertThat(naturalStringComparator.compare("00p0","09p1")).isLessThan(0);
        Assertions.assertThat(naturalStringComparator.compare("00p0","0p0")).isEqualTo(0);
    }

    @Test
    public void testSimpleNumbers() {
        Assertions.assertThat(naturalStringComparator.compare("123","456")).isLessThan(0);
        Assertions.assertThat(naturalStringComparator.compare("1234","456")).isGreaterThan(0);
        Assertions.assertThat(naturalStringComparator.compare("123","4567")).isLessThan(0);
        Assertions.assertThat(naturalStringComparator.compare("123","123")).isEqualTo(0);
        Assertions.assertThat(naturalStringComparator.compare("0123","123")).isEqualTo(0);
        Assertions.assertThat(naturalStringComparator.compare("0000","0000")).isEqualTo(0);
        Assertions.assertThat(naturalStringComparator.compare("0000","456")).isLessThan(0);
        Assertions.assertThat(naturalStringComparator.compare("123","00")).isGreaterThan(0);
        Assertions.assertThat(naturalStringComparator.compare("123  ","123 ")).isEqualTo(0);
    }

    @Test
    public void testSimpleAscii() {
        Assertions.assertThat(naturalStringComparator.compare("abc","def")).isLessThan(0);
        Assertions.assertThat(naturalStringComparator.compare("abc","abc")).isEqualTo(0);
        Assertions.assertThat(naturalStringComparator.compare("def","abc")).isGreaterThan(0);
    }

    @Test
    public void testChunked() {
        Assertions.assertThat(naturalStringComparator.compare("a123","a456")).isLessThan(0);
        Assertions.assertThat(naturalStringComparator.compare("a123","a123")).isEqualTo(0);
        Assertions.assertThat(naturalStringComparator.compare("a456","a123")).isGreaterThan(0);
        Assertions.assertThat(naturalStringComparator.compare("a123a","a123b")).isLessThan(0);
        Assertions.assertThat(naturalStringComparator.compare("a123b","a123a")).isGreaterThan(0);
    }

    /**
     * <p>These test cases have been taken from NaturalCompareTest::TestSome in the Haiku source tree, but
     * have been adjusted to suit the semantics of this application's language.</p>
     */

    @Test
    public void testsFromCpp() {
        Assertions.assertThat(naturalStringComparator.compare(null,null)).isEqualTo(0);
        Assertions.assertThat(naturalStringComparator.compare(null,"A")).isLessThan(0);
        Assertions.assertThat(naturalStringComparator.compare("a","A")).isEqualTo(0);
        Assertions.assertThat(naturalStringComparator.compare("\u00e4","a")).isGreaterThan(0); // a-umlaut
        Assertions.assertThat(naturalStringComparator.compare("3","99")).isLessThan(0);
        Assertions.assertThat(naturalStringComparator.compare("9","19")).isLessThan(0);
        Assertions.assertThat(naturalStringComparator.compare("13","99")).isLessThan(0);
        Assertions.assertThat(naturalStringComparator.compare("9","11")).isLessThan(0);
        Assertions.assertThat(naturalStringComparator.compare("00000009","111")).isLessThan(0);
        Assertions.assertThat(naturalStringComparator.compare("Hallo2","hallo12")).isLessThan(0);
        Assertions.assertThat(naturalStringComparator.compare("Hallo 2","hallo12")).isLessThan(0);
        Assertions.assertThat(naturalStringComparator.compare("Hallo  2","hallo12")).isLessThan(0);
        Assertions.assertThat(naturalStringComparator.compare("Hallo  2 ","hallo12")).isLessThan(0);
        Assertions.assertThat(naturalStringComparator.compare("12 \u00e4ber 42","12aber42")).isGreaterThan(0); // a-umlaut
        Assertions.assertThat(naturalStringComparator.compare("12 \u00e4ber 42","12aber43")).isGreaterThan(0); // a-umlaut
        Assertions.assertThat(naturalStringComparator.compare("12 \u00e4ber 44","12aber43")).isGreaterThan(0); // a-umlaut
        Assertions.assertThat(naturalStringComparator.compare("12 \u00e4ber 44","12aber45")).isGreaterThan(0); // a-umlaut
    }

}
