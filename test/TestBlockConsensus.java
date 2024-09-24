package test;

import blockchain.Block;
import blockchain.MinerNode;

import java.io.IOException;
import java.util.List;

/**
 * Tests the consensus mechanism of the blockchain network to ensure that all nodes agree on the same blockchain
 * state after multiple transactions have been broadcast and blocks mined.
 * This class verifies the integrity and consistency of the blockchain across multiple nodes by comparing the chains
 * held by each node after transactions are processed and blocks are mined under a specified difficulty level.
 *
 * <p>The goal of this test is to confirm that all nodes reach consensus on the valid chain despite potential propagation
 * delays and the asynchronous nature of mining across different nodes.</p>
 */
public class TestBlockConsensus extends Test {

    private final Integer DIFFICULTY = 4;
    private final int NUM_TRANSACTIONS = 10;

    /**
     * Constructs a new TestBlockConsensus instance.
     * Initializes miner nodes and their respective network ports to prepare for the test.
     */
    public TestBlockConsensus() {
        NUM_NODES = 5;
        nodes = new MinerNode[NUM_NODES];
        threads = new Thread[NUM_NODES];
        initializePorts();
    }

    /**
     * Executes the block consensus test.
     * This method starts the miner nodes, broadcasts a series of transactions, and then checks
     * the consistency and integrity of the blockchain across all participating nodes.
     *
     * @throws IOException If there is an I/O error during communication with the nodes.
     * @throws InterruptedException If the thread is interrupted during the wait period for block mining.
     */
    @Override
    public void perform() throws IOException, InterruptedException {
        try {
            startMinerNodes(DIFFICULTY);
            Thread.sleep(2000);
            // Broadcast transactions to all nodes
            for (int i = 1; i < NUM_TRANSACTIONS; i++) {
                broadcastTransaction(String.valueOf(i));
            }

            // Wait for multiple blocks to be mined
            Thread.sleep(5000); // This time can be less if you reduce the difficulty level

            // Fetch the blockchain from each node and compare
            List<Block> referenceChain = null;
            for (int i = 0; i < NUM_NODES; i++) {
                List<Block> chain = fetchChainFromNode(peerAddresses.get(i));
                System.out.println("Node " + i + ": Received chain " + (chain == null ? "[]" : chain));

                if (referenceChain == null) {
                    validateChainContents(chain, NUM_TRANSACTIONS);
                    referenceChain = chain;
                } else {
                    if (!referenceChain.equals(chain)) {
                        fail("Node " + i + " has a different chain from the reference chain");
                    }
                }
            }

            System.out.println("Block consensus test passed");
        } catch (Exception e) {
            e.printStackTrace(System.err);
            throw e;
        } finally {
            clean();
        }
    }
}