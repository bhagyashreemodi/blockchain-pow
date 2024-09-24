package test;

import blockchain.Block;
import blockchain.MinerNode;

import java.io.IOException;
import java.util.List;

/**
 * Test class to simulate outdated information rejection in a blockchain scenario.
 * This class extends a base Test class and focuses on testing how nodes handle
 * synchronization when some nodes initially accept incorrect transactions.
 */
public class TestOutdatedInformationRejection extends Test {
    private static final int DIFFICULTY = 4;

    /**
     * Constructor initializes nodes and their communication settings.
     */
    public TestOutdatedInformationRejection() {
        NUM_NODES = 5; // Five nodes, where the last two initially accept incorrect transactions
        initializePorts(); // Initializes ports and peerAddresses for communication
        nodes = new MinerNode[NUM_NODES];
        threads = new Thread[NUM_NODES];
    }

    @Override
    public void perform() throws IOException, InterruptedException {
        try {
            simulateOutdatedForkAndRejection();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            throw e;
        } finally {
            clean(); // Clean up resources
        }
    }

    /**
     * Simulates a scenario where nodes initially accept different sets of transactions,
     * and must later synchronize to maintain a consistent blockchain across the network.
     */
    private void simulateOutdatedForkAndRejection() throws IOException, InterruptedException {
        // Initialize and start nodes
        for (int i = 0; i < NUM_NODES; i++) {
            nodes[i] = new MinerNode(clientPorts[i], peerPorts[i], peerAddresses, i, DIFFICULTY);
            int finalI = i;
            threads[i] = new Thread(() -> nodes[finalI].startNode());
            threads[i].start();
        }

        // Allow nodes to initialize
        Thread.sleep(2000);

        // Broadcast correct transactions to the first three nodes and incorrect to the last two
        for (int i = 0; i < 3; i++) {
            sendTransactionToNode(String.valueOf(i + 1), i);
        }

        // Incorrect transactions to the last two nodes
        for (int i = 3; i < NUM_NODES; i++) {
            for (int transId = 20; transId < 22; transId++) {
                List<Block> chain = fetchChainFromNode(peerAddresses.get(i));
                Block block = new Block(chain.get(chain.size() - 1).getHash(), 0, List.of(String.valueOf(transId + 1)));
                block.mineBlock(5);
                sendBlockToNode(block, i);
            }
        }

        Thread.sleep(2000); // Time for transactions to be processed

        // Broadcast a new transaction that should trigger all nodes to synchronize
        for (int i = 0; i < NUM_NODES; i++) {
            sendTransactionToNode("4", i);
        }

        Thread.sleep(8000); // Allow time for the nodes to process the new transaction and synchronize

        // Verify blockchain consistency across all nodes
        List<Block> referenceChain = fetchChainFromNode(peerAddresses.get(0));
        for (int i = 1; i < NUM_NODES; i++) {
            List<Block> nodeChain = fetchChainFromNode(peerAddresses.get(i));
            if (!nodeChain.equals(referenceChain)) {
                System.out.println("Node " + i + " does not have the same blockchain as the reference node. Resynchronization failed.");
                for(Block block : nodeChain) {
                    System.out.println("node chain:" + block);
                }
                fail("Resynchronization failed.");
            }
        }
        System.out.println("All nodes have successfully synchronized and share the same blockchain.");
    }
}
