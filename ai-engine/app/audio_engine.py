"""
Audio Engine module.
Handles audio capture and anomaly detection (screams, glass breaks, etc.).
"""

import asyncio
import logging
import threading
import numpy as np
from typing import Optional, Callable
from dataclasses import dataclass
from datetime import datetime
import struct

try:
    import pyaudio
    PYAUDIO_AVAILABLE = True
except ImportError:
    PYAUDIO_AVAILABLE = False
    logging.warning("PyAudio not available - audio detection disabled")

from .config import settings

logger = logging.getLogger(__name__)


@dataclass
class AudioEvent:
    """Represents an audio anomaly event."""
    timestamp: int  # Unix timestamp in milliseconds
    amplitude: float
    description: str
    device_id: str


class AudioListener:
    """
    Listens for audio anomalies from a microphone or audio stream.
    Uses PyAudio for cross-platform audio capture.
    """
    
    def __init__(
        self,
        device_id: str = "default",
        device_index: Optional[int] = None,
        on_event: Optional[Callable[[AudioEvent], None]] = None
    ):
        """
        Initialize the audio listener.
        
        Args:
            device_id: Identifier for this audio device
            device_index: PyAudio device index (None for default)
            on_event: Callback function when an audio event is detected
        """
        self.device_id = device_id
        self.device_index = device_index
        self.on_event = on_event
        
        self._running = False
        self._thread: Optional[threading.Thread] = None
        self._pyaudio: Optional['pyaudio.PyAudio'] = None
        self._stream = None
        
        # Detection parameters
        self.threshold = settings.audio_threshold
        self.sample_rate = settings.audio_sample_rate
        self.chunk_size = settings.audio_chunk_size
        
        # Cooldown to prevent flooding
        self._last_event_time = 0
        self._cooldown_ms = 2000  # 2 second cooldown between events
    
    def start(self) -> bool:
        """Start the audio listener thread."""
        if not PYAUDIO_AVAILABLE:
            logger.error("PyAudio is not available")
            return False
        
        if self._running:
            logger.warning(f"Audio listener {self.device_id} is already running")
            return True
        
        try:
            self._pyaudio = pyaudio.PyAudio()
            self._stream = self._pyaudio.open(
                format=pyaudio.paInt16,
                channels=1,
                rate=self.sample_rate,
                input=True,
                input_device_index=self.device_index,
                frames_per_buffer=self.chunk_size
            )
            
            self._running = True
            self._thread = threading.Thread(
                target=self._listen_loop,
                daemon=True,
                name=f"audio-{self.device_id}"
            )
            self._thread.start()
            logger.info(f"Audio listener {self.device_id} started successfully")
            return True
            
        except Exception as e:
            logger.exception(f"Error starting audio listener: {e}")
            self._cleanup()
            return False
    
    def stop(self):
        """Stop the audio listener thread."""
        self._running = False
        if self._thread:
            self._thread.join(timeout=5.0)
            self._thread = None
        self._cleanup()
        logger.info(f"Audio listener {self.device_id} stopped")
    
    def _cleanup(self):
        """Clean up PyAudio resources."""
        if self._stream:
            try:
                self._stream.stop_stream()
                self._stream.close()
            except Exception:
                pass
            self._stream = None
        
        if self._pyaudio:
            try:
                self._pyaudio.terminate()
            except Exception:
                pass
            self._pyaudio = None
    
    def _listen_loop(self):
        """Main audio capture loop running in background thread."""
        while self._running and self._stream:
            try:
                # Read audio chunk
                data = self._stream.read(self.chunk_size, exception_on_overflow=False)
                
                # Convert to numpy array
                audio_data = np.frombuffer(data, dtype=np.int16)
                
                # Calculate normalized amplitude (RMS)
                rms = np.sqrt(np.mean(audio_data.astype(np.float32) ** 2))
                normalized_amplitude = rms / 32768.0  # Normalize to 0-1 range
                
                # Check if above threshold
                if normalized_amplitude >= self.threshold:
                    self._handle_detection(normalized_amplitude)
                    
            except Exception as e:
                if self._running:
                    logger.exception(f"Error in audio loop: {e}")
    
    def _handle_detection(self, amplitude: float):
        """Handle a detected audio anomaly."""
        current_time = int(datetime.now().timestamp() * 1000)
        
        # Check cooldown
        if current_time - self._last_event_time < self._cooldown_ms:
            return
        
        self._last_event_time = current_time
        
        # Determine event description based on amplitude
        if amplitude >= 0.9:
            description = "Loud noise detected - possible scream or alarm"
        elif amplitude >= 0.8:
            description = "High amplitude sound detected"
        else:
            description = "Audio threshold exceeded"
        
        event = AudioEvent(
            timestamp=current_time,
            amplitude=amplitude,
            description=description,
            device_id=self.device_id
        )
        
        logger.info(f"Audio event: {description} (amplitude: {amplitude:.2f})")
        
        # Trigger callback
        if self.on_event:
            try:
                self.on_event(event)
            except Exception as e:
                logger.exception(f"Error in audio event callback: {e}")
    
    @property
    def is_running(self) -> bool:
        """Check if the listener is currently running."""
        return self._running


class AudioEngine:
    """
    Manages multiple audio listeners and coordinates audio events.
    """
    
    def __init__(self):
        self.listeners: dict[str, AudioListener] = {}
        self._event_callback: Optional[Callable] = None
    
    def set_event_callback(self, callback: Callable[[AudioEvent], None]):
        """Set the callback function for audio events."""
        self._event_callback = callback
    
    def add_listener(
        self,
        device_id: str,
        device_index: Optional[int] = None
    ) -> bool:
        """Add and start a new audio listener."""
        if device_id in self.listeners:
            logger.warning(f"Audio listener {device_id} already exists")
            return False
        
        listener = AudioListener(
            device_id=device_id,
            device_index=device_index,
            on_event=self._event_callback
        )
        
        if listener.start():
            self.listeners[device_id] = listener
            return True
        return False
    
    def remove_listener(self, device_id: str) -> bool:
        """Stop and remove an audio listener."""
        if device_id not in self.listeners:
            return False
        
        self.listeners[device_id].stop()
        del self.listeners[device_id]
        return True
    
    def get_listener_status(self, device_id: str) -> Optional[dict]:
        """Get the status of a specific listener."""
        if device_id not in self.listeners:
            return None
        
        listener = self.listeners[device_id]
        return {
            "device_id": device_id,
            "is_running": listener.is_running,
            "threshold": listener.threshold
        }
    
    def get_all_listeners(self) -> list[dict]:
        """Get status of all listeners."""
        return [self.get_listener_status(lid) for lid in self.listeners]
    
    def stop_all(self):
        """Stop all listeners."""
        for listener in self.listeners.values():
            listener.stop()
        self.listeners.clear()
    
    @staticmethod
    def list_devices() -> list[dict]:
        """List available audio input devices."""
        if not PYAUDIO_AVAILABLE:
            return []
        
        devices = []
        try:
            p = pyaudio.PyAudio()
            for i in range(p.get_device_count()):
                info = p.get_device_info_by_index(i)
                if info.get('maxInputChannels', 0) > 0:
                    devices.append({
                        "index": i,
                        "name": info.get('name', 'Unknown'),
                        "sample_rate": int(info.get('defaultSampleRate', 44100))
                    })
            p.terminate()
        except Exception as e:
            logger.error(f"Error listing audio devices: {e}")
        
        return devices


# Global audio engine instance
audio_engine = AudioEngine()
