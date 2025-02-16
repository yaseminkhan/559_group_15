import React, { useState } from "react";
import "../styles/GamePage.css";

const correctAnswer = "Cactus"; // replace with actual game logic

const Chat = () => {
    const [messages, setMessages] = useState([
        { sender: "Sarah", text: "Palm Tree"},
        { sender: "John", text: "Idk"}
    ]);
    const [newMessage, setNewMessage] = useState("");
    const senderName = "You"; 

    const handleKeyDown = (e) => {
        if (e.key === "Enter" && !e.shiftKey) {
            e.preventDefault();
            if (newMessage.trim() !== "") {
                const isCorrect = newMessage.trim().toLowerCase() === correctAnswer.toLowerCase();
                const displayText = isCorrect ? `${senderName} got it!` : newMessage;

                setMessages([...messages, { sender: senderName, text: displayText, correct: isCorrect }]);
                setNewMessage("");
            }
        }
    };

  return (
    <div className="chat-background">
        <div className="round-info">Round 1/3</div>
        <div className="message-container">
            {messages.map((msg, index) => (
            <div key={index} className={`message ${msg.correct ? "correct-message" : ""}`}>
                {msg.correct ? msg.text : <> <strong>{msg.sender}: </strong>{msg.text}</>} 
            </div>
            ))}
        </div>
        <input
            type="text"
            className="text-box"
            placeholder="Enter a guess..."
            value={newMessage}
            onChange={(e) => setNewMessage(e.target.value)}
            onKeyDown={handleKeyDown}
        />
        </div>
  );
};

export default Chat;