import React, { useState } from "react";
import "../styles/GamePage.css";
import Header from "./Header"; 
import ColourPalette from "./ColourPalette"; 
import Canvas from "./Canvas";
import Chat from "./ChatBox";

const GamePage = () => {
  const [selectedColour, setSelectedColour] = useState("#000000");
  return (
    <div className="game-page">
      <Header /> 
      <div className="game-content">
        <ColourPalette selectedColour={selectedColour} setSelectedColour={setSelectedColour} />
        <Canvas selectedColour={selectedColour} />
        <Chat />
      </div>
    </div>
  );
};

export default GamePage;