import React, { useState, useRef, useEffect } from "react";
import "../styles/GamePage.css";
import { useWebSocket, tickClock } from "../WebSocketContext.js"; // Externally defined WebSocket instance

function constructChatMessage(sender, text, id) {
  return {
    sender,
    text,
    id,
    correct: false,
  };
}

const ChatBox = ({ isDrawer, wordToDraw }) => {
  const [messages, setMessages] = useState([]);
  const [newMessage, setNewMessage] = useState("");
  const gameCode = localStorage.getItem("gameCode");
  const { socket, isConnected, queueOrSendEvent } = useWebSocket() || {}; // Get WebSocket context
  const username = useRef("");
  const userId = localStorage.getItem("userId");
  const alias = "You";
  const inputRef = useRef(null); // Create a ref for the input element
  const handleMessage = useRef(null); // Function ref.
  const handleKeyDown = useRef(null); // Function ref.

  useEffect(() => {
    handleMessage.current = (e) => {
      if (!socket) return;

      if (e.data.startsWith("HISTORY: ")) {
        const i = e.data.indexOf(" ");
        const chat = e.data.slice(i + 1);
        const messages = JSON.parse(chat);

        if (username.current === "") {
          for (const message of messages) {
            if (message.id === userId) {
              username.current = message.sender;
              break;
            }
          }
        }

        messages.forEach((msg) => {
          if (msg.id === userId) {
            msg.sender = alias;
            if (msg.correct)
              msg.text = msg.text.replace(username.current, alias); // Show up as "You" guessed correctly.
          }
        });

        setMessages(messages);
      }
    };

    if (!socket || !isConnected) return;
    socket.addEventListener("message", handleMessage.current);
    return () => {
      socket.removeEventListener("message", handleMessage.current);
    };
  }, [socket, username, handleMessage, isConnected]);

  useEffect(() => {
    const interval = 200; // 200ms polling interval.
    const getChatHistory = () => {
      if (!socket || !isConnected) return;
      //console.log("is chat coming through");
      socket.send(`/chat-history ${gameCode}`);
    };

    const intervalId = setInterval(getChatHistory, interval);
    return () => clearInterval(intervalId);
  }, [socket, gameCode, isConnected]);

  // useEffect to attach a keydown listener to the input element
  useEffect(() => {
    handleKeyDown.current = (e) => {
      if (e.key === "Enter" && !e.shiftKey) {
        e.preventDefault();
        if (newMessage.trim() === "") return;
      
        const messageToSend = {
          sender: alias,
          text: newMessage,
          id: userId,
          correct: false,
        };
      
        queueOrSendEvent(`/chat ${gameCode}`, messageToSend);
        setNewMessage("");
      }
    };

    const inputElem = inputRef.current;
    if (inputElem !== null) {
      inputElem.addEventListener("keydown", handleKeyDown.current);
      return () => {
        inputElem.removeEventListener("keydown", handleKeyDown.current);
      };
    }
  }, [newMessage, socket, gameCode, username, handleKeyDown, isConnected]);

  return (
    <div className="chat-background">
      <div className="message-container">
        {messages.map((msg, index) => (
          <div
            key={index}
            className={`message ${msg.correct ? "correct-message" : ""}`}
          >
            {msg.correct ? (
              msg.text
            ) : (
              <>
                {" "}
                <strong>{msg.sender}: </strong>
                {msg.text}
              </>
            )}
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
