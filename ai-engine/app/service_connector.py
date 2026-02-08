"""
Service Connector module.
Handles HTTP communication with the Java backend management server.
"""

import httpx
import asyncio
import logging
from typing import Optional
from datetime import datetime

from .config import settings

logger = logging.getLogger(__name__)


class ServiceConnector:
    """
    Async HTTP client for communicating with the Spring Boot management server.
    Uses httpx for async HTTP requests with retry logic.
    """
    
    def __init__(self):
        self.backend_url = settings.backend_url
        self.api_key = settings.api_key
        self.client: Optional[httpx.AsyncClient] = None
        self._lock = asyncio.Lock()
    
    async def _get_client(self) -> httpx.AsyncClient:
        """Get or create the HTTP client."""
        if self.client is None:
            async with self._lock:
                if self.client is None:
                    self.client = httpx.AsyncClient(
                        timeout=httpx.Timeout(30.0),
                        headers={
                            "Content-Type": "application/json",
                            "X-API-KEY": self.api_key
                        }
                    )
        return self.client
    
    async def send_alert(
        self,
        camera_id: str,
        alert_type: str,
        description: str,
        image_base64: str = "",
        timestamp: Optional[int] = None
    ) -> bool:
        """
        Send an alert to the management server.
        
        Args:
            camera_id: Unique identifier for the camera
            alert_type: Either "VISUAL" or "AUDIO"
            description: Human-readable description of the alert
            image_base64: Base64 encoded image data (for visual alerts)
            timestamp: Unix timestamp in milliseconds
        
        Returns:
            True if alert was successfully sent, False otherwise
        """
        if timestamp is None:
            timestamp = int(datetime.now().timestamp() * 1000)
        
        payload = {
            "cameraId": camera_id,
            "alertType": alert_type,
            "description": description,
            "imageBase64": image_base64,
            "timestamp": timestamp
        }
        
        try:
            client = await self._get_client()
            response = await client.post(
                f"{self.backend_url}/api/v1/alerts",
                json=payload
            )
            
            if response.status_code in (200, 201):
                logger.info(f"Alert sent successfully: {alert_type} from {camera_id}")
                return True
            else:
                logger.error(
                    f"Failed to send alert: {response.status_code} - {response.text}"
                )
                return False
                
        except httpx.RequestError as e:
            logger.error(f"Network error sending alert: {e}")
            return False
        except Exception as e:
            logger.exception(f"Unexpected error sending alert: {e}")
            return False
    
    async def health_check(self) -> bool:
        """Check if the backend server is reachable."""
        try:
            client = await self._get_client()
            response = await client.get(f"{self.backend_url}/actuator/health")
            return response.status_code == 200
        except Exception:
            return False
    
    async def close(self):
        """Close the HTTP client."""
        if self.client:
            await self.client.aclose()
            self.client = None


# Global connector instance
connector = ServiceConnector()
