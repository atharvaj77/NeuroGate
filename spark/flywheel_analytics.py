from pyspark.sql import SparkSession
from pyspark.sql.functions import col, avg, count
import sys

def analyze_traces(input_path, output_path):
    """
    Analyzes agent traces to identify high-quality interactions ('Golden Traces')
    and compute provider performance metrics.
    """
    spark = SparkSession.builder \
        .appName("NeuroGateFlywheel") \
        .getOrCreate()

    # Read JSONL traces
    # Assuming the input path points to a directory of JSON log files
    try:
        df = spark.read.json(input_path)
    except Exception as e:
        print(f"Error reading input: {e}")
        spark.stop()
        return

    print("--- Provider Performance Metrics ---")
    
    # Calculate Provider Metrics
    # We assume 'provider' and 'latency' fields exist in the Trace model
    if 'provider' in df.columns:
        provider_metrics = df.groupBy("provider").agg(
            count("traceId").alias("total_requests"),
            avg("totalLatency").alias("avg_latency_ms"),
            avg("totalCostUsd").alias("avg_cost")
        )
        provider_metrics.show()
    else:
        print("Column 'provider' not found in dataset.")

    print("--- Extracting Golden Traces ---")
    
    # Filter "Golden Traces"
    # Criteria: Latency < 2000ms AND No Error
    # In a real scenario, this would use user feedback scores
    golden_traces = df.filter(
        (col("totalLatency") < 2000) & 
        (col("error").isNull() | (col("error") == False))
    )
    
    count_golden = golden_traces.count()
    print(f"Found {count_golden} golden traces.")
    
    # Export to JSONL
    golden_traces.write.mode("overwrite").json(output_path)
    print(f"Golden traces exported to {output_path}")

    spark.stop()

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python flywheel_analytics.py <input_path> [output_path]")
        sys.exit(1)
        
    input_file = sys.argv[1]
    output_dir = sys.argv[2] if len(sys.argv) > 2 else "refined_dataset"
    
    analyze_traces(input_file, output_dir)
