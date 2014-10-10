package gr.uom.java.xmi;

public class XMIHeader {
    private String exporter;
    private String exporterVersion;
    private String metamodelName;
    private String metamodelVersion;

    public XMIHeader(String exporter, String exporterVersion, String metamodelName, String metamodelVersion) {
        this.exporter = exporter;
        this.exporterVersion = exporterVersion;
        this.metamodelName = metamodelName;
        this.metamodelVersion = metamodelVersion;
    }

    public String getExporter() {
        return exporter;
    }

    public String getExporterVersion() {
        return exporterVersion;
    }

    public String getMetamodelName() {
        return metamodelName;
    }

    public String getMetamodelVersion() {
        return metamodelVersion;
    }
}
