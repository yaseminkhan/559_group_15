import React from "react";
import "../styles/GamePage.css";
import { useWebSocket } from "../WebSocketContext";



const colours = [
  "#000000", "#A52A2A", "#007BFF", "#34C759",
  "#FFCC00", "#FF9500", "#FF3B30", "#AF52DE",
  "#FA96A9", "white"
];

const ColourPalette = ({ selectedColour, setSelectedColour, clearCanvas }) => {
  const gameCode = localStorage.getItem("gameCode");
  const { socket, isConnected } = useWebSocket() || {}; // Get WebSocket context

  const handleColourClick = (colour) => {
    if (colour === "white") {
      clearCanvas();
      // This is obv a work around, but it's a quick fix for now
      socket.send(`/clear-canvas ${gameCode}`);
    } else {
      setSelectedColour(colour);
    }
  };

  return (
    <div className="palette">
      {colours.map((colour) => (
        <button
          key={colour}
          className={`oval-btn ${selectedColour === colour ? "selected" : ""}`}
          onClick={() => handleColourClick(colour)}
          style={{
            backgroundColor: colour === "white" ? "#FFFFFF" : colour, 
            border: selectedColour === colour ? "2px solid white" : "none",
            display: "flex",
            justifyContent: "center",
            alignItems: "center",
            position: "relative", 
          }}
        >
          {colour === "white" && <span className="clear-btn">Clear</span>}
        </button>
      ))}
    </div>
  );
};

export default ColourPalette;