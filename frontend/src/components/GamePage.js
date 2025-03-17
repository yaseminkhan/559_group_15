import React, { useState, useEffect, useRef } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useWebSocket } from "../WebSocketContext"; 
import "../styles/GamePage.css";
import Header from "./Header"; 
import ColourPalette from "./ColourPalette"; 
import Canvas from "./Canvas";
import ChatBox from "./ChatBox";

const GamePage = () => {
  const { socket, isConnected } = useWebSocket() || {};
  const [selectedColour, setSelectedColour] = useState("#000000");
  const clearCanvasRef = useRef(null);
  const [wordToDraw, setWordToDraw] = useState("");
  const [isChoosingWord, setIsChoosingWord] = useState(localStorage.getItem("isChoosingWord") === "true");
  const navigate = useNavigate();
  const location = useLocation();
  const userId = localStorage.getItem("userId");
  const [isDrawer, setIsDrawer] = useState(localStorage.getItem("isDrawer") === "true");
  const initGameCode = location.state?.gameCode || localStorage.getItem("gameCode");
  const [gameCode, setGameCode] = useState(initGameCode);
  
  useEffect(() => {
    if (!socket) return;

    setIsChoosingWord(localStorage.getItem("isChoosingWord") === "true");
    setWordToDraw(localStorage.getItem("wordToDraw"));
    const handleMessage = (event) => {


        if (event.data.startsWith("WORD_SELECTED:")) {
            setIsChoosingWord(false);
            localStorage.setItem("isChoosingWord", false);
            localStorage.setItem("wordToDraw", event.data.split(" ")[1]);
            setWordToDraw(event.data.split(" ")[1]);
        }

        if (event.data.startsWith("NEW_ROUND:")) {
          const roundInfo = event.data.split(" ");
          const newRound = parseInt(roundInfo[1]);
          const newDrawerId = roundInfo[3];

          console.log(`Starting Round ${newRound}, New Drawer: ${newDrawerId}`);

          if (userId === newDrawerId) {
              localStorage.setItem("isDrawer", true);
              setIsDrawer(true);
              setIsChoosingWord(false);
              console.log("You are the new drawer. Redirecting to word selection...");
              navigate(`/wordselection/${gameCode}`);
          } else {
              localStorage.setItem("isDrawer", false);
              localStorage.setItem("isChoosingWord", true);
              setIsDrawer(false);
              setIsChoosingWord(true);
          }
        }

        if (event.data.startsWith("GAME_OVER")) {
          console.log("Game has ended. Redirecting to the end game page...");
          navigate(`/endgame/${gameCode}`); 
        }
    };
    socket.addEventListener("message", handleMessage);
    return () => {
        socket.removeEventListener("message", handleMessage);
    };
  }, [socket, userId, navigate, gameCode, isDrawer, isConnected]);

  return (
    <div className="game-page">
      <Header isChoosingWord={isChoosingWord} gameCode={gameCode} />
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
        <div>
          isDrawer: {isDrawer ? "true" : "false"} <br />
          socket: {socket ? "connected" : "disconnected"} <br />
          isConnected: {isConnected ? "true" : "false"} <br />
          clearCanvasRef: {clearCanvasRef.current ? "exists" : "null"} <br />
          isChoosingWord: {isChoosingWord ? "true" : "false"} <br />
        </div>
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