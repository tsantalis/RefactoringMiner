package org.refactoringminer.rm2.model;

public enum RelationshipType {

    SAME                    (true,  false, false),
    MOVE_TYPE               (true,  false, false),
    CONVERT_TO_INTERFACE    (true,  false, false),
    CONVERT_TO_CLASS        (true,  false, false),
    RENAME_TYPE             (true,  false, false),
    MOVE_AND_RENAME_TYPE    (true,  false, false),
//    MOVE_MEMBER             (true,  false, false),
    MOVE_METHOD             (true,  true,  true ),
    MOVE_FIELD              (true,  true,  true ),
//    RENAME_MEMBER           (true,  false, false),
    RENAME_METHOD           (true,  false, false),
    CHANGE_METHOD_SIGNATURE (true,  false, false),
//    PULL_UP_MEMBER          (true,  true,  false),
    PULL_UP_METHOD          (true,  true,  false),
    PULL_UP_FIELD           (true,  true,  false),
//    PUSH_DOWN_MEMBER        (true,  false, true ),
    PUSH_DOWN_METHOD        (true,  false, true ),
    PUSH_DOWN_FIELD         (true,  false, true ),
//    EXTRACT                 (false, true,  false),
    EXTRACT_METHOD          (false, true,  false),
    EXTRACT_SUPERTYPE       (false, true,  false),
    INLINE_METHOD           (false, false, true);

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
