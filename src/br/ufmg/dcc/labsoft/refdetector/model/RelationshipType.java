package br.ufmg.dcc.labsoft.refdetector.model;

public enum RelationshipType {

    SAME                    (true,  false, false),
    MOVE_TYPE               (true,  false, false),
    CONVERT_TO_INTERFACE    (true,  false, false),
    CONVERT_TO_CLASS        (true,  false, false),
    RENAME_TYPE             (true,  false, false),
    MOVE_AND_RENAME_TYPE    (true,  false, false),
    MOVE_MEMBER             (true,  true,  true ),
    RENAME_MEMBER           (true,  false, false),
    CHANGE_METHOD_SIGNATURE (true,  false, false),
    PULL_UP_MEMBER          (true,  true,  false),
    PUSH_DOWN_MEMBER        (true,  false, true ),
    EXTRACT                 (false, true,  false),
    INLINE                  (false, false, true);

    private final boolean matching;
    private final boolean multisource;
    private final boolean multitarget;
    
    private RelationshipType(boolean matching, boolean multisource, boolean multitarget) {
        this.matching = matching;
        this.multisource = multisource;
        this.multitarget = multitarget;
    }

    public boolean isMatching() {
        return matching;
    }

    public boolean isMultisource() {
        return multisource;
    }

    public boolean isMultitarget() {
        return multitarget;
    }

}
