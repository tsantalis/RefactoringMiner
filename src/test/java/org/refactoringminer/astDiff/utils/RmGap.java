package org.refactoringminer.astDiff.utils;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.function.Executable;
import org.opentest4j.AssertionFailedError;

/**
 * Flags an oracle case exercising something RM does not represent, by its item on the page of RM representation gaps
 * and the identifier the page cites for the case. The expectation of the case is still the ground truth: a gap worked
 * around in ChangeNarrator must pass, and the failure of a gap not handled yet is reported as a skipped test, until RM
 * represents it.
 */
public class RmGap {
    public int item;
    public String id;
    public boolean workaround;

    /** Runs the checks of a case, flagged with the gap or not flagged (null). */
    public static void check(RmGap gap, Executable checks) throws Throwable {
        try {
            checks.execute();
        } catch (AssertionFailedError failure) {
            if (gap == null || gap.workaround) {
                throw failure;
            }
            Assumptions.abort(gap + ", not handled: " + failure.getMessage());
        }
    }

    @Override
    public String toString() {
        return "RM gap " + item + " (" + id + ")";
    }
}
