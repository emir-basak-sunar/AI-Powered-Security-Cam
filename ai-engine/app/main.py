"""
Main FastAPI application for the Sentry AI Engine.
Provides REST API endpoints and manages vision/audio pipelines.
"""

import asyncio
import logging
from contextlib import asynccontextmanager
from typing import Optional

from fastapi import FastAPI, HTTPException, BackgroundTasks
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

from .config import settings
from .vision_engine import vision_engine, Detection
from .audio_engine import audio_engine, AudioEvent
from .service_connector import connector

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
)
logger = logging.getLogger(__name__)


# ============================================================================
# Request/Response Models
# ============================================================================

class CameraRequest(BaseModel):
    """Request model for adding a camera."""
    camera_id: str
    source: str  # URL, file path, or device index as string


class AudioDeviceRequest(BaseModel):
    """Request model for adding an audio device."""
    device_id: str
    device_index: Optional[int] = None


class HealthResponse(BaseModel):
    """Health check response."""
    status: str
    version: str
    backend_connected: bool
    cameras_active: int
    audio_listeners_active: int


class StatusResponse(BaseModel):
    """Generic status response."""
    success: bool
    message: str


# ============================================================================
# Event Handlers
# ============================================================================

async def handle_visual_detection(camera_id: str, detection: Detection):
    """Handle a visual detection event by sending to backend."""
    description = (
        f"{detection.class_name.capitalize()} detected with "
        f"{detection.confidence:.1%} confidence"
    )
    
    await connector.send_alert(
        camera_id=camera_id,
        alert_type="VISUAL",
        description=description,
        image_base64=detection.cropped_image_base64
    )


async def handle_audio_event(event: AudioEvent):
    """Handle an audio event by sending to backend."""
    await connector.send_alert(
        camera_id=event.device_id,
        alert_type="AUDIO",
        description=event.description,
        image_base64="",
        timestamp=event.timestamp
    )


def sync_visual_callback(camera_id: str, detection: Detection):
    """Synchronous wrapper for async visual detection handler."""
    asyncio.create_task(handle_visual_detection(camera_id, detection))


def sync_audio_callback(event: AudioEvent):
    """Synchronous wrapper for async audio event handler."""
    asyncio.create_task(handle_audio_event(event))


# ============================================================================
# Application Lifecycle
# ============================================================================

@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan manager."""
    logger.info("Starting Sentry AI Engine...")
    
    # Set up callbacks
    vision_engine.set_detection_callback(sync_visual_callback)
    audio_engine.set_event_callback(sync_audio_callback)
    
    # Start default camera if configured
    if settings.default_camera_url:
        vision_engine.add_camera("default", settings.default_camera_url)
    
    logger.info("Sentry AI Engine started successfully")
    
    yield
    
    # Cleanup
    logger.info("Shutting down Sentry AI Engine...")
    vision_engine.stop_all()
    audio_engine.stop_all()
    await connector.close()
    logger.info("Sentry AI Engine stopped")


# ============================================================================
# FastAPI Application
# ============================================================================

app = FastAPI(
    title=settings.app_name,
    version=settings.app_version,
    description="Real-time security monitoring with AI-powered detection",
    lifespan=lifespan
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# ============================================================================
# Health & Status Endpoints
# ============================================================================

@app.get("/health", response_model=HealthResponse)
async def health_check():
    """Check the health status of the AI Engine."""
    backend_connected = await connector.health_check()
    
    return HealthResponse(
        status="healthy",
        version=settings.app_version,
        backend_connected=backend_connected,
        cameras_active=len(vision_engine.cameras),
        audio_listeners_active=len(audio_engine.listeners)
    )


@app.get("/")
async def root():
    """Root endpoint."""
    return {
        "service": settings.app_name,
        "version": settings.app_version,
        "status": "running"
    }


# ============================================================================
# Camera Management Endpoints
# ============================================================================

@app.get("/api/cameras")
async def list_cameras():
    """List all active cameras."""
    return {"cameras": vision_engine.get_all_cameras()}


@app.post("/api/cameras", response_model=StatusResponse)
async def add_camera(request: CameraRequest):
    """Add a new camera to the vision pipeline."""
    # Try to parse source as int for local device
    source = request.source
    try:
        source = int(source)
    except ValueError:
        pass  # Keep as string (URL/path)
    
    success = vision_engine.add_camera(request.camera_id, source)
    
    if success:
        return StatusResponse(
            success=True,
            message=f"Camera {request.camera_id} added successfully"
        )
    else:
        raise HTTPException(
            status_code=400,
            detail=f"Failed to add camera {request.camera_id}"
        )


@app.delete("/api/cameras/{camera_id}", response_model=StatusResponse)
async def remove_camera(camera_id: str):
    """Remove a camera from the vision pipeline."""
    success = vision_engine.remove_camera(camera_id)
    
    if success:
        return StatusResponse(
            success=True,
            message=f"Camera {camera_id} removed successfully"
        )
    else:
        raise HTTPException(
            status_code=404,
            detail=f"Camera {camera_id} not found"
        )


@app.get("/api/cameras/{camera_id}")
async def get_camera(camera_id: str):
    """Get status of a specific camera."""
    status = vision_engine.get_camera_status(camera_id)
    
    if status:
        return status
    else:
        raise HTTPException(
            status_code=404,
            detail=f"Camera {camera_id} not found"
        )


# ============================================================================
# Audio Device Management Endpoints
# ============================================================================

@app.get("/api/audio/devices")
async def list_audio_devices():
    """List available audio input devices."""
    return {"devices": audio_engine.list_devices()}


@app.get("/api/audio/listeners")
async def list_audio_listeners():
    """List all active audio listeners."""
    return {"listeners": audio_engine.get_all_listeners()}


@app.post("/api/audio/listeners", response_model=StatusResponse)
async def add_audio_listener(request: AudioDeviceRequest):
    """Add a new audio listener."""
    success = audio_engine.add_listener(
        device_id=request.device_id,
        device_index=request.device_index
    )
    
    if success:
        return StatusResponse(
            success=True,
            message=f"Audio listener {request.device_id} added successfully"
        )
    else:
        raise HTTPException(
            status_code=400,
            detail=f"Failed to add audio listener {request.device_id}"
        )


@app.delete("/api/audio/listeners/{device_id}", response_model=StatusResponse)
async def remove_audio_listener(device_id: str):
    """Remove an audio listener."""
    success = audio_engine.remove_listener(device_id)
    
    if success:
        return StatusResponse(
            success=True,
            message=f"Audio listener {device_id} removed successfully"
        )
    else:
        raise HTTPException(
            status_code=404,
            detail=f"Audio listener {device_id} not found"
        )


# ============================================================================
# Main Entry Point
# ============================================================================

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "app.main:app",
        host="0.0.0.0",
        port=8000,
        reload=settings.debug
    )
