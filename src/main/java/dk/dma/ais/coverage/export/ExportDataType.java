package dk.dma.ais.coverage.export;

/**
 * Supported data types for export.
 */
public enum ExportDataType {
    RECEIVED_MESSAGES(0.8, 0.5), SIGNAL_STRENGTH(-75, -110);

    private final double greenThreshold;
    private final double redThreshold;

    ExportDataType(double greenThreshold, double redThreshold) {
        this.greenThreshold = greenThreshold;
        this.redThreshold = redThreshold;
    }

    public static ExportDataType forType(String type) {
        return ExportDataType.valueOf(type.toUpperCase());
    }

    public double greenThreshold() {
        return greenThreshold;
    }

    public double redThreshold() {
        return redThreshold;
    }
}
