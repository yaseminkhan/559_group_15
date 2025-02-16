import React, { useRef, useEffect, useState } from "react";
import "../styles/GamePage.css";

const Canvas = ({ selectedColour }) => {
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
    context.strokeStyle = selectedColour; // Set initial brush colour
    contextRef.current = context;
  }, []);

  // ✅ Fix: Include selectedColour in dependency array
  useEffect(() => {
    if (contextRef.current) {
      contextRef.current.strokeStyle = selectedColour;
    }
  }, [selectedColour]); // ✅ Now ESLint warning is gone

  const startDrawing = (event) => {
    contextRef.current.beginPath();
    contextRef.current.moveTo(
      event.nativeEvent.offsetX,
      event.nativeEvent.offsetY
    );
    setDrawing(true);
  };
  
  const draw = (event) => {
    if (!drawing) return;
    
    contextRef.current.strokeStyle = selectedColour; // Ensure colour updates while drawing
    contextRef.current.lineTo(
      event.nativeEvent.offsetX,
      event.nativeEvent.offsetY
    );
    contextRef.current.stroke();
  };

  const stopDrawing = () => {
    contextRef.current.closePath();
    setDrawing(false);
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