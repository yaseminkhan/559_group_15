import React from "react";
import { BrowserRouter as Router, Route, Routes } from "react-router-dom";
import WelcomePage from "./components/WelcomePage";
import GamePage from "./components/GamePage"; 
import SetupPage from "./components/GameSetup";
import WordSelection from "./components/WordSelection";
import JoinGamePage from "./components/JoinGamePage";

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<WelcomePage />} />
        <Route path="/game" element={<GamePage />} />
        <Route path= "/setup" element={<SetupPage />} />
        <Route path= "/wordselection" element={<WordSelection />} />
        <Route path="/join-game" element={<JoinGamePage />} />
      </Routes>
    </Router>
  );
}

export default App;