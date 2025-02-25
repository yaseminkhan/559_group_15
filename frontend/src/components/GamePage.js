import React, { useState, useRef } from "react";
import { useLocation } from "react-router-dom";
import "../styles/GamePage.css";
import Header from "./Header"; 
import ColourPalette from "./ColourPalette"; 
import Canvas from "./Canvas";
import ChatBox from "./ChatBox";

const GamePage = () => {
  const [selectedColour, setSelectedColour] = useState("#000000");
  const clearCanvasRef = useRef(null);
  
  // Temporary placeholders
  // const [isDrawer, setIsDrawer] = useState(true); 
  const [wordToDraw, setWordToDraw] = useState("Cactus"); 
  // const [isChoosingWord, setIsChoosingWord] = useState(false);
  const location = useLocation();
  const isChoosingWord = location.state?.choosingWord || false;
  const drawerName = location.state?.drawerName || "Host";
  const isDrawer = location.state?.isDrawer || true;

  return (
    <div className="game-page">
      <Header /> 
      <div className="game-content">
        {isDrawer ? (
          <ColourPalette 
            selectedColour={selectedColour} 
            setSelectedColour={setSelectedColour} 
            clearCanvas={() => clearCanvasRef.current && clearCanvasRef.current()}
          />
        ) : (
          <div className="empty-placeholder"></div> 
        )}
        <Canvas selectedColour={selectedColour} isDrawer={isDrawer} clearCanvasRef={clearCanvasRef} />
        <ChatBox isDrawer={isDrawer} wordToDraw={wordToDraw} />
      </div>

      {/* Buttons for toggling roles and simulating pop-up */}
      {/* <div className="button-container">
        <button className="role-toggle-btn" onClick={() => setIsDrawer(!isDrawer)}>
          Switch to {isDrawer ? "Guesser" : "Drawer"} View
        </button>

        {!isDrawer && (
          <button onClick={() => setIsChoosingWord(true)}>
            Simulate Pop-up
          </button>
        )}
      </div> */}
      {/* {!isDrawer && isChoosingWord && (
        <div className="overlay">
          <div className="word-popup">
            <strong>Please wait...</strong>
            <p>The drawer is choosing a word.</p>
            <button onClick={() => setIsChoosingWord(false)}>
              Close
            </button>
          </div>
        </div>
      )} */}
    </div>
  );
};

export default GamePage;