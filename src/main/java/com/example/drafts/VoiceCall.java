package com.example.drafts;

import javax.sound.sampled.*;
import java.net.*;

public class VoiceCall {
    private static final AudioFormat FORMAT = new AudioFormat(16000, 16, 1, true, false);
    private static final int PACKET_MS   = 20;
    private static final int PACKET_SIZE =
            (int)(FORMAT.getSampleRate() * FORMAT.getFrameSize() * PACKET_MS / 1000);

    private final InetAddress remoteAddr;
    private final int         remotePort;
    private DatagramSocket    udpSocket;
    private volatile boolean  running;
    private volatile boolean  muted;

    private Thread captureThread;
    private Thread playbackThread;

    public VoiceCall(InetAddress remoteAddr, int remotePort) {
        this.remoteAddr = remoteAddr;
        this.remotePort = remotePort;
    }

    public VoiceCall(int localPort, InetAddress remoteAddr, int remotePort) throws SocketException {
        this.remoteAddr = remoteAddr;
        this.remotePort = remotePort;
        this.udpSocket  = new DatagramSocket(localPort);
    }

    public int start() throws Exception {
        if (udpSocket == null) udpSocket = new DatagramSocket();
        udpSocket.setSoTimeout(0);
        running = true;

        captureThread  = new Thread(this::captureAndSend, "voice-capture");
        playbackThread = new Thread(this::receiveAndPlay, "voice-playback");
        captureThread.setDaemon(true);
        playbackThread.setDaemon(true);
        captureThread.start();
        playbackThread.start();

        return udpSocket.getLocalPort();
    }

    public void stop() {
        running = false;
        if (udpSocket != null && !udpSocket.isClosed()) udpSocket.close();
        if (captureThread  != null) captureThread.interrupt();
        if (playbackThread != null) playbackThread.interrupt();
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public boolean isMuted() { return muted; }

    private void captureAndSend() {
        try {
            DataLine.Info  info = new DataLine.Info(TargetDataLine.class, FORMAT);
            TargetDataLine mic  = (TargetDataLine) AudioSystem.getLine(info);
            mic.open(FORMAT);
            mic.start();

            byte[] buf     = new byte[PACKET_SIZE];
            byte[] silence = new byte[PACKET_SIZE]; // all zeros

            while (running) {
                int read = mic.read(buf, 0, buf.length);
                if (read > 0) {
                    byte[] payload = muted ? silence : buf;
                    DatagramPacket pkt = new DatagramPacket(payload, read, remoteAddr, remotePort);
                    udpSocket.send(pkt);
                }
            }
            mic.stop();
            mic.close();
        } catch (Exception e) {
            if (running) e.printStackTrace();
        }
    }

    private void receiveAndPlay() {
        try {
            DataLine.Info  info     = new DataLine.Info(SourceDataLine.class, FORMAT);
            SourceDataLine speakers = (SourceDataLine) AudioSystem.getLine(info);
            speakers.open(FORMAT);
            speakers.start();

            byte[]         buf = new byte[PACKET_SIZE];
            DatagramPacket pkt = new DatagramPacket(buf, buf.length);

            while (running) {
                udpSocket.receive(pkt);
                speakers.write(pkt.getData(), 0, pkt.getLength());
            }
            speakers.drain();
            speakers.close();
        } catch (Exception e) {
            if (running) e.printStackTrace();
        }
    }
}