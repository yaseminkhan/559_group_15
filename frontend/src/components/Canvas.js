import React, { useRef, useEffect, useState } from "react";
import "../styles/GamePage.css";

const Canvas = ({ selectedColour, isDrawer, clearCanvasRef }) => {
  const canvasRef = useRef(null);
  const contextRef = useRef(null);
  const [drawing, setDrawing] = useState(false);

  useEffect(() => {
    const canvas = canvasRef.current;
    canvas.width = 800;
    canvas.height = 690;
    canvas.style.width = "100%";
    canvas.style.height = "100%";
    
    const context = canvas.getContext("2d");
    context.lineCap = "round";
    context.lineWidth = 5;
    context.strokeStyle = selectedColour; 
    contextRef.current = context;
  }, []);

  useEffect(() => {
    if (contextRef.current) {
      contextRef.current.strokeStyle = selectedColour;
    }
  }, [selectedColour]);

  const startDrawing = (event) => {
    if (!isDrawer) return; 
    contextRef.current.beginPath();
    contextRef.current.moveTo(event.nativeEvent.offsetX, event.nativeEvent.offsetY);
    setDrawing(true);
  };

  const draw = (event) => {
    if (!drawing || !isDrawer) return;
    contextRef.current.lineTo(event.nativeEvent.offsetX, event.nativeEvent.offsetY);
    contextRef.current.stroke();
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