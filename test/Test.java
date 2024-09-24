package test;

import blockchain.Block;
import blockchain.MinerNode;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public abstract class Test {

    protected int NUM_NODES;
    protected MinerNode[] nodes;
    protected Thread[] threads;
    protected int[] peerPorts;
    protected int[] clientPorts;
    protected List<String> peerAddresses;
    public abstract void perform() throws IOException, InterruptedException;


    public void clean() {
        for (int i = 0; i < NUM_NODES; i++) {
            try {
                nodes[i].stopNode();
                threads[i].interrupt();
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
        for (int i = 0; i < NUM_NODES; i++) {
            try {
                threads[i].join();
                threads[i] = null;
            } catch (InterruptedException ignored) {
            }
        }
        threads = null;
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {}
    }

    protected List<Block> fetchChainFromNode(String peerAddress) {
        String[] parts = peerAddress.split(":");
        try (Socket socket = new Socket(parts[0], Integer.parseInt(parts[1]));
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            out.writeObject("REQUEST_BLOCKCHAIN");
            out.flush();

            Object object = in.readObject();
            if (object instanceof List) {
                List<Block> peerChain = (List<Block>) object;
                return peerChain;
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch blockchain from " + peerAddress + ": " + e.getMessage());
            e.printStackTrace(System.err);
            throw new IllegalStateException("Failed to fetch blockchain from " + peerAddress);
        }
        return null;
    }

    protected void broadcastTransaction(String transaction) {
        for (int i = 0; i < NUM_NODES; i++) {
            try (Socket socket = new Socket("127.0.0.1", clientPorts[i]);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
                out.writeObject(transaction);
                out.flush();
                String object = (String) in.readObject();
                System.out.println("Response from node " + i + ": " + object);
            } catch (Exception e) {
                //System.err.println("Failed to broadcast transaction to node " + i + ": " + e.getMessage());
                //e.printStackTrace(System.err);
            }
        }
    }

    protected List<String> createPeerAddresses() {
        String[] addresses = new String[NUM_NODES];
        for (int i = 0; i < NUM_NODES; i++) {
            addresses[i] = "127.0.0.1:" + peerPorts[i];
        }
        return Arrays.asList(addresses);
    }

    protected void initializePorts() {
        Random rng = new Random(System.nanoTime());
        clientPorts = new int[NUM_NODES];
        for (int i = 0; i < NUM_NODES; i++) {
            clientPorts[i] = rng.nextInt(10000) + 6000;
        }
        peerPorts = new int[NUM_NODES];
        for (int i = 0; i < NUM_NODES; i++) {
            peerPorts[i] = rng.nextInt(10000) + 5000;
        }
        peerAddresses = createPeerAddresses();
    }

    protected void startMinerNodes(int DIFFICULTY) throws IOException {
        for (int i = 0; i < NUM_NODES; i++) {
            nodes[i] = new MinerNode(clientPorts[i], peerPorts[i], peerAddresses, i, DIFFICULTY);
            int finalI = i;
            threads[i] = new Thread(() -> {
                nodes[finalI].startNode();
            });
            threads[i].start();
        }
    }

    public void fail(String reason) {
        throw new IllegalStateException(reason);
    }

    protected void validateChainContents(List<Block> referenceChain, int expectedChainSize) {
        if(referenceChain == null)
            fail("Blockchain is null");
        if(referenceChain.size() < expectedChainSize)
            fail("Blocks are missing");
        for(int i = 0; i < expectedChainSize; i++) {
            if(!referenceChain.get(i).getTransactions().get(0).equals(String.valueOf(i)))
                fail("Block " + i + " has invalid transaction");
        }
    }

    protected void sendTransactionToNode(String transaction, int nodeIndex) {
        try (Socket socket = new Socket("127.0.0.1", clientPorts[nodeIndex]);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            out.writeObject(transaction);
            out.flush();
            String response = (String) in.readObject();
            System.out.println("Response from node " + nodeIndex + ": " + response);
        } catch (Exception e) {
            System.err.println("Failed to send transaction to node " + nodeIndex + ": " + e.getMessage());
        }
    }

    /**
     * Sends a block to a specified node.
     * @param block The block to be sent.
     * @param nodeIndex The index of the node in the nodes array to send the block to.
     */
    protected void sendBlockToNode(Block block, int nodeIndex) {
        try (Socket socket = new Socket("127.0.0.1", peerPorts[nodeIndex]);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            out.writeObject(block);
            out.flush();
            // Optionally read a response from the node
            // String response = (String) in.readObject();
            // System.out.println("Response from node " + nodeIndex + ": " + response);
        } catch (Exception e) {
            System.err.println("Failed to send block to node " + nodeIndex + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
