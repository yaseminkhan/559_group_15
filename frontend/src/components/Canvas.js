import React, { useRef, useEffect, useState } from "react";
import "../styles/GamePage.css";
import { useWebSocket } from "../WebSocketContext";

const Canvas = ({ selectedColour, isDrawer, clearCanvasRef }) => {
  const canvasRef = useRef(null);
  const contextRef = useRef(null);

  const [drawing, setDrawing] = useState(false);
  const [lastIndex, setLastIndex] = useState(0);
  const [lastX, setLastX] = useState(null);
  const [lastY, setLastY] = useState(null);
  const lastPos = useRef({ x: null, y: null });

  // Use a ref to hold the current stroke index. Increment on every new stroke.
  const currentStrokeIndexRef = useRef(0);
  // Use a ref to hold the current point index for the current stroke.
  const currentPointIndexRef = useRef(0);
  // Track the last applied stroke index (for separating strokes on the receiver)
  const lastStrokeIndexApplied = useRef(null);
  // Track the last applied point index in a stroke (for ordering)
  const lastPointIndexApplied = useRef(null);

  const { socket, isConnected, queueOrSendEvent } = useWebSocket() || {};
  const gameCode = localStorage.getItem("gameCode");
  const [historyReceived, setHistoryReceived] = useState(false);

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

  useEffect(() => {
    if (contextRef.current) {
      contextRef.current.strokeStyle = selectedColour;
    }
  }, [selectedColour]);

  // Reset lastPos on connection status changes.
  useEffect(() => {
    lastPos.current = { x: null, y: null };
  }, [isConnected]);

  // Reset lastPos when the WebSocket reconnects.
  useEffect(() => {
    if (!socket) return;
  
    const handleOpen = () => {
      console.log("WebSocket reconnected: resetting lastPos");
      lastPos.current = { x: null, y: null };
    };
  
    socket.addEventListener("open", handleOpen);
    return () => socket.removeEventListener("open", handleOpen);
  }, [socket]);

  // When a new stroke starts, reset the point counter.
  const startDrawing = (event) => {
    if (!isDrawer) return;
    setDrawing(true);

    // Increment the stroke index for a new stroke.
    currentStrokeIndexRef.current += 1;
    // Reset the point index for the new stroke.
    currentPointIndexRef.current = 0;

    const { offsetX, offsetY } = event.nativeEvent;
    setLastX(offsetX);
    setLastY(offsetY);

    const ctx = contextRef.current;
    ctx.beginPath();
    // Draw the starting dot for the new stroke.
    ctx.arc(offsetX, offsetY, ctx.lineWidth / 2, 0, Math.PI * 2);
    ctx.fillStyle = ctx.strokeStyle;
    ctx.fill();

    // Send the starting point with strokeIndex and pointIndex (0)
    const pointData = {
      x: offsetX,
      y: offsetY,
      color: ctx.strokeStyle,
      width: ctx.lineWidth,
      strokeIndex: currentStrokeIndexRef.current,
      pointIndex: currentPointIndexRef.current,
    };
    queueOrSendEvent(`/canvas-update ${gameCode}`, pointData);
  };

  const draw = (event) => {
    if (!drawing || !isDrawer) return;

    const ctx = contextRef.current;
    const { offsetX, offsetY } = event.nativeEvent;
    
    // Draw the line on the canvas (from last known coordinates)
    ctx.beginPath();
    ctx.moveTo(lastX, lastY);
    ctx.lineTo(offsetX, offsetY);
    ctx.stroke();

    setLastX(offsetX);
    setLastY(offsetY);

    // Increment the point index for this stroke.
    currentPointIndexRef.current += 1;
    // Include the current stroke and point indices in the data.
    const pointData = {
      x: offsetX,
      y: offsetY,
      color: ctx.strokeStyle,
      width: ctx.lineWidth,
      strokeIndex: currentStrokeIndexRef.current,
      pointIndex: currentPointIndexRef.current,
    };
    console.log("Sending event:", `/canvas-update ${gameCode}`, pointData);
    queueOrSendEvent(`/canvas-update ${gameCode}`, pointData);
  };

  const stopDrawing = () => {
    setDrawing(false);
    contextRef.current.closePath();
  };

  const clearCanvas = () => {
    const canvas = canvasRef.current;
    const ctx = canvas.getContext("2d");
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    if (isConnected) {
      queueOrSendEvent("/clear-canvas", { gameCode });
    }
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
        if (isDrawer && historyReceived) return;

        const parts = data.split(" ", 4);
        const newIndex = parseInt(parts[1], 10);
        const prefix = `CANVAS_HISTORY ${parts[1]} `;
        const jsonString = data.substring(prefix.length);
        const strokes = JSON.parse(jsonString);

        strokes.forEach((stroke) => {
          applyDrawing(stroke);
        });
        setLastIndex(newIndex);
        
        if (isDrawer) {
          setHistoryReceived(true);
        }
      } else if (data.startsWith("CANVAS_CLEAR") || data.startsWith("ROUND_OVER")) {
        const canvas = canvasRef.current;
        if (!canvas) return;
        const ctx = canvas.getContext("2d");
        if (!ctx) return;
        ctx.clearRect(0, 0, canvas.width, canvas.height);
      }
    };

    socket.addEventListener("message", handleMessage);
    return () => socket.removeEventListener("message", handleMessage);
  }, [socket, isConnected]);

  useEffect(() => {
    if (!socket || !isConnected) return;

    const intervalId = setInterval(() => {
      socket.send(`/getcanvas ${gameCode} ${lastIndex}`);
    }, 100);

    return () => clearInterval(intervalId);
  }, [socket, gameCode, lastIndex, isConnected]);

  // Updated applyDrawing that uses both strokeIndex and pointIndex for ordering.
  const applyDrawing = (point) => {
    const ctx = contextRef.current;
    // For a new stroke, or if there is no previous point recorded:
    if (point.strokeIndex !== lastStrokeIndexApplied.current) {
      // Start a new stroke: update both stroke and point indices.
      lastStrokeIndexApplied.current = point.strokeIndex;
      lastPointIndexApplied.current = point.pointIndex;
      // Set the starting point without drawing a connecting line.
      lastPos.current = { x: point.x, y: point.y };
      ctx.beginPath();
      ctx.arc(point.x, point.y, point.width / 2, 0, Math.PI * 2);
      ctx.fillStyle = point.color;
      ctx.fill();
    } else {
      // Same stroke: check the ordering using pointIndex.
      // If the current point's index is exactly one higher than the last applied,
      // draw a connecting line; otherwise, reinitialize the lastPos.
      if (lastPointIndexApplied.current !== null &&
          point.pointIndex === lastPointIndexApplied.current + 1) {
        ctx.beginPath();
        ctx.moveTo(lastPos.current.x, lastPos.current.y);
        ctx.lineTo(point.x, point.y);
        ctx.stroke();
      } else {
        // Not in order—this might happen if points arrive out-of-order.
        // In that case, simply draw a dot for the new point.
        ctx.beginPath();
        ctx.arc(point.x, point.y, point.width / 2, 0, Math.PI * 2);
        ctx.fillStyle = point.color;
        ctx.fill();
      }
      // Update ordering and last position.
      lastPos.current = { x: point.x, y: point.y };
      lastPointIndexApplied.current = point.pointIndex;
    }
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
