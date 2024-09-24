package test;

import blockchain.Block;
import blockchain.MinerNode;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests the mining of the first block following the genesis block across multiple nodes in a blockchain network.
 * This class is designed to verify that all nodes can successfully mine the first block with the same transactions
 * and maintain consistency in their blockchains after the genesis block.
 *
 * <p>The test ensures that after the initial transaction is broadcast, all nodes work under the same mining difficulty
 * and can synchronize their blockchains by adding a correctly mined block that is consistent across the network.</p>
 */
public class TestFirstBlockMining extends Test {

    private final Integer DIFFICULTY = 5;

    /**
     * Constructs a new TestFirstBlockMining instance.
     * Initializes miner nodes and their respective network ports.
     */
    public TestFirstBlockMining() {
        NUM_NODES = 3;
        nodes = new MinerNode[NUM_NODES];
        threads = new Thread[NUM_NODES];
        initializePorts();
    }

    /**
     * Executes the test for mining the first block.
     * Starts the miner nodes, broadcasts a transaction, and checks the consistency of the first mined block across all nodes.
     *
     * @throws IOException If there is an I/O error during node communication.
     * @throws InterruptedException If the thread is interrupted during its execution.
     */
    @Override
    public void perform() throws IOException, InterruptedException {
        try {
            testFirstBlockMining();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            throw e;
        } finally {
            clean();
        }
    }

    /**
     * Tests the mining of the first block by broadcasting a transaction and then validating the mined block across all nodes.
     * Verifies that all nodes have extended their blockchain with a new block that matches in terms of transactions and structure.
     *
     * @throws IOException If there is an I/O issue with node communication.
     * @throws InterruptedException If there is an interruption during the wait period for block mining completion.
     */
    private void testFirstBlockMining() throws IOException, InterruptedException {
        startMinerNodes(DIFFICULTY);
        broadcastTransaction("1");
        Thread.sleep(2000); // Wait for at least 1 node mining to finish
        List<Block> referenceChain = null;
        for (int i = 0; i < NUM_NODES; i++) {
            List<Block> chain = fetchChainFromNode(peerAddresses.get(i));
            System.out.println("Node " + i + ": Received chain " + (chain == null ? "[]" : chain));

            if (referenceChain == null) {
                validateChainContents(chain, 2);
                referenceChain = chain;
            } else {
                if (!referenceChain.equals(chain)) {
                    fail("Node " + i + " has a different chain from the reference chain");
                }
            }
        }
    }
}
