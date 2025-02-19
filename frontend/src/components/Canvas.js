import React, { useRef, useEffect, useState } from "react";
import "../styles/GamePage.css";

const Canvas = ({ selectedColour, isDrawer, sendStroke, strokes, clearStrokes }) => {
  const canvasRef = useRef(null);
  const contextRef = useRef(null);
  const [drawing, setDrawing] = useState(false);
  const [currentStroke, setCurrentStroke] = useState([]);

  const clearCanvas = () => {
    const canvas = canvasRef.current;
    const context = canvas.getContext("2d");
    context.clearRect(0, 0, canvas.width, canvas.height);
  };

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
    setDrawing(true);
    setCurrentStroke([{ x: event.nativeEvent.offsetX, y: event.nativeEvent.offsetY, colour: selectedColour, timestamp: Date.now() }]);
    contextRef.current.beginPath();
    contextRef.current.moveTo(event.nativeEvent.offsetX, event.nativeEvent.offsetY);
  };

  const draw = (event) => {
    if (!drawing || !isDrawer) return;
    const newPoint = { x: event.nativeEvent.offsetX, y: event.nativeEvent.offsetY, colour: selectedColour, timestamp: Date.now() };
    setCurrentStroke(prev => [...prev, newPoint]);

    contextRef.current.lineTo(newPoint.x, newPoint.y);
    contextRef.current.stroke();
  };

  const stopDrawing = () => {
    if (!isDrawer) return;
    setDrawing(false);
    contextRef.current.closePath();
    
    if (currentStroke.length > 1) {
        sendStroke(currentStroke); 
    }
    setCurrentStroke([]); 
  };

  // clears canvas 
  useEffect(() => {
    if (clearStrokes) {
      clearCanvas();
    }
  }, [clearStrokes]); 

  // functionality for replay button to test strokes 
  /*
  useEffect(() => {
    console.log("Replaying strokes:", strokes);
    if (!strokes || strokes.length === 0) return;

    const canvas = canvasRef.current;
    const context = canvas.getContext("2d");

    context.clearRect(0, 0, canvas.width, canvas.height);

    setTimeout(() => {
        strokes.forEach((stroke, i) => {
            if (!stroke || stroke.length === 0) return;

            console.log(`Drawing stroke ${i}:`, stroke);

            context.beginPath();
            context.strokeStyle = stroke[0]?.colour || "black";
            context.lineWidth = 5;

            stroke.forEach((point, index) => {
                if (index === 0) {
                    context.moveTo(point.x, point.y);
                } else {
                    context.lineTo(point.x, point.y);
                    context.stroke();
                }
            });

            context.closePath();
        });
    }, 50); // small delay so you can see if it actually replays 
  }, [strokes]);
  */

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