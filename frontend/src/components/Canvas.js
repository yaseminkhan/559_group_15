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

  const currentStrokeIndexRef = useRef(0);
  const currentPointIndexRef = useRef(0);
  const lastStrokeIndexApplied = useRef(null);
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

  useEffect(() => {
    lastPos.current = { x: null, y: null };
  }, [isConnected]);

  useEffect(() => {
    if (!socket) return;
  
    const handleOpen = () => {
      console.log("WebSocket reconnected: resetting lastPos");
      lastPos.current = { x: null, y: null };
    };
  
    socket.addEventListener("open", handleOpen);
    return () => socket.removeEventListener("open", handleOpen);
  }, [socket]);

  const startDrawing = (event) => {
    if (!isDrawer) return;
    setDrawing(true);

    currentStrokeIndexRef.current += 1;
    currentPointIndexRef.current = 0;

    const { offsetX, offsetY } = event.nativeEvent;
    setLastX(offsetX);
    setLastY(offsetY);

    const ctx = contextRef.current;
    ctx.beginPath();
    ctx.arc(offsetX, offsetY, ctx.lineWidth / 2, 0, Math.PI * 2);
    ctx.fillStyle = ctx.strokeStyle;
    ctx.fill();

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
    
    ctx.beginPath();
    ctx.moveTo(lastX, lastY);
    ctx.lineTo(offsetX, offsetY);
    ctx.stroke();

    setLastX(offsetX);
    setLastY(offsetY);

    currentPointIndexRef.current += 1;
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

  const applyDrawing = (point) => {
    const ctx = contextRef.current;

    const prevColor = ctx.strokeStyle;
    const prevWidth = ctx.lineWidth;

    ctx.strokeStyle = point.color;
    ctx.lineWidth = point.width;

    if (point.strokeIndex !== lastStrokeIndexApplied.current) {
      lastStrokeIndexApplied.current = point.strokeIndex;
      lastPointIndexApplied.current = point.pointIndex;
      lastPos.current = { x: point.x, y: point.y };
      ctx.beginPath();
      ctx.arc(point.x, point.y, point.width / 2, 0, Math.PI * 2);
      ctx.fillStyle = point.color;
      ctx.fill();
    } else {
      if (lastPointIndexApplied.current !== null &&
          point.pointIndex === lastPointIndexApplied.current + 1) {
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
      lastPos.current = { x: point.x, y: point.y };
      lastPointIndexApplied.current = point.pointIndex;
      ctx.strokeStyle = prevColor;
      ctx.lineWidth = prevWidth;
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
