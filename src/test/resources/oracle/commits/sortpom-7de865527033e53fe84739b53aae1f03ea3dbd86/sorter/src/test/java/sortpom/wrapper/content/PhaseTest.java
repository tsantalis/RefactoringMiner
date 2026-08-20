package sortpom.wrapper.content;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class PhaseTest {
    @Test
    void compareStandardPhasesAndOtherPhases() {
        Phase[] arr = { Phase.getPhase("a"), Phase.getPhase("teST"), Phase.getPhase("site"), 
                Phase.getPhase("c"), Phase.getPhase("B"), 
                Phase.getPhase("01"), Phase.getPhase("clea"), Phase.getPhase("") };
        List<Phase> list = Arrays.asList(arr);
        Collections.shuffle(list);
        list.sort(Phase::compareTo);
        assertThat(arr.length, is(8));
        int i = 0;
        assertThat(arr[i++].getText(), is("test"));
        assertThat(arr[i++].getText(), is("site"));
        assertThat(arr[i++].getText(), is(""));
        assertThat(arr[i++].getText(), is("01"));
        assertThat(arr[i++].getText(), is("a"));
        assertThat(arr[i++].getText(), is("b"));
        assertThat(arr[i++].getText(), is("c"));
        assertThat(arr[i].getText(), is("clea"));
    }
    
    @Test
    void toStringForPhase() {
        Phase test = Phase.getPhase("teST");
        Phase clea = Phase.getPhase("clea");
        assertThat(test.toString(), is("TEST"));
        assertThat(clea.toString(), is("NonStandardPhase{text='clea'}"));
    }

}
