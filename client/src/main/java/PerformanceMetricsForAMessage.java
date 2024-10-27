public class PerformanceMetricsForAMessage {
    private String message;
    private Long latency;
    private Long processingTime;
    private Long networkPerformance;
    private Double jitter;
    private Double unprocessedRate;
    private Double throughput;

    public PerformanceMetricsForAMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getLatency() {
        return this.latency;
    }

    public void setLatency(Long latency) {
        this.latency = latency;
    }

    public Long getProcessingTime() {
        return this.processingTime;
    }

    public void setProcessingTime(Long processingTime) {
        this.processingTime = processingTime;
    }

    public Long getNetworkPerformance() {
        return this.networkPerformance;
    }

    public void setNetworkPerformance(Long networkPerformance) {
        this.networkPerformance = networkPerformance;
    }

    public Double getJitter() {
        return this.jitter;
    }

    public void setJitter(Double jitter) {
        this.jitter = jitter;
    }

    public Double getUnprocessedRate() {
        return this.unprocessedRate;
    }

    public void setUnprocessedRate(Double unprocessedRate) {
        this.unprocessedRate = unprocessedRate;
    }

    public Double getThroughput() {
        return this.throughput;
    }

    public void setThroughput(Double throughput) {
        this.throughput = throughput;
    }

    public boolean areMetricsComplete() {
        // Comprueba si todas las métricas están inicializadas (no son null)
        return latency != null
                && processingTime != null
                && networkPerformance != null
                && jitter != null
                && unprocessedRate != null
                && throughput != null;
    }

}
