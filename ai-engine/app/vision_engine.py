"""
Vision Engine module.
Handles camera capture and YOLOv11 object detection pipeline.
"""

import cv2
import base64
import asyncio
import logging
import threading
from typing import Optional, Callable, Dict, Any
from dataclasses import dataclass
from queue import Queue, Empty
from io import BytesIO
from PIL import Image
import numpy as np

from ultralytics import YOLO

from .config import settings

logger = logging.getLogger(__name__)


@dataclass
class Detection:
    """Represents a single detection result."""
    class_id: int
    class_name: str
    confidence: float
    bbox: tuple[int, int, int, int]  # x1, y1, x2, y2
    cropped_image_base64: str


class CameraHandler:
    """
    Manages video capture from a camera source.
    Runs frame capture in a background thread to avoid blocking.
    """
    
    def __init__(
        self,
        camera_id: str,
        source: str | int,
        on_detection: Optional[Callable[[str, Detection], None]] = None
    ):
        """
        Initialize the camera handler.
        
        Args:
            camera_id: Unique identifier for this camera
            source: Video source (URL, file path, or device index)
            on_detection: Callback function when a detection occurs
        """
        self.camera_id = camera_id
        self.source = source
        self.on_detection = on_detection
        
        self._capture: Optional[cv2.VideoCapture] = None
        self._running = False
        self._thread: Optional[threading.Thread] = None
        self._frame_queue: Queue = Queue(maxsize=10)
        self._frame_count = 0
        
        # Load YOLO model
        self._model: Optional[YOLO] = None
        self._model_lock = threading.Lock()
    
    def _get_model(self) -> YOLO:
        """Lazy load the YOLO model."""
        if self._model is None:
            with self._model_lock:
                if self._model is None:
                    logger.info(f"Loading YOLO model: {settings.model_path}")
                    self._model = YOLO(settings.model_path)
        return self._model
    
    def start(self) -> bool:
        """Start the camera capture thread."""
        if self._running:
            logger.warning(f"Camera {self.camera_id} is already running")
            return True
        
        try:
            self._capture = cv2.VideoCapture(self.source)
            if not self._capture.isOpened():
                logger.error(f"Failed to open camera source: {self.source}")
                return False
            
            self._running = True
            self._thread = threading.Thread(
                target=self._capture_loop,
                daemon=True,
                name=f"camera-{self.camera_id}"
            )
            self._thread.start()
            logger.info(f"Camera {self.camera_id} started successfully")
            return True
            
        except Exception as e:
            logger.exception(f"Error starting camera {self.camera_id}: {e}")
            return False
    
    def stop(self):
        """Stop the camera capture thread."""
        self._running = False
        if self._thread:
            self._thread.join(timeout=5.0)
            self._thread = None
        if self._capture:
            self._capture.release()
            self._capture = None
        logger.info(f"Camera {self.camera_id} stopped")
    
    def _capture_loop(self):
        """Main capture loop running in background thread."""
        model = self._get_model()
        
        while self._running and self._capture and self._capture.isOpened():
            ret, frame = self._capture.read()
            if not ret:
                logger.warning(f"Failed to read frame from camera {self.camera_id}")
                continue
            
            self._frame_count += 1
            
            # Skip frames for performance
            if self._frame_count % settings.frame_skip != 0:
                continue
            
            try:
                self._process_frame(frame, model)
            except Exception as e:
                logger.exception(f"Error processing frame: {e}")
    
    def _process_frame(self, frame: np.ndarray, model: YOLO):
        """Process a single frame through the detection pipeline."""
        # Run YOLO detection
        results = model(frame, verbose=False)
        
        for result in results:
            boxes = result.boxes
            if boxes is None:
                continue
            
            for i, box in enumerate(boxes):
                class_id = int(box.cls[0])
                confidence = float(box.conf[0])
                
                # Check if it's a target class with sufficient confidence
                if (class_id in settings.target_classes and 
                    confidence >= settings.detection_confidence):
                    
                    # Get bounding box coordinates
                    x1, y1, x2, y2 = map(int, box.xyxy[0].tolist())
                    
                    # Crop and encode the detection
                    cropped = frame[y1:y2, x1:x2]
                    image_base64 = self._encode_image(cropped)
                    
                    # Get class name
                    class_name = model.names.get(class_id, "unknown")
                    
                    detection = Detection(
                        class_id=class_id,
                        class_name=class_name,
                        confidence=confidence,
                        bbox=(x1, y1, x2, y2),
                        cropped_image_base64=image_base64
                    )
                    
                    logger.info(
                        f"Detection: {class_name} ({confidence:.2f}) "
                        f"at {(x1, y1, x2, y2)} on camera {self.camera_id}"
                    )
                    
                    # Trigger callback if set
                    if self.on_detection:
                        try:
                            self.on_detection(self.camera_id, detection)
                        except Exception as e:
                            logger.exception(f"Error in detection callback: {e}")
    
    @staticmethod
    def _encode_image(image: np.ndarray) -> str:
        """Encode a numpy array image to base64 string."""
        try:
            # Convert BGR to RGB
            rgb_image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
            pil_image = Image.fromarray(rgb_image)
            
            # Encode to JPEG
            buffer = BytesIO()
            pil_image.save(buffer, format="JPEG", quality=85)
            buffer.seek(0)
            
            # Base64 encode
            return base64.b64encode(buffer.getvalue()).decode("utf-8")
        except Exception as e:
            logger.error(f"Error encoding image: {e}")
            return ""
    
    @property
    def is_running(self) -> bool:
        """Check if the camera handler is currently running."""
        return self._running


class VisionEngine:
    """
    Manages multiple camera handlers and coordinates detection events.
    """
    
    def __init__(self):
        self.cameras: Dict[str, CameraHandler] = {}
        self._detection_callback: Optional[Callable] = None
    
    def set_detection_callback(self, callback: Callable[[str, Detection], None]):
        """Set the callback function for detection events."""
        self._detection_callback = callback
    
    def add_camera(self, camera_id: str, source: str | int) -> bool:
        """Add and start a new camera."""
        if camera_id in self.cameras:
            logger.warning(f"Camera {camera_id} already exists")
            return False
        
        handler = CameraHandler(
            camera_id=camera_id,
            source=source,
            on_detection=self._detection_callback
        )
        
        if handler.start():
            self.cameras[camera_id] = handler
            return True
        return False
    
    def remove_camera(self, camera_id: str) -> bool:
        """Stop and remove a camera."""
        if camera_id not in self.cameras:
            return False
        
        self.cameras[camera_id].stop()
        del self.cameras[camera_id]
        return True
    
    def get_camera_status(self, camera_id: str) -> Optional[Dict[str, Any]]:
        """Get the status of a specific camera."""
        if camera_id not in self.cameras:
            return None
        
        handler = self.cameras[camera_id]
        return {
            "camera_id": camera_id,
            "source": handler.source,
            "is_running": handler.is_running
        }
    
    def get_all_cameras(self) -> list[Dict[str, Any]]:
        """Get status of all cameras."""
        return [self.get_camera_status(cid) for cid in self.cameras]
    
    def stop_all(self):
        """Stop all cameras."""
        for handler in self.cameras.values():
            handler.stop()
        self.cameras.clear()


# Global vision engine instance
vision_engine = VisionEngine()
