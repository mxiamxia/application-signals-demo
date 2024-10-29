package org.springframework.samples.petclinic.customers.otel;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.Aggregation;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.AggregationTemporalitySelector;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@Slf4j
public class MetricProvider {
    private final Meter meter;

    public MetricProvider() {
        SdkMeterProvider sdkMeterProvider =
                SdkMeterProvider.builder()
                        .registerMetricReader(
                                PeriodicMetricReader.builder(OtlpHttpMetricExporter.builder()
                                                .setDefaultAggregationSelector(this::getAggregation)
                                                .setAggregationTemporalitySelector(AggregationTemporalitySelector.deltaPreferred()).build())
                                        .setInterval(Duration.ofSeconds(60))
                                        .build())
                        .build();
        OpenTelemetrySdk.builder().setMeterProvider(sdkMeterProvider).build();
        OpenTelemetry openTelemetry =
                OpenTelemetrySdk.builder().setMeterProvider(sdkMeterProvider).build();
        meter =
                openTelemetry
                        .meterBuilder("appsignals-metrics-test")
                        .setInstrumentationVersion("1.0.0")
                        .build();
        log.info("Metric provider initialized");
    }

    public Meter getMeter(String meterName) {
        return meter;
    }

    public LongHistogram createHistogramMetric(String metricName) {
        log.info("Created Histogram metric {}", metricName);
        return meter
                .histogramBuilder(metricName)
                .ofLongs()
                .setDescription("A distribution of job execution time using a custom scale value.")
                .setUnit("seconds")
                .build();
    }

    private Aggregation getAggregation(InstrumentType instrumentType) {
        if (instrumentType == InstrumentType.HISTOGRAM) {
            return Aggregation.base2ExponentialBucketHistogram();
        }
        return Aggregation.defaultAggregation();
    }

}
