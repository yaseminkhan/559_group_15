import React, { useState } from "react";
import "../styles/GamePage.css";
import Header from "./Header"; 
import ColourPalette from "./ColourPalette"; 
import Canvas from "./Canvas";
import ChatBox from "./ChatBox";

const GamePage = () => {
  const [selectedColour, setSelectedColour] = useState("#000000");
  const [strokes, setStrokes] = useState([]); // store strokes 
  const [clearStrokes, setClearStrokes] = useState(false); // checks if canvas needs to be cleared

  const handleSendStroke = (stroke) => {
    // console.log("stroke data sent:", stroke);
    setStrokes((prevStrokes) => [...prevStrokes, stroke]);
  };

  const handleClearCanvas = () => {
    // console.log("canvas cleared for everyone");
    setStrokes([]); 
    setClearStrokes(true); 
    setTimeout(() => setClearStrokes(false), 100); 
  };

  // const handleReplayStrokes = () => {
  //   setStrokes([...strokes]); // this will trigger re-rendering
  // };

  // temporary placeholders
  const [isDrawer, setIsDrawer] = useState(true); 
  const [wordToDraw, setWordToDraw] = useState("Cactus"); 
  const [isChoosingWord, setIsChoosingWord] = useState(false);

  return (
    <div className="game-page">
      <Header /> 
      <div className="game-content">
        {isDrawer ? (
          <ColourPalette 
            selectedColour={selectedColour} 
            setSelectedColour={setSelectedColour} 
            clearCanvas={handleClearCanvas} // drawer can clear for all users
          />
        ) : (
          <div className="empty-placeholder"></div> 
        )}
        <Canvas 
          selectedColour={selectedColour} 
          isDrawer={isDrawer} 
          sendStroke={handleSendStroke} 
          strokes={strokes} // pass strokes to canvas
          clearStrokes={clearStrokes} // pass clear signal to canvas
        />
        <ChatBox isDrawer={isDrawer} wordToDraw={wordToDraw} />
      </div>

      {/* buttons for toggling roles and simulating pop-up */}
      <div className="button-container">
        <button className="role-toggle-btn" onClick={() => setIsDrawer(!isDrawer)}>
          switch to {isDrawer ? "guesser" : "drawer"} view
        </button>

        {/* <button onClick={handleReplayStrokes}>
          replay strokes
        </button> */}

        {!isDrawer && (
          <button onClick={() => setIsChoosingWord(true)}>
            simulate pop-up
          </button>
        )}
      </div>
      {!isDrawer && isChoosingWord && (
        <div className="overlay">
          <div className="word-popup">
            <strong>please wait...</strong>
            <p>the drawer is choosing a word.</p>
            <button onClick={() => setIsChoosingWord(false)}>
              close
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default GamePage;