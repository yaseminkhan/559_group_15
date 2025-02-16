import React from "react";
import "../styles/GamePage.css";
import Header from "./Header"; 
import ColourPalette from "./ColourPalette"; 
import Canvas from "./Canvas";
import Chat from "./ChatBox";

const GamePage = () => {
  return (
    <div className="game-page">
      <Header /> 
      <div className="game-content">
        <ColourPalette />
        <Canvas />
        <Chat />
      </div>
    </div>
  );
};

export default GamePage;