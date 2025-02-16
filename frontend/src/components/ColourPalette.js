import React, { useState } from "react";
import "../styles/GamePage.css";

const colours = ["#000000", "#522504", "#007BFF", "#34C759", "#FFCC00", "#FF9500", "#FF3B30", "#AF52DE", "#FA96A9", "#FFFFFF"];

const ColourPalette = () => {
  const [selectedColour, setSelectedColour] = useState("#000000");

  return (
    <div className="palette">
      {colours.map((colour) => (
        <button
          className={`oval-btn ${selectedColour === colour ? "selected" : ""}`}
          key={colour}
          onClick={() => setSelectedColour(colour)}
          style={{ backgroundColor: colour }}
        ></button>
      ))}
    </div>
  );
};

export default ColourPalette;