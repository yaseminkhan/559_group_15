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
  const username = useRef("");
  const timestamp = useRef(0);
  const alias = "You";
  const inputRef = useRef(null); // Create a ref for the input element
  const handleMessage = useRef(null); // Function ref.
  const handleKeyDown = useRef(null); // Function ref.

  useEffect(() => {

    // const deconstructMessage = (data) => {
    //   data = data.split(" ", 3)[2];
    //   return JSON.parse(data);
    // }

    handleMessage.current = (e) => {

        if (!socket)
            return;
        
        // Clear chat when starting new round.
        // if (e.data.includes("NEW_ROUND: ")) {
        //   setMessages([]);
        // } else

        if (e.data.startsWith("HISTORY: ")) {
          const chat = e.data.split(" ", 2)[1];
          // console.log("===============================================")
          // console.log(chat);
          // console.log("===============================================")
          const messages = JSON.parse(chat);

          if (username.current === "") {
            for (const message of messages) {
              if (message.timestamp === timestamp.current)
                  username.current = message.sender;
            }
          }

          messages.forEach((msg) => {
            if (msg.sender === username.current) {
              msg.sender = alias;
            }
          });

          setMessages(messages);
        }
        // } else {
        //   const msg = deconstructMessage(e.data);

        //   if (username.current === "" && msg.timestamp === timestamp.current)
        //       username.current = msg.sender;

        //   if (msg.sender === username.current)
        //     msg.sender = alias;
        //   setMessages((prevMessages) => [...prevMessages, msg]);
        // }
    };

    socket.addEventListener("message", handleMessage.current);
    return () => {
      socket.removeEventListener("message", handleMessage.current);
    }
  }, [socket, timestamp, username, handleMessage]);

  useEffect(() => {
    const interval = 200;  // 200ms polling interval.
    const getChatHistory = () => {
      if (socket && socket.readyState === WebSocket.OPEN) {
        socket.send(`/chat-history ${gameCode}`)
      }
    };

    const intervalId = setInterval(getChatHistory, interval);
    return () => clearInterval(intervalId);
  }, [socket, gameCode]);

  // useEffect to attach a keydown listener to the input element
  useEffect(() => {
    
    handleKeyDown.current = (e) => {
      // Check for the Enter key and no shift modifier
      if (e.key === "Enter" && !e.shiftKey) {
        e.preventDefault(); // Prevent default behavior, like newline insertion
        if (newMessage.trim() === "") return; // Avoid sending empty messages

        // Construct the chat message
        const messageToSend = constructChatMessage(alias, newMessage);

        // Send the message only if the WebSocket is open
        if (!socket || socket.readyState !== WebSocket.OPEN)
          console.log("WebSocket is not open.");
        
        if (username.current === "")
          timestamp.current = messageToSend.timestamp;

        socket.send(`/chat ${gameCode} ` + JSON.stringify(messageToSend));
        console.log("Sent message:", newMessage);
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
  }, [newMessage, socket, gameCode, timestamp, username, handleKeyDown]);

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
