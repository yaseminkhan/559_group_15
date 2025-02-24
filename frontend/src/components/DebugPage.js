import React, { useState, useEffect } from 'react';

const DebugPage = () => {
    const [messages, setMessages] = useState([]);
    const [connection, setConnection] = useState(null);
    const [clients, setClients] = useState([]);

    useEffect(() => {
        if (connection) return;

        console.log('Establishing WebSocket connection...');
        const ws = new WebSocket('ws://localhost:8887');
        setConnection(ws);

        ws.onopen = () => {
            console.log('Connected to WebSocket server');
        };

        ws.onmessage = (event) => {
            const data = event.data;
            setMessages((prevMessages) => [...prevMessages, data]);

            // Update clients list if the message contains client information
            if (data.startsWith('USER_ID:')) {
                const userId = data.split(':')[1];
                setClients((prevClients) => [...prevClients, userId]);
            } else if (data.startsWith('USER_DISCONNECTED:')) {
                const userId = data.split(':')[1];
                setClients((prevClients) => prevClients.filter(client => client !== userId));
            } else if (data.startsWith('CONNECTED_USERS:')) {
                const users = JSON.parse(data.split(':')[1]);
                setClients(users);
            }
        };

        ws.onclose = () => {
            console.log('Disconnected from WebSocket server');
        };

        ws.onerror = (error) => {
            console.error('WebSocket error:', error);
        };

        return () => {
            console.log('Closing WebSocket connection...');
            ws.close();
        };
    }, []);

    const sendMessage = (message) => {
        if (connection) {
            connection.send(message);
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