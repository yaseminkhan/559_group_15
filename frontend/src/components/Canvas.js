import React, { useRef, useEffect, useState } from "react";
import "../styles/GamePage.css";
import { useWebSocket } from "../WebSocketContext";
import { useLocation, useNavigate } from "react-router-dom";


const Canvas = ({ selectedColour, isDrawer, clearCanvasRef }) => {
  const canvasRef = useRef(null);
  const contextRef = useRef(null);
  
  const [drawing, setDrawing] = useState(false);

  const [lastX, setLastX] = useState(null);
  const [lastY, setLastY] = useState(null);

  const [lastIndex, setLastIndex] = useState(0);

  const lastPos = useRef({ x: null, y: null });

  const { socket, isConnected, queueOrSendEvent } = useWebSocket() || {}; // Get WebSocket context
  const gameCode = localStorage.getItem("gameCode");

  // Buffer to store points
  const pointBuffer = useRef([]);
  const location = useLocation();

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

  // Color Change
  useEffect(() => {
    if (contextRef.current) {
      contextRef.current.strokeStyle = selectedColour;
    }
  }, [selectedColour]);

  
  //Start Drawing
  const startDrawing = (event) => {
    if (!isDrawer || !isConnected) return; 
    setDrawing(true);
    
    const { offsetX, offsetY } = event.nativeEvent;
    setLastX(offsetX);
    setLastY(offsetY);

    const ctx = contextRef.current;
    ctx.beginPath();
    ctx.moveTo(offsetX, offsetY);

    // if (socket && gameCode) {
    if (gameCode) {
      const pointData = {
        x: offsetX,
        y: offsetY,
        color: ctx.strokeStyle,
        width: ctx.lineWidth,
        newStroke: true,
      };
      queueOrSendEvent(`/canvas-update ${gameCode}`, pointData);
      // socket.send(`/canvas-update ${gameCode} ${JSON.stringify(pointData)}`);
    }
  };

  const draw = (event) => {
    // if (!drawing || !isDrawer || !isConnected) return;
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
        newStroke: false,
      };
      // if (!isConnected) return;
      queueOrSendEvent(`/canvas-update ${gameCode}`, pointData);
      // socket.send(`/canvas-update ${gameCode} ${JSON.stringify(pointData)}`);
    }

    // Update locally
    setLastX(offsetX);
    setLastY(offsetY);
  };

  const stopDrawing = () => {
    if (!isDrawer || !isConnected) return; 
    contextRef.current.closePath();
    setDrawing(false);
  };

  const clearCanvas = () => {
    const canvas = canvasRef.current;
    const context = canvas.getContext("2d");
    context.clearRect(0, 0, canvas.width, canvas.height);

    if (!isConnected) return;
    socket.send(`/clear-canvas ${gameCode}`);
    
  };

  useEffect(() => {
    if (clearCanvasRef) {
      clearCanvasRef.current = clearCanvas;
    }
  }, [clearCanvasRef]);
  
  useEffect(() => {
    if (!socket) return;

    const handleMessage = (event) => {
      const data = event.data;

      if (data.startsWith("CANVAS_HISTORY")) {
        const parts = data.split(" ", 4);
        const newIndex = parseInt(parts[1], 10);
        const prefix = `CANVAS_HISTORY ${parts[1]} `;
        const jsonString = data.substring(prefix.length);
        const strokes = JSON.parse(jsonString);

        strokes.forEach((stroke) => {
          applyDrawing(stroke);
        });
        setLastIndex(newIndex);
      } else if (data.startsWith("CANVAS_CLEAR") || data.startsWith("ROUND_OVER")) {
        const canvas = canvasRef.current;
        const context = canvas.getContext("2d");
        context.clearRect(0, 0, canvas.width, canvas.height);
      }
    };

    socket.addEventListener("message", handleMessage);
    return () => {
      socket.removeEventListener("message", handleMessage);
    };
  }, [socket, isConnected]);

  // Request Canvas Data In Intervals and Refresh Canvas with New Data
  useEffect(() => {
    if (!socket) return;
    
    if (socket && isConnected) {
      const intervalId = setInterval(() => {
        socket.send(`/getcanvas ${gameCode} ${lastIndex}`);
      }, 100); // 16 ms
      return () => clearInterval(intervalId);
    }

    // clear canvas
    const ctx = contextRef.current;
    pointBuffer.current.forEach((point) => {
      applyDrawing(point);
    });

  }, [socket, gameCode, lastIndex, isConnected]);
  
 
  const applyDrawing = (point) => {
    const ctx = contextRef.current;
    
    // If the point marks the beginning of a new stroke, reset the last position
    if (point.newStroke) {
      lastPos.current = { x: null, y: null };
    }
    
    // Save current drawing settings
    const prevColor = ctx.strokeStyle;
    const prevWidth = ctx.lineWidth;
    
    ctx.strokeStyle = point.color;
    ctx.lineWidth = point.width;
  
    // If there is a previous point, draw a line; otherwise, draw a dot
    if (lastPos.current.x != null && lastPos.current.y != null) {
      ctx.beginPath();
      ctx.moveTo(lastPos.current.x, lastPos.current.y);
      ctx.lineTo(point.x, point.y);
      ctx.stroke();
    } else {
      ctx.beginPath();
      ctx.arc(point.x, point.y, point.width / 2, 0, Math.PI * 2);
      ctx.fillStyle = point.color;
      ctx.fill();
    }
    
    // Update the last position
    lastPos.current = { x: point.x, y: point.y };
  
    // Restore previous drawing settings
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