import React from "react";
import { BrowserRouter as Router, Route, Routes } from "react-router-dom";
import WelcomePage from "./components/WelcomePage";
import GamePage from "./components/GamePage"; 
import JoinGamePage from "./components/JoinGamePage"; 
import SetupPage from "./components/GameSetup";
import WordSelectionPage from "./components/WordSelection";

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<WelcomePage />} />
        <Route path="/game" element={<GamePage />} />
        <Route path="/join-game" element={<JoinGamePage />} />
        <Route path="/go-back" element={<WelcomePage />} />
        <Route path= "/setup" element={<SetupPage />} />
        <Route path= "/wordselection" element={<WordSelectionPage />} />
      </Routes>
    </Router>
  );
}

export default App;