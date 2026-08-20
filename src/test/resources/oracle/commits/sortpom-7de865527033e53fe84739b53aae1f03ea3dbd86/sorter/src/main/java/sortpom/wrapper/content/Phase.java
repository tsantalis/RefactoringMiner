package sortpom.wrapper.content;

import java.util.Arrays;
import java.util.Optional;

/** Keep track of all Maven standard phases and custom phases */
interface Phase {
    String getText();

    static int compareTo(Phase o1, Phase o2) {
        if (o1 == null) {
            if (o2 == null) {
                return 0;
            }
            return -1;
        }
        if (o2 == null) {
            return 1;
        }
        if (o1 instanceof NonStandardPhase) {
            if (o2 instanceof NonStandardPhase) {
                return o1.getText().compareTo(o2.getText());
            } else {
                return 1;
            }
        }
        StandardPhase so1 = (StandardPhase) o1;
        if (o2 instanceof NonStandardPhase) {
            return -1;
        }
        return so1.compareTo((StandardPhase) o2);
    }

    static Phase getPhase(String name) {
        Optional<Phase> phase = StandardPhase.getPhase(name);
        return phase.orElse(new NonStandardPhase(name));
    }

    class NonStandardPhase implements Phase {
        private final String text;

        public NonStandardPhase(String text) {
            this.text = text.toLowerCase();
        }

        @Override
        public String getText() {
            return text;
        }

        @Override
        public String toString() {
            return "NonStandardPhase{" +
                    "text='" + text + '\'' +
                    '}';
        }
    }

    enum StandardPhase implements Phase {
        PRE_CLEAN("pre-clean"),
        CLEAN("clean"),
        POST_CLEAN("post-clean"),
        VALIDATE("validate"),
        INITIALIZE("initialize"),
        GENERATE_SOURCES("generate-sources"),
        PROCESS_SOURCES("process-sources"),
        GENERATE_RESOURCES("generate-resources"),
        PROCESS_RESOURCES("process-resources"),
        COMPILE("compile"),
        PROCESS_CLASSES("process-classes"),
        GENERATE_TEST_SOURCES("generate-test-sources"),
        PROCESS_TEST_SOURCES("process-test-sources"),
        GENERATE_TEST_RESOURCES("generate-test-resources"),
        PROCESS_TEST_RESOURCES("process-test-resources"),
        TEST_COMPILE("test-compile"),
        PROCESS_TEST_CLASSES("process-test-classes"),
        TEST("test"),
        PREPARE_PACKAGE("prepare-package"),
        PACKAGE("package"),
        PRE_INTEGRATION_TEST("pre-integration-test"),
        INTEGRATION_TEST("integration-test"),
        POST_INTEGRATION_TEST("post-integration-test"),
        VERIFY("verify"),
        INSTALL("install"),
        DEPLOY("deploy"),
        PRE_SITE("pre-site"),
        SITE("site"),
        POST_SITE("post-site"),
        SITE_DEPLOY("site-deploy");

        StandardPhase(String text) {
            this.text = text;
        }

        private final String text;

        static Optional<Phase> getPhase(String phase) {
            if (phase == null || phase.isEmpty()) {
                return Optional.empty();
            }
            return Arrays.stream(StandardPhase.values())
                    .filter(p -> phase.equalsIgnoreCase(p.text))
                    .map(Phase.class::cast)
                    .findFirst();
        }

        public String getText() {
            return text;
        }
    }
}

