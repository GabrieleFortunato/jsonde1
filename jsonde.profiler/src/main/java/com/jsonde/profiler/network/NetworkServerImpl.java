package com.jsonde.profiler.network;

import com.jsonde.api.Message;
import com.jsonde.api.MessageListener;
import com.jsonde.profiler.DaemonThreadFactory;
import com.jsonde.util.log.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Commenti Javadoc
 * @author gabriele
 *
 */
public class NetworkServerImpl implements NetworkServer {

    private static final Log log = Log.getLog(NetworkServerImpl.class);

    private int port;

    private boolean running;
    private boolean outputWorkerReady;
    private boolean inputWorkerReady;

    //private BlockingQueue<Message> messageQueue = new ArrayBlockingQueue<Message>(50);
    private BlockingQueue<Message> messageQueue = new ArrayBlockingQueue<Message>(5000);

    private ServerSocket serverSocket;
    private Socket socket;

    private Thread outputThread;
    private Thread inputThread;

    private Collection<MessageListener> messageListeners = new Vector<MessageListener>();

    private DaemonThreadFactory daemonThreadFactory;

    public NetworkServerImpl(int port, DaemonThreadFactory daemonThreadFactory) {
        this.port = port;
        this.daemonThreadFactory = daemonThreadFactory;
    }

    public void start() throws NetworkServerException {

        try {
            serverSocket = new ServerSocket(port);

            String testProperty = System.getProperty("com.jsonde.test");

            if (null != testProperty && Boolean.parseBoolean(testProperty)) {
                System.out.write(0);
                System.out.flush();
            }

            socket = serverSocket.accept();
        } catch (IOException e) {
            throw new NetworkServerException(e);
        }

        setRunning(true);

        inputThread = daemonThreadFactory.newThread(new ServerInputWorker(this, socket));
        inputThread.start();

        outputThread = daemonThreadFactory.newThread(new ServerOutputWorker(this, socket));
        outputThread.start();

        waitForInitialization();

    }

    public void stop() throws NetworkServerException {

        final String METHOD_NAME = "stop()";

        setRunning(false);

        if (null != outputThread) {
            try {
                outputThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error(METHOD_NAME, e);
            }
        }

        //

        if (null != inputThread) {
            try {
                inputThread.join(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error(METHOD_NAME, e);
            }

            if (inputThread.isAlive()) {

                closeSockets();

                try {
                    inputThread.join(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error(METHOD_NAME, e);
                }

                if (inputThread.isAlive()) {
                    inputThread.interrupt();
                }

            } else {
                closeSockets();
            }
        } else {
            closeSockets();
        }

    }

    private void closeSockets() throws NetworkServerException {
    	synchronized (this) {
    		try {
                if (null != socket)
                    socket.close();
                if (null != serverSocket)
                    serverSocket.close();
            } catch (IOException e) {
                throw new NetworkServerException(e);
            }
            notifyAll();
    	}
    }

    private void waitForInitialization() {

    	synchronized (this) {
        final String METHOD_NAME = "waitForInitialization()";

        boolean flag = areWorkersReady();
        
        try {
        	while (!flag) {
        		wait();
            flag = areWorkersReady();
        	} 
        } catch (InterruptedException e) {
            log.error(METHOD_NAME, e);
            Thread.currentThread().interrupt();
        }
        notifyAll();
    	}
    }

    public void sendMessage(Message message) {
    	synchronized (this){
            final String METHOD_NAME = "sendMessage(Message)";

            try {
                boolean inserted = false;
                while (!inserted) {
                    inserted = messageQueue.offer(message);
                    if (inserted) {
                        notifyAll();
                    } else {
                        wait();
                    }
                }
            } catch (InterruptedException e) {
                log.error(METHOD_NAME, e);
                Thread.currentThread().interrupt();
            }

    	}
    }


    protected boolean isMessageInQueue() throws InterruptedException {
    	synchronized (this){
    		boolean flag = isRunning();
        	int a = messageQueue.size();
            while (flag && 0 == a) {
                wait();
                flag = isRunning();
                a = messageQueue.size();
            }
            return 0 != messageQueue.size();
    	}
    }

    protected Message takeMessageFromQueue() {
        synchronized (this){
        	Message message = messageQueue.poll();
            notifyAll();
            return message;
        }
    }

    protected void processMessage(Message message) {
        for (MessageListener messageListener : messageListeners) {
            messageListener.onMessage(message);
        }
    }


    public void addMessageListener(MessageListener messageListener) {
        messageListeners.add(messageListener);
    }

    public void removeMessageListener(MessageListener messageListener) {
        messageListeners.remove(messageListener);
    }


    public boolean isRunning() {
    	synchronized (this){
    		return running;
    	}
    }

    public void setRunning(boolean running) {
    	synchronized (this){
    		this.running = running;
            notifyAll();
    	}
    }

    private boolean areWorkersReady() {
    	synchronized (this){
    		return inputWorkerReady && outputWorkerReady;
    	}
    }

    public void setOutputWorkerReady(boolean outputWorkerReady) {
    	synchronized (this){
    		this.outputWorkerReady = outputWorkerReady;
            notifyAll();
    	}
    }

    public void setInputWorkerReady(boolean inputWorkerReady) {
    	synchronized (this){
    		this.inputWorkerReady = inputWorkerReady;
            notifyAll();
    	}
    }

}