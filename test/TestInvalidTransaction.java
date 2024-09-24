package test;

import blockchain.MinerNode;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * This class tests the blockchain network's ability to correctly identify and reject invalid transactions.
 * It ensures that invalid transactions are not added to the transaction pool across multiple nodes.
 *
 * <p>The class sets up a small network of nodes and attempts to send what is defined as an invalid transaction
 * to each node. It checks if each node correctly rejects the transaction by not adding it to their respective
 * transaction pool and providing the appropriate response.</p>
 */
public class TestInvalidTransaction extends Test {

    /**
     * Constructs a new TestInvalidTransaction instance.
     * Initializes miner nodes and their respective network ports.
     */
    public TestInvalidTransaction() {
        NUM_NODES = 3; // Adjust as needed based on test setup
        nodes = new MinerNode[NUM_NODES];
        threads = new Thread[NUM_NODES];
        initializePorts();
    }

    /**
     * Executes the test for handling invalid transactions.
     * Duplication of transactions is treated as invalid, and the test verifies that nodes reject such transactions.
     * The method encapsulates the testing logic to verify if invalid transactions
     * are rejected by all nodes in the network.
     *
     * @throws IOException If there is an I/O error during communication with nodes.
     * @throws InterruptedException If the thread is interrupted during its execution.
     */
    @Override
    public void perform() throws IOException, InterruptedException {
        try {
            startMinerNodes(4); // Set the mining difficulty
            for (int i = 0; i < 5; i++) {
                broadcastTransaction(String.valueOf(i+1));
            }
            testInvalidTransactionNotAddedToPool();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            throw e;
        } finally {
            clean();
        }
    }

    /**
     * Tests each node to ensure that an invalid transaction is not added to the transaction pool.
     * The method communicates with each node in the network, sending a predefined invalid transaction
     * and expecting a specific rejection response.
     */
    private void testInvalidTransactionNotAddedToPool() {
        for (int i = 0; i < NUM_NODES; i++) {
            try (Socket socket = new Socket("127.0.0.1", clientPorts[i]);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
                // Duplicate is treated as invalid, Send an invalid transaction to each node
                out.writeObject("3");
                out.flush();
                String response = (String) in.readObject();
                if (!response.equals("Invalid transaction.")) {
                    fail("Node " + i + " did not reject the invalid transaction");
                }
            } catch (Exception e) {
                System.err.println("Failed to broadcast transaction to node " + i + ": " + e.getMessage());
                e.printStackTrace(System.err);
            }
        }
    }
}
