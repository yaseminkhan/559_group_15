import React from "react";
import "../styles/GamePage.css";

const colours = ["#000000", "#A52A2A", "#007BFF", "#34C759", "#FFCC00", "#FF9500", "#FF3B30", "#AF52DE", "#FA96A9", "#FFFFFF"];

const ColourPalette = ({ selectedColour, setSelectedColour }) => {
  return (
    <div className="palette">
      {colours.map((colour) => (
        <button
          key={colour}
          className={`oval-btn ${selectedColour === colour ? "selected" : ""}`}
          onClick={() => setSelectedColour(colour)}
          style={{
            backgroundColor: colour,
            border: selectedColour === colour ? "2px solid white" : "none",
          }}
        />
      ))}
    </div>
  );
};

export default ColourPalette;