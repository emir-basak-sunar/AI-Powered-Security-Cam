"""
Configuration module for AI Engine.
Uses Pydantic Settings for environment-based configuration.
"""

from pydantic_settings import BaseSettings
from pydantic import Field
from typing import Optional
import os


class Settings(BaseSettings):
    """Application settings loaded from environment variables."""
    
    # Service Configuration
    app_name: str = "Sentry AI Engine"
    app_version: str = "1.0.0"
    debug: bool = Field(default=False, env="DEBUG")
    
    # Backend Connection
    backend_url: str = Field(
        default="http://management-server:8080",
        env="BACKEND_URL"
    )
    api_key: str = Field(
        default="changeme-api-key",
        env="AI_API_KEY"
    )
    
    # Vision Configuration
    model_path: str = Field(default="yolov8n.pt", env="MODEL_PATH")
    detection_confidence: float = Field(default=0.6, env="DETECTION_CONFIDENCE")
    target_classes: list[int] = [0]  # 0 = person in COCO dataset
    frame_skip: int = Field(default=2, env="FRAME_SKIP")  # Process every nth frame
    
    # Audio Configuration
    audio_threshold: float = Field(default=0.7, env="AUDIO_THRESHOLD")
    audio_sample_rate: int = Field(default=44100, env="AUDIO_SAMPLE_RATE")
    audio_chunk_size: int = Field(default=1024, env="AUDIO_CHUNK_SIZE")
    
    # Camera Defaults
    default_camera_url: Optional[str] = Field(default=None, env="DEFAULT_CAMERA_URL")
    
    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


# Global settings instance
settings = Settings()
