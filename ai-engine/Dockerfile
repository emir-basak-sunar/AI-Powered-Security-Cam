FROM python:3.11-slim

WORKDIR /app

# Install system dependencies for OpenCV and PyAudio
RUN apt-get update && apt-get install -y \
    libgl1 \
    libglib2.0-0t64 \
    libsm6 \
    libxext6 \
    libxrender-dev \
    portaudio19-dev \
    python3-pyaudio \
    ffmpeg \
    && rm -rf /var/lib/apt/lists/*

# Copy requirements first for caching
COPY requirements.txt .

# Install CPU-only PyTorch first (much smaller than CUDA version: ~150MB vs ~670MB)
RUN pip install --no-cache-dir --timeout 300 \
    torch==2.1.2+cpu \
    torchvision==0.16.2+cpu \
    --extra-index-url https://download.pytorch.org/whl/cpu

# Install remaining dependencies (skip torch/torchvision since already installed)
RUN pip install --no-cache-dir --timeout 300 -r requirements.txt

# Copy application code
COPY app/ ./app/

# Download YOLO model on build
RUN python -c "from ultralytics import YOLO; YOLO('yolov8n.pt')"

# Expose port
EXPOSE 8000

# Run the application
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
