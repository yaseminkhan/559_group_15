import React, { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useWebSocket } from "../WebSocketContext"; 
import "../styles/GameSetup.css";
import crayons from "./assets/setupPage/crayons.png"
import hourglass from "./assets/setupPage/hourglass.png"
import right_bkg from "./assets/right_bkg.png";
import left_bkg from "./assets/left_bkg.png";

const WordSelection = ({ currentDrawer }) => {
  // just using mary for now, need to add functionality once we have actual users
  const { gameCode } = useParams(); 
  const drawer = currentDrawer || { name: "Mary", avatar: "🐌", score: 245 };
  const [randomWords, setRandomWords] = useState([]);
  const socket = useWebSocket(); 


  useEffect(() => {
    console.log("WordSelection useEffect running...");
    console.log("Game Code:", gameCode);

    if (!socket) {
        console.log("WebSocket is null. Exiting useEffect.");
        return;
    }

    const handleMessage = (event) => {
        console.log("\n========== WebSocket MESSAGE RECEIVED ==========");
        console.log("Raw Data:", event.data);

        try {
            if (!event.data.startsWith("{")) {
                console.log("Skipping non-JSON message:", event.data);
                return;
            }
            const message = JSON.parse(event.data);

            if (message.type === "WORDS") {
                console.log("WORDS event received. Updating random words...");
                console.log("Parsed Words:", message.data);
                const wordsData = message.data;
                setRandomWords(wordsData);

                console.log("Updated Random Words List:");
                wordsData.forEach((word) => {
                    console.log(`   - ${word}`);
                });
            }
        } catch (error) {
            console.error("Error parsing WebSocket message:", error);
        }
    };

    const attachWebSocketListeners = () => {
        console.log("Attaching WebSocket message listener...");

        console.log(`Requesting random words for game: ${gameCode}`);
        socket.send(`/word-selection ${gameCode}`);

        socket.addEventListener("message", handleMessage);
    };

    if (socket.readyState === WebSocket.OPEN) {
        console.log("WebSocket is already open. Proceeding...");
        attachWebSocketListeners();
    } else {
        console.log("WebSocket is not open. Waiting for connection...");
        socket.onopen = () => {
            console.log("WebSocket just opened. Attaching event listeners...");
            attachWebSocketListeners();
        };
    }

    return () => {
        socket.removeEventListener("message", handleMessage);
    };
}, [socket, gameCode]);

  return (
    <div className="setup_container">
      <img src={left_bkg} alt="Left Top" className="left-top-image" />
      <img src={right_bkg} alt="Right Top" className="right-top-image" />
      <div className="setup_logo"><h1>InkBlink</h1></div>
      <hr className="underline"/>

      <div className="ws_content_container">
        {/* show the actual user’s avatar, name, and score */}
        <div className="ws_player_card">
          <span className="wordselect-avatar">{drawer.avatar}</span>  
          <div>
            <div className="ws_username">{drawer.name}</div> 
            <div className="ws_points">{drawer.score} points</div> 
          </div>
        </div>
        <div className="setup_image">
          <img src={crayons} alt="crayons" className="wordselect_image crayons" />
          <img src={hourglass} alt="hourglass" className="wordselect_image hourglass" />
        </div>
        <div className="ws_box">
          <div className="setup_join_title"> Select one of the four words to draw</div>
          <hr className="ws_underline"/>
          <div className="ws_button_group">
            {randomWords.map((word, index) => (
              <button key={index} className="ws_btn">{word}</button>
            ))}
          </div>
        </div> 
      </div>
    </div>
  );
};
export default WordSelection;
