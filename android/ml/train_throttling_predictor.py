"""
╔══════════════════════════════════════════════════════════════════════╗
║   NMDLOCK v3.0 — Thermal Throttling Predictor Training Script       ║
║   TensorFlow Lite Model để dự đoán thermal throttling               ║
║   Input: 22 features from system telemetry                          ║
║   Output: Binary (throttle trong 5 phút tới hay không)             ║
╚══════════════════════════════════════════════════════════════════════╝
"""

import numpy as np
import pandas as pd
import tensorflow as tf
from tensorflow import keras
from sklearn.preprocessing import StandardScaler
from sklearn.model_selection import TimeSeriesSplit
import os

# ── Constants ──
WINDOW_SIZE = 30  # 30 samples = 90 giây history (3s/sample)
FUTURE_WINDOW = 100  # 100 samples = 5 phút dự đoán
FEATURE_COUNT = 22


def create_synthetic_dataset(n_samples=10000):
    """
    Tạo dữ liệu synthetic cho training
    Trong production, cần thay bằng dữ liệu thật từ device telemetry
    
    Features:
    - cpu_temp_now, gpu_temp_now, cpu_usage_now
    - ram_used_pct, battery_temp, charging, brightness
    - cpu_temp_ema, gpu_temp_ema (EMA smoothed)
    - cpu_temp_slope, gpu_temp_slope (linear regression)
    - cpu_temp_mean, cpu_temp_std, cpu_temp_max, cpu_temp_min
    - cpu_temp_range, cpu_zscore
    - temp_increasing, usage_trend
    """
    np.random.seed(42)
    
    # Base temperature với noise
    base_temp = 35.0
    data = {
        'cpu_temp': base_temp + np.cumsum(np.random.randn(n_samples) * 0.3),
        'gpu_temp': base_temp + np.cumsum(np.random.randn(n_samples) * 0.2),
        'cpu_usage': 30 + np.random.randn(n_samples) * 15,
        'ram_used_pct': 40 + np.random.randn(n_samples) * 10,
        'battery_temp': 30 + np.random.randn(n_samples) * 2,
        'charging': np.random.choice([0, 1], n_samples, p=[0.7, 0.3]),
        'brightness': np.random.randint(30, 255, n_samples),
        'cpu_freq': 1800 + np.random.randn(n_samples) * 200,  # MHz
    }
    
    # Tạo throttle events (nhiệt > 45°C hoặc freq drop > 30%)
    df = pd.DataFrame(data)
    df['cpu_temp'] = df['cpu_temp'].clip(30, 50)
    df['gpu_temp'] = df['gpu_temp'].clip(30, 48)
    
    # Labels: throttle = temp > 45 or freq < 70% max
    max_freq = df['cpu_freq'].max()
    df['throttled'] = ((df['cpu_temp'] > 45) | 
                       (df['cpu_freq'] < 0.7 * max_freq)).astype(float)
    
    return df


def create_features(df, window_size=WINDOW_SIZE, future_window=FUTURE_WINDOW):
    """
    Feature engineering từ sliding window
    
    Returns:
        X: Feature matrix (n_samples, 22)
        y: Label vector (n_samples, 1)
    """
    features = []
    labels = []
    
    for i in range(window_size, len(df) - future_window):
        window = df.iloc[i - window_size:i]
        future = df.iloc[i:i + future_window]
        
        feature = {
            # Current state
            'cpu_temp_now': window['cpu_temp'].iloc[-1],
            'gpu_temp_now': window['gpu_temp'].iloc[-1],
            'cpu_usage_now': window['cpu_usage'].iloc[-1],
            'ram_used_pct': window['ram_used_pct'].iloc[-1],
            'battery_temp': window['battery_temp'].iloc[-1],
            'charging': window['charging'].iloc[-1],
            'brightness': window['brightness'].iloc[-1] / 255.0,
            
            # EMA (exponential moving average)
            'cpu_temp_ema': window['cpu_temp'].ewm(alpha=0.3).mean().iloc[-1],
            'gpu_temp_ema': window['gpu_temp'].ewm(alpha=0.3).mean().iloc[-1],
            
            # Slope (linear regression - rate of change)
            'cpu_temp_slope': np.polyfit(range(window_size), 
                                         window['cpu_temp'], 1)[0],
            'gpu_temp_slope': np.polyfit(range(window_size), 
                                         window['gpu_temp'], 1)[0],
            
            # Statistics
            'cpu_temp_mean': window['cpu_temp'].mean(),
            'cpu_temp_std': window['cpu_temp'].std(),
            'cpu_temp_max': window['cpu_temp'].max(),
            'cpu_temp_min': window['cpu_temp'].min(),
            'cpu_temp_range': window['cpu_temp'].max() - window['cpu_temp'].min(),
            
            # Z-Score current
            'cpu_zscore': ((window['cpu_temp'].iloc[-1] - 
                           window['cpu_temp'].mean()) / 
                          (window['cpu_temp'].std() + 1e-6)),
            
            # Trend features
            'temp_increasing': float(window['cpu_temp'].iloc[-1] > 
                                     window['cpu_temp'].iloc[0]),
            'usage_trend': np.polyfit(range(window_size), 
                                      window['cpu_usage'], 1)[0],
            
            # GPU stats
            'gpu_temp_mean': window['gpu_temp'].mean(),
            'gpu_temp_max': window['gpu_temp'].max(),
        }
        features.append(feature)
        
        # Label: Có throttle trong 5 phút tới không?
        future_max_temp = future['cpu_temp'].max()
        future_throttled = future['throttled'].max()
        labels.append(float(future_throttled))
    
    return pd.DataFrame(features), np.array(labels)


def build_model():
    """
    Lightweight neural network cho mobile deployment
    
    Architecture:
    - Input: 22 features
    - Hidden: 32 → 16 → 8 neurons (ReLU + BatchNorm + Dropout)
    - Output: 1 neuron (sigmoid)
    """
    model = keras.Sequential([
        keras.layers.Input(shape=(FEATURE_COUNT,)),
        
        # Layer 1
        keras.layers.Dense(32, activation='relu',
                          kernel_regularizer=keras.regularizers.l2(0.001)),
        keras.layers.BatchNormalization(),
        keras.layers.Dropout(0.2),
        
        # Layer 2
        keras.layers.Dense(16, activation='relu'),
        keras.layers.BatchNormalization(),
        keras.layers.Dropout(0.2),
        
        # Layer 3
        keras.layers.Dense(8, activation='relu'),
        
        # Output
        keras.layers.Dense(1, activation='sigmoid')
    ])
    
    model.compile(
        optimizer=keras.optimizers.Adam(learning_rate=0.001),
        loss='binary_crossentropy',
        metrics=['accuracy', keras.metrics.AUC(name='auc')]
    )
    
    return model


def train_and_export():
    """Training pipeline hoàn chỉnh"""
    print("=" * 60)
    print("NMDLOCK Thermal Throttling Predictor Training")
    print("=" * 60)
    
    # 1. Load / generate data
    print("\n[1/5] Generating synthetic training data...")
    df = create_synthetic_dataset(10000)
    print(f"    Generated {len(df)} samples")
    
    # 2. Feature engineering
    print("\n[2/5] Creating features...")
    X, y = create_features(df)
    print(f"    Features: {X.shape[1]}")
    print(f"    Samples: {len(X)}")
    print(f"    Throttle ratio: {y.mean():.2%}")
    
    # 3. Scale features
    print("\n[3/5] Scaling features...")
    scaler = StandardScaler()
    X_scaled = scaler.fit_transform(X)
    
    # 4. Train with timeseries cross-validation
    print("\n[4/5] Training with TimeSeries CV...")
    tscv = TimeSeriesSplit(n_splits=3)
    
    histories = []
    fold = 0
    for train_idx, val_idx in tscv.split(X_scaled):
        fold += 1
        print(f"\n    Fold {fold}/3...")
        
        X_train, X_val = X_scaled[train_idx], X_scaled[val_idx]
        y_train, y_val = y[train_idx], y[val_idx]
        
        model = build_model()
        
        history = model.fit(
            X_train, y_train,
            validation_data=(X_val, y_val),
            epochs=30,
            batch_size=32,
            callbacks=[
                keras.callbacks.EarlyStopping(
                    patience=5,
                    restore_best_weights=True,
                    monitor='val_auc',
                    mode='max'
                ),
                keras.callbacks.ReduceLROnPlateau(
                    factor=0.5,
                    patience=3,
                    min_lr=1e-6
                )
            ],
            verbose=1
        )
        histories.append(history)
    
    # 5. Convert to TFLite với quantization
    print("\n[5/5] Converting to TFLite with quantization...")
    
    # Train final model on all data
    final_model = build_model()
    final_model.fit(
        X_scaled, y,
        epochs=20,
        batch_size=32,
        verbose=0
    )
    
    # Standard conversion
    converter = tf.lite.TFLiteConverter.from_keras_model(final_model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    converter.target_spec.supported_types = [tf.float16]
    tflite_quantized = converter.convert()
    
    # Save model
    output_dir = os.path.join(os.path.dirname(__file__), 'models')
    os.makedirs(output_dir, exist_ok=True)
    
    tflite_path = os.path.join(output_dir, 'throttling_predictor.tflite')
    with open(tflite_path, 'wb') as f:
        f.write(tflite_quantized)
    
    # Save scaler
    import joblib
    scaler_path = os.path.join(output_dir, 'feature_scaler.pkl')
    joblib.dump(scaler, scaler_path)
    
    # Save model summary
    print(f"\n✅ Model saved to: {tflite_path}")
    print(f"✅ Scaler saved to: {scaler_path}")
    print(f"   Model size: {len(tflite_quantized) / 1024:.1f} KB")
    
    # Evaluate
    y_pred = (final_model.predict(X_scaled) > 0.5).astype(float)
    accuracy = (y_pred.flatten() == y).mean()
    print(f"   Final accuracy: {accuracy:.2%}")
    
    return tflite_path


if __name__ == '__main__':
    train_and_export()
