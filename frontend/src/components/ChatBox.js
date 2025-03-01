import React, { useState, useRef, useEffect } from "react";
import "../styles/GamePage.css";
import { useWebSocket } from "../WebSocketContext.js"; // Externally defined WebSocket instance

function constructChatMessage(sender, text) {
  return {
    sender,
    text,
    timestamp: performance.now(),
    correct: false,
  };
}


const ChatBox = ({ isDrawer, wordToDraw }) => {
  const [messages, setMessages] = useState([]);
  const [newMessage, setNewMessage] = useState("");
  const gameCode = localStorage.getItem("gameCode");
  const socket = useWebSocket(); // Using the external WebSocket instance
  // const [username, setUsername] = useState("");
  const username = useRef("");
  const timestamp = useRef(0);
  // const [timestamp, setTimestamp] = useState(0);
  const alias = "You";

  // Create a ref for the input element
  const inputRef = useRef(null);

  useEffect(() => {

    const deconstructMessage = (data) => {
      data = data.split(" ", 3)[2];
      return JSON.parse(data);
    }

    const handleMessage = (event) => {
        if (!socket || !event.data.includes("/chat "))
            return;
        const msg = deconstructMessage(event.data);
        if (username.current === "") {
          if (msg.timestamp === timestamp.current) {
            username.current = msg.sender;
          }
        }
        if (msg.sender !== username.current)
          messages.push(msg);
    }

    socket.addEventListener("message", handleMessage);
    return () => {
      socket.removeEventListener("message", handleMessage);
    }
  }, [messages, socket, timestamp, username]);

  // useEffect to attach a keydown listener to the input element
  useEffect(() => {
    
    const handleKeyDown = (e) => {
      // Check for the Enter key and no shift modifier
      if (e.key === "Enter" && !e.shiftKey) {
        e.preventDefault(); // Prevent default behavior, like newline insertion
        if (newMessage.trim() === "") return; // Avoid sending empty messages

        // Construct the chat message
        const messageToSend = constructChatMessage(alias, newMessage);

        // Send the message only if the WebSocket is open
        if (!socket || socket.readyState !== WebSocket.OPEN)
          console.log("WebSocket is not open.");
        
        if (username.current === "") {
          timestamp.current = messageToSend.timestamp;
        }

        socket.send(`/chat ${gameCode} ` + JSON.stringify(messageToSend));
        messages.push(messageToSend);
        console.log("Sent message:", newMessage);
        setNewMessage("");
      }
    };

    const inputElem = inputRef.current;
    if (inputElem)
      inputElem.addEventListener("keydown", handleKeyDown);

    return () => {
      inputElem.removeEventListener("keydown", handleKeyDown);
    };
  }, [newMessage, socket, gameCode, timestamp, username]);

  return (
    <div className="chat-background">
      <div className="message-container">
            {messages.map((msg, index) => (
                <div key={index} className={`message ${msg.correct ? "correct-message" : ""}`}>
                    {msg.correct ? msg.text : <> <strong>{msg.sender}: </strong>{msg.text}</>} 
                </div>
            ))}
      </div>
      <div className="input-container">
        {!isDrawer ? (
          <input
            ref={inputRef}
            type="text"
            className="text-box"
            placeholder="Enter a guess..."
            value={newMessage}
            onChange={(e) => setNewMessage(e.target.value)}
          />
        ) : (
          <div className="word-box">
            The word is: <strong>{wordToDraw}</strong>
          </div>
        )}
      </div>
    </div>
  );
};

export default ChatBox;
