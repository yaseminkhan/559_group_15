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
        applyDrawing(pointData);
      }
    };

    socket.addEventListener("message", handleMessage);
    return () => socket.removeEventListener("message", handleMessage);
  }, [socket]);

  const applyDrawing = (point) => {
 
    const ctx = contextRef.current;
    const prevColor = ctx.strokeStyle;
    const prevWidth = ctx.lineWidth;
  
    // Use the incoming style
    ctx.strokeStyle = point.color;
    ctx.lineWidth = point.width;
  
    // Retrieve the last remote position (rx, ry)
    const { x: rx, y: ry } = lastPos.current;
  
    // If this is the first remote point, just move to it
    if (rx == null || ry == null) {
      ctx.beginPath();
      ctx.moveTo(point.x, point.y);
    } else {
      // Draw from the last remote point to this new one
      ctx.beginPath();
      ctx.moveTo(rx, ry);
      ctx.lineTo(point.x, point.y);
      ctx.stroke();
  
      // Reposition the pen
      ctx.beginPath();
      ctx.moveTo(point.x, point.y);
    }
  
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