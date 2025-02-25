import React, { useState, useEffect } from 'react';
import { useWebSocket } from "../WebSocketContext"; 

const DebugPage = () => {
  const socket = useWebSocket(); // Get the single WebSocket instance
  const [messages, setMessages] = useState([]);
  const [clients, setClients] = useState([]);

  useEffect(() => {
    if (!socket) return; // socket might be null on first render

    // Handle incoming messages
    socket.onmessage = (event) => {
      const data = event.data;
      setMessages((prevMessages) => [...prevMessages, data]);

      if (data.startsWith('USER_ID:')) {
        const userId = data.split(':')[1];
        setClients((prevClients) => [...prevClients, userId]);
      } else if (data.startsWith('USER_DISCONNECTED:')) {
        const userId = data.split(':')[1];
        setClients((prevClients) =>
          prevClients.filter((client) => client !== userId)
        );
      } else if (data.startsWith('CONNECTED_USERS:')) {
        const users = JSON.parse(data.split(':')[1]);
        setClients(users);
      }
    };

    // Optional cleanup: if you have any event listeners, remove them here
    // But typically we just rely on onmessage being reassigned on each render
    // return () => {
    //   socket.onmessage = null;
    // };

  }, [socket]);

  // A helper to send messages
  const sendMessage = (message) => {
    if (socket && socket.readyState === WebSocket.OPEN) {
      socket.send(message);
    }
  };

  return (
    <div>
      <h1>WebSocket Debug Page</h1>
      <div>
        <input
          type="text"
          placeholder="Enter message"
          onKeyDown={(e) => {
            if (e.key === 'Enter') {
              sendMessage(e.target.value);
              e.target.value = '';
            }
          }}
        />
      </div>
      <div>
        <h2>Messages</h2>
        <ul>
          {messages.map((msg, index) => (
            <li key={index}>{msg}</li>
          ))}
        </ul>
      </div>
      <div>
        <h2>Connected Clients</h2>
        <ul>
          {clients.map((client, index) => (
            <li key={index}>{client}</li>
          ))}
        </ul>
      </div>
    </div>
  );
};

export default DebugPage;
