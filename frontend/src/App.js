import React from "react";
import { BrowserRouter as Router, Route, Routes } from "react-router-dom";
import { WebSocketProvider } from "./WebSocketContext"; 
import WelcomePage from "./components/WelcomePage";
import GamePage from "./components/GamePage";
import SetupPage from "./components/GameSetup";
import WordSelection from "./components/WordSelection";
import JoinGamePage from "./components/JoinGamePage";
import EndGamePage from "./components/EndGamePage";

function App() {
    return (
        <WebSocketProvider>
            <Router>
                <Routes>
                    <Route path="/" element={<WelcomePage />} />
                    <Route path="/game" element={<GamePage />} />
                    <Route path="/setup/:gameCode" element={<SetupPage />} />
                    <Route path="/wordselection/:gameCode" element={<WordSelection />} />
                    <Route path="/join-game" element={<JoinGamePage />} />
                    <Route path="/endgame" element={<EndGamePage />} />
                </Routes>
            </Router>
        </WebSocketProvider>
    );
}

export default App;