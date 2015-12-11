package br.ufmg.dcc.labsoft.refdetector.model;

import java.util.Arrays;
import java.util.Map;

public class MembersRepresentation extends HashArray {

    private final Map<Long, String> debug;
    
    public MembersRepresentation(long[] hashes, Map<Long, String> debug) {
        super(hashes);
        this.debug = debug;
    }
    
    public MembersRepresentation minus(MembersRepresentation other) {
        return new MembersRepresentation(computeMinus(hashes, other.hashes), debug);
    }
    
    @Override
    public String toString() {
        if (debug == null) {
            return Arrays.toString(hashes);
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < hashes.length; i++) {
                long h = get(i);
                sb.append(debug.get(h));
                sb.append('\n');
            }
            return sb.toString();
        }
    }
}
