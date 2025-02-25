import React, { useState, useEffect, useRef } from "react";
import { useLocation } from "react-router-dom";
import { useWebSocket } from "../WebSocketContext"; 
import "../styles/GamePage.css";
import Header from "./Header"; 
import ColourPalette from "./ColourPalette"; 
import Canvas from "./Canvas";
import ChatBox from "./ChatBox";

const GamePage = () => {
  const socket = useWebSocket();
  const [selectedColour, setSelectedColour] = useState("#000000");
  const clearCanvasRef = useRef(null);
  const [wordToDraw, setWordToDraw] = useState("");
  const [isChoosingWord, setIsChoosingWord] = useState(true);  
  const [timeLeft, setTimeLeft] = useState(60);
  
  const location = useLocation();
  const userId = localStorage.getItem("userId");
  const gameCode = location.state?.gameCode;
  
  // Ensure the correct role is assigned based on navigation state
  const [isDrawer, setIsDrawer] = useState(location.state?.isDrawer || false);

  useEffect(() => {
    if (!socket) return;

    const handleMessage = (event) => {
        console.log("WebSocket message:", event.data);

        if (event.data.startsWith("WORD_SELECTED:")) {
            const chosenWord = event.data.split(": ")[1];
            console.log("Game has officially started. Word is:", chosenWord);

            setWordToDraw(chosenWord);
            setIsChoosingWord(false); // Hide pop-up for all players
        }

        if (event.data.startsWith("DRAWER_JOINED:")) {
            console.log("Drawer has joined the game. Starting timer...");
            setTimeLeft(60); // Timer starts when drawer joins
        }
    };

    socket.addEventListener("message", handleMessage);

    return () => {
        socket.removeEventListener("message", handleMessage);
    };
  }, [socket]);

  return (
    <div className="game-page">
      <Header isChoosingWord={isChoosingWord} />

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

      {/* Show pop-up while waiting for drawer to pick a word (ONLY for guessers) */}
      {!isDrawer && isChoosingWord && (
        <div className="overlay">
          <div className="word-popup">
            <strong>Please wait...</strong>
            <p>The drawer is choosing a word.</p>
          </div>
        </div>
      )}
    </div>
  );
};

export default GamePage;