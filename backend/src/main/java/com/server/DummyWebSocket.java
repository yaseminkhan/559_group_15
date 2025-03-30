package com.server;

import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.protocols.IProtocol;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import org.java_websocket.framing.Framedata;
import org.java_websocket.enums.Opcode;
import javax.net.ssl.SSLSession;
import org.java_websocket.enums.ReadyState;

import java.util.Collection;

public class DummyWebSocket implements WebSocket {

    @Override
    public InetSocketAddress getRemoteSocketAddress() {
        return new InetSocketAddress("127.0.0.1", 0); // Dummy address
    }

    @Override
    public void send(String message) {
        System.out.println("DummyWebSocket sent message: " + message);
    }

    @Override
    public void send(ByteBuffer data) {
        System.out.println("DummyWebSocket sent ByteBuffer data.");
    }

    @Override
    public void send(byte[] data) {
        System.out.println("DummyWebSocket sent byte array data.");
    }

    @Override
    public void sendFrame(Collection<Framedata> frame) {
        System.out.println("DummyWebSocket sent collection of frame data.");
    }

    @Override
    public void sendPing() {
        System.out.println("DummyWebSocket sent Ping.");
    }

    @Override
    public void sendFragmentedFrame(Opcode opcode, ByteBuffer buffer, boolean fin) {
        System.out.println("DummyWebSocket sent fragmented frame.");
    }

    @Override
    public void close() {
        System.out.println("DummyWebSocket closed.");
    }

    @Override
    public void close(int code, String reason) {
        System.out.println("DummyWebSocket closed with code " + code + " and reason: " + reason);
    }

    @Override
    public void close(int code) {
        System.out.println("DummyWebSocket closed with code " + code);
    }

    @Override
    public void closeConnection(int code, String reason) {
        System.out.println("DummyWebSocket connection closed with code " + code + " and reason: " + reason);
    }

    @Override
    public boolean isOpen() {
        return true; // Simulate an open connection
    }

    @Override
    public boolean isClosing() {
        return false; // Simulate a non-closing connection
    }

    @Override
    public boolean isClosed() {
        return false; // Simulate a non-closed connection
    }

    @Override
    public <T> void setAttachment(T attachment) {
        // No-op
    }

    @Override
    public <T> T getAttachment() {
        return null; // No attachment
    }

    @Override
    public boolean hasBufferedData() {
        return false; // Simulate no buffered data
    }

    @Override
    public String getResourceDescriptor() {
        return "/dummy"; // Dummy resource descriptor
    }

    @Override
    public ReadyState getReadyState() {
        return ReadyState.OPEN; // Simulate an open WebSocket
    }

    @Override
    public SSLSession getSSLSession() {
        return null; // No SSL support in the dummy WebSocket
    }

    @Override
    public boolean hasSSLSupport() {
        return false; // No SSL support in the dummy WebSocket
    }

    @Override
    public boolean isFlushAndClose() {
        return false; // Simulate no flush-and-close behavior
    }

    @Override
    public Draft getDraft() {
        return null; // No Draft in the dummy WebSocket
    }

    @Override
    public void sendFrame(Framedata frame) {
        System.out.println("DummyWebSocket sent frame data.");
    }

    @Override
    public InetSocketAddress getLocalSocketAddress() {
        return new InetSocketAddress("127.0.0.1", 8080); // Use a dummy value for local address
    }

    @Override
    public IProtocol getProtocol() {
        return null; // No IProtocol support in the dummy WebSocket
    }
}