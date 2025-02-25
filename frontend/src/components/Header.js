import React, { useState, useEffect } from "react";
import "../styles/Header.css";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faCrown } from "@fortawesome/free-solid-svg-icons";

const players = [
  { name: "Mary", avatar: "🐌", score: 245 },
  { name: "Bob", avatar: "🌵", score: 7653 },
  { name: "Jeff", avatar: "😃", score: 176 },
  { name: "Sarah", avatar: "☀️", score: 45729 },
  { name: "James", avatar: "🦃", score: 2148 },
  { name: "Daniel", avatar: "💬", score: 40652 },
  { name: "Joe", avatar: "📚", score: 4155 },
  { name: "Olivia", avatar: "🥶", score: 42385 },
];

const Header = ({ isChoosingWord }) => {
    const highestScore = Math.max(...players.map(player => player.score));
    const [timeLeft, setTimeLeft] = useState(60); // Timer starts at 60 seconds

    useEffect(() => {
        if (timeLeft === 0 || isChoosingWord) return; // Don't start if waiting for word selection

        const timer = setInterval(() => {
            setTimeLeft((prevTime) => prevTime - 1);
        }, 1000);

        return () => clearInterval(timer);
    }, [timeLeft, isChoosingWord]);

    return (
        <div className="header">
            <div className="clock">
                {timeLeft}
            </div>
            <div className="player-container">
                {players.map((player, index) => (
                    <div key={index} className="player">
                        {player.score === highestScore && <FontAwesomeIcon icon={faCrown} className="crown" />}
                        <span className="avatar">{player.avatar}</span>
                        <span className="name">{player.name}</span>
                        <span className="score">{player.score} pts</span>
                    </div>
                ))}
            </div>
        </div>
    );
};

export default Header;