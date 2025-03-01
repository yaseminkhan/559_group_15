import React, { useRef, useEffect, useState } from "react";
import "../styles/GamePage.css";
import { useWebSocket } from "../WebSocketContext";

const Canvas = ({ selectedColour, isDrawer, clearCanvasRef }) => {
  const canvasRef = useRef(null);
  const contextRef = useRef(null);
  
  const [drawing, setDrawing] = useState(false);

  const [lastX, setLastX] = useState(null);
  const [lastY, setLastY] = useState(null);

  const lastPos = useRef({ x: null, y: null });

  const socket = useWebSocket();
  const gameCode = localStorage.getItem("gameCode");

  // Buffer to store points
  const pointBuffer = useRef([]);

  /**
   * Setup canvas context on component mount
   */
  useEffect(() => {
    const canvas = canvasRef.current;
    canvas.width = 800;
    canvas.height = 690;
    canvas.style.width = "100%";
    canvas.style.height = "100%";
    
    const context = canvas.getContext("2d");
    context.lineCap = "round";
    context.lineJoin = "round";
    context.lineWidth = 5;
    context.strokeStyle = selectedColour; 
    contextRef.current = context;
  }, []);

  /**
   * Color Change
   */

  useEffect(() => {
    if (contextRef.current) {
      contextRef.current.strokeStyle = selectedColour;
    }
  }, [selectedColour]);

  /**
   * Start Drawing Function  - sets the drawing state to true and sets the last x and y coordinates
   */
  const startDrawing = (event) => {
    if (!isDrawer) return; 
    setDrawing(true);
    
    const { offsetX, offsetY } = event.nativeEvent;
    setLastX(offsetX);
    setLastY(offsetY);

    // reset for new storke 
    const ctx = contextRef.current;
    ctx.beginPath();
    ctx.moveTo(offsetX, offsetY);
  };

  const draw = (event) => {
    if (!drawing || !isDrawer) return;

    const ctx = contextRef.current;
    const {offsetX, offsetY} = event.nativeEvent;
    
    ctx.beginPath();
    ctx.moveTo(lastX, lastY);
    ctx.lineTo(offsetX, offsetY);
    ctx.stroke();

    ctx.beginPath();
    ctx.moveTo(lastX, lastY);

    if (socket && gameCode) {
      const pointData = {
        x: offsetX,
        y: offsetY,
        color: ctx.strokeStyle,
        width: ctx.lineWidth,
      };
      socket.send(`/canvas-update ${gameCode} ${JSON.stringify(pointData)}`);
    }

    // Update locally
    setLastX(offsetX);
    setLastY(offsetY);
  };

  const stopDrawing = () => {
    if (!isDrawer) return; 
    contextRef.current.closePath();
    setDrawing(false);
  };

  const clearCanvas = () => {
    const canvas = canvasRef.current;
    const context = canvas.getContext("2d");
    context.clearRect(0, 0, canvas.width, canvas.height);

    if (socket && gameCode && isDrawer) {
      socket.send(`/clear-canvas ${gameCode}`);
    }
  };

  useEffect(() => {
    if (clearCanvasRef) {
      clearCanvasRef.current = clearCanvas;
    }
  }, [clearCanvasRef]);
  
  useEffect(() => {
    if (!socket || isDrawer) return;

    const handleMessage = (event) => {
      if (event.data.startsWith("CANVAS_UPDATE")) {
        const jsonString = event.data.replace("CANVAS_UPDATE", "").trim();
        const pointData = JSON.parse(jsonString);
        pointBuffer.current.push(pointData);
      } else if (event.data.startsWith("CLEAR_CANVAS")) {
        clearCanvas();
      }
    };

    socket.addEventListener("message", handleMessage);
    return () => socket.removeEventListener("message", handleMessage);
  }, [socket]);

  // Process the buffer at regular intervals
  useEffect(() => {
    const interval = setInterval(() => {
      if (pointBuffer.current.length > 0) {
        const points = pointBuffer.current.splice(0, pointBuffer.current.length);
        points.forEach(point => applyDrawing(point));
      }
    }, 16); //16ms

    return () => clearInterval(interval);
  }, []);

  const applyDrawing = (point) => {
    const ctx = contextRef.current;
    const prevColor = ctx.strokeStyle;
    const prevWidth = ctx.lineWidth;

    // Use the incoming style
    ctx.strokeStyle = point.color;
    ctx.lineWidth = point.width;

    // Draw a small circle at the new point
    ctx.beginPath();
    ctx.arc(point.x, point.y, point.width / 2, 0, Math.PI * 2);
    ctx.fillStyle = point.color;
    ctx.fill();

    // Update remoteLastPos to the new point
    lastPos.current = { x: point.x, y: point.y };

    // Restore style
    ctx.strokeStyle = prevColor;
    ctx.lineWidth = prevWidth;
  };

  return (
    <canvas
      ref={canvasRef}
      onMouseDown={startDrawing}
      onMouseMove={draw}
      onMouseUp={stopDrawing}
      onMouseLeave={stopDrawing}
      className="drawing-canvas"
    />
  );
};

export default Canvas;