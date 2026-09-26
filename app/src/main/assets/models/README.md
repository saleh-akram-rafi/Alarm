# Models Directory

Place your TensorFlow Lite feature extractor model here:
File: `mobilenet_v2_features.tflite`

### Model Specification:
- Input shape: `[1, 224, 224, 3]` (Float32, normalized [-1.0, 1.0])
- Output shape: `[1, 1280]` (Float32 feature embedding vector)

*Note: If this file is not present, Discipline Alarm automatically activates its built-in 512-dimensional Spatial Color & Edge Feature Descriptor, ensuring the application remains 100% functional and testable immediately.*
