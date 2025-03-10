import React, { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useWebSocket } from "../WebSocketContext"; 
import "../styles/GameSetup.css";
import crayons from "./assets/setupPage/crayons.png";
import hourglass from "./assets/setupPage/hourglass.png";
import right_bkg from "./assets/right_bkg.png";
import left_bkg from "./assets/left_bkg.png";

const WordSelection = () => {
  const { gameCode } = useParams(); 
  const navigate = useNavigate();
  const [randomWords, setRandomWords] = useState([]);
  const [drawer, setDrawer] = useState(null); // Stores actual drawer info
  const socket = useWebSocket(); 
  const userId = localStorage.getItem("userId"); // Get user ID

  const handleWordSelect = (word) => {
    if (!socket) {
        console.error("WebSocket is not connected.");
        return;
    }

    // Navigate to the game page after selecting a word
    navigate(`/game/${gameCode}`, { state: { choosingWord: false, isDrawer: true } });
    setTimeout(() => {
      console.log(`Drawer selected word: ${word}`);
      socket.send(`/select-word ${gameCode} ${word}`); // Send word selection to the backend

      // Notify backend that the drawer is now joining the game
      socket.send(`/drawer-joined ${gameCode}`);
    }, 100); // 100ms delay
  };

  useEffect(() => {
    console.log("WordSelection useEffect running...");
    console.log("Game Code:", gameCode);
    console.log("User ID:", userId);

    if (!socket) {
        console.log("WebSocket is null. Exiting useEffect.");
        return;
    }

    const handleMessage = (event) => {
        console.log("\n========== WebSocket MESSAGE RECEIVED ==========");
        console.log("Raw Data:", event.data);

        try {
            const message = JSON.parse(event.data);

            if (message.type === "WORDS") {
                console.log("WORDS event received. Updating random words...");
                setRandomWords(message.data);
                console.log("Word Choices:", message.data);
            }

            if (message.type === "GAME_PLAYERS") {
                console.log("GAME_PLAYERS event received. Finding drawer...");

                const players = JSON.parse(message.data);
                console.log("Full Players List:", players);

                // Find current user in player list
                const currentPlayer = players.find(player => player.id === userId);
                if (currentPlayer) {
                    console.log("Current User Found:", currentPlayer);
                    
                    // Ensure we use `username` not `name`
                    setDrawer({
                        avatar: currentPlayer.icon || "❓",
                        username: currentPlayer.username || "Unknown", // FIX HERE
                        score: currentPlayer.score !== undefined ? currentPlayer.score : "No score data",
                    });
                } else {
                    console.error("Current player not found in game data.");
                }
            }
        } catch (error) {
            console.error("Error parsing WebSocket message:", error);
        }
    };

    const attachWebSocketListeners = () => {
        console.log("Attaching WebSocket message listener...");
        socket.send(`/word-selection ${gameCode}`); // Request words
        socket.send(`/getgame ${gameCode}`); // Request player list

        socket.addEventListener("message", handleMessage);
    };

    if (socket.readyState === WebSocket.OPEN) {
        attachWebSocketListeners();
    } else {
        socket.onopen = attachWebSocketListeners;
    }

    return () => {
        socket.removeEventListener("message", handleMessage);
    };
  }, [socket, gameCode, userId]);

  return (
    <div className="setup_container">
      <img src={left_bkg} alt="Left Top" className="left-top-image" />
      <img src={right_bkg} alt="Right Top" className="right-top-image" />
      <div className="setup_logo"><h1>InkBlink</h1></div>
      <hr className="underline"/>

      <div className="ws_content_container">
        {/* Display actual user’s avatar, name, and score */}
        {drawer ? (
          <div className="ws_player_card">
            <span className="wordselect-avatar">{drawer.avatar}</span>  
            <div>
              <div className="ws_username">{drawer.username}</div> {/* FIXED */}
              <div className="ws_points">{drawer.score} points</div> 
            </div>
          </div>
        ) : (
          <p>Loading player data...</p>
        )}
        <div className="setup_image">
          <img src={crayons} alt="crayons" className="wordselect_image crayons" />
          <img src={hourglass} alt="hourglass" className="wordselect_image hourglass" />
        </div>
        <div className="ws_box">
          <div className="setup_join_title"> Select one of the four words to draw</div>
          <hr className="ws_underline"/>
          <div className="ws_button_group">
              {randomWords.map((word, index) => (
                  <button key={index} className="ws_btn" onClick={() => handleWordSelect(word)}>
                      {word}
                  </button>
              ))}
          </div>
        </div> 
      </div>
    </div>
  );
};
export default WordSelection;