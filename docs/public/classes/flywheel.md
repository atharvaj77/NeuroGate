# Flywheel Module Documentation

The **Flywheel** module handles the "Data Flywheel" capability. It collects user feedback on model outputs to create high-quality "Golden Datasets" for fine-tuning future models.

## API & Control

### `FlywheelController`
**Package:** `com.neurogate.flywheel`

**Purpose:**
Exposes REST endpoints for users (or the frontend) to submit feedback (thumbs up/down, corrections) and manage datasets.

**Key Methods:**
- `submitFeedback(FeedbackRequest request)`: Accepts feedback for a specific trace ID.
- `exportDataset(String model)`: Downloads the collected high-quality data as a JSONL file, formatted for fine-tuning (e.g., OpenAI format).
- `getGoldenInteractions()`: Retrieves the list of curated "Golden" interactions.

---

## Data Management

### `DatasetService`
**Package:** `com.neurogate.flywheel`

**Purpose:**
Manages the storage and lifecycle of feedback data. It filters for high-quality interactions (e.g., 5-star ratings or human corrections) to ensure only the best data makes it into the training set.

**Key Methods:**
- `recordFeedback(FeedbackRequest feedback)`: Stores incoming feedback.
- `getGoldenTraces(int minRating)`: Retrieves high-quality traces (rating >= minRating) for distillation.
- `exportGoldenDataset()`: Asynchronously processes and writes the golden dataset to disk.

**Context:**
The bridge between runtime operations and model improvement pipelines.

---

## Analytics Engine

### `flywheel_analytics.py`
**Path:** `spark/flywheel_analytics.py`

**Purpose:**
A **PySpark** script that processes massive volumes of trace data to identify high-performance interactions. It connects to the Data Flywheel by consuming the raw trace logs (conceptually) or refined datasets.

**Key Actions:**
-   **Golden Trace Extraction**: Filters traces with high user feedback and low latency (< 2s).
-   **Provider Metrics**: Aggregates success rates and costs per LLM provider.
-   **Export**: outputs a `refined_dataset.jsonl` for fine-tuning.
