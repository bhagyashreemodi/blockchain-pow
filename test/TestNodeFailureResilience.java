package test;

import blockchain.Block;
import blockchain.MinerNode;

import java.io.IOException;
import java.util.List;
import java.util.Random;

/**
 * This class tests the resilience of the blockchain network to node failures.
 * It simulates node failures during the mining process and checks if the network
 * can still function correctly and maintain a consistent blockchain across all
 * active nodes.
 *
 * <p>The test initializes a fixed number of miner nodes, broadcasts a set of transactions,
 * and then randomly stops some nodes to simulate failure. After a short period, it resumes
 * the stopped nodes and continues to broadcast transactions to observe if the stopped nodes
 * can catch up with the rest of the network.</p>
 *
 * <p>This test is crucial for ensuring that the blockchain network is robust and can handle
 * node failures without data loss or inconsistency.</p>
 */
public class TestNodeFailureResilience extends Test {
    private final Integer DIFFICULTY = 4;
    private final int NUM_TRANSACTIONS = 10;

    /**
     * Constructs a new TestNodeFailureResilience instance.
     * Initializes miner nodes and their respective network ports.
     */
    public TestNodeFailureResilience() {
        NUM_NODES = 5;
        nodes = new MinerNode[NUM_NODES];
        threads = new Thread[NUM_NODES];
        initializePorts();
    }

    /**
     * Executes the node failure resilience test.
     * This method handles the process of starting nodes, broadcasting transactions,
     * simulating node failures, and checking blockchain consistency.
     *
     * @throws IOException If there is an I/O error during node communication.
     * @throws InterruptedException If the thread is interrupted while sleeping.
     */
    @Override
    public void perform() throws IOException, InterruptedException {
        try {
            startMinerNodes(DIFFICULTY);

            // Broadcast transactions to all nodes
            for (int i = 0; i < NUM_TRANSACTIONS; i++) {
                broadcastTransaction(String.valueOf(i));
            }

            // Wait for multiple blocks to be mined
            Thread.sleep(2000);

            // Randomly stop some nodes to simulate failure
            Random random = new Random();
            int numNodesToStop = random.nextInt(NUM_NODES - 1) + 1;
            int[] stoppedNodes = new int[numNodesToStop];
            for (int i = 0; i < numNodesToStop; i++) {
                int nodeIndex = random.nextInt(NUM_NODES);
                nodes[nodeIndex].stopNode();
                System.out.println("Stopped node " + nodeIndex);
                stoppedNodes[i] = nodeIndex;
            }

            // Broadcast more transactions after node failures
            for (int i = NUM_TRANSACTIONS; i < NUM_TRANSACTIONS * 2; i++) {
                broadcastTransaction(String.valueOf(i));
            }

            // Wait for more blocks to be mined
            Thread.sleep(2000);

            for (int i = 0; i < numNodesToStop; i++) {
                nodes[stoppedNodes[i]].startNode();
                System.out.println("Started stopped nodes " + stoppedNodes[i]);
            }
            broadcastTransaction("20"); // Broadcast one more transaction and see if the stopped nodes can catch up

            Thread.sleep(2000);
            // Fetch the blockchain from each running node and compare
            List<Block> referenceChain = null;
            for (int i = 0; i < NUM_NODES; i++) {
                List<Block> chain = fetchChainFromNode(peerAddresses.get(i));
                System.out.println("Node " + i + ": Received chain " + (chain == null ? "[]" : chain));
                if (referenceChain == null) {
                    validateChainContents(chain, 21);
                    referenceChain = chain;
                } else {
                    if (!referenceChain.equals(chain)) {
                        fail("Node " + i + " has a different chain from the reference chain");
                    }
                }
            }

            System.out.println("Node failure resilience test passed");
        } catch (Exception e) {
            e.printStackTrace(System.err);
            throw e;
        } finally {
            clean();
        }
    }
}