package narrator.llm;

public enum ClientMode {
    ONLINE("online"), OFFLINE("offline");

    String label;

    ClientMode(String label) {
        this.label = label;
    }
}
