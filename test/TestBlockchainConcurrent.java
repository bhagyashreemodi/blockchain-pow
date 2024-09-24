package test;

import blockchain.Block;
import blockchain.MinerNode;

import java.io.IOException;
import java.util.List;

/**
 * Tests blockchain forking scenarios by simulating forks and observing if nodes correctly resolve them.
 */
public class TestBlockchainConcurrent extends Test {
    private static final int DIFFICULTY = 4;
    private static final int NUM_TRANSACTIONS = 5;

    /**
     * Initializes the test scenario for blockchain forking.
     */
    public TestBlockchainConcurrent() {
        NUM_NODES = 5;
        initializePorts();
        nodes = new MinerNode[NUM_NODES];
        threads = new Thread[NUM_NODES];
    }

    /**
     * Executes the fork simulation test, including setup, transaction broadcasting, fork creation, and verification of blockchain consistency.
     * @throws IOException If an I/O error occurs during the test.
     * @throws InterruptedException If the thread is interrupted while waiting.
     */
    @Override
    public void perform() throws IOException, InterruptedException {
        try {
            simulateBlockchainFork();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            throw e;
        } finally {
            clean();
        }
    }

    /**
     * Simulates a blockchain fork by creating conflicting blocks at the same height and tests if the network can resolve the fork.
     * It involves starting nodes, sending transactions, creating forks, and finally verifying blockchain consistency across all nodes.
     * @throws IOException If an I/O error occurs during simulation.
     * @throws InterruptedException If the thread is interrupted while waiting.
     */
    private void simulateBlockchainFork() throws IOException, InterruptedException {
        startMinerNodes(DIFFICULTY);

        // Allow nodes to initialize
        Thread.sleep(1000);

        // Broadcast transactions to all nodes
        for (int i = 0; i < NUM_TRANSACTIONS; i++) {
            broadcastTransaction(String.valueOf(i + 1));
        }

        Thread.sleep(2000); // Time for transactions to be processed

        // Miner 0: 2 blocks with 6 and 7 transactions, block 7 will be pushed concurrently with other miner
        List<Block> chain_from_0 = fetchChainFromNode(peerAddresses.get(0));
        Block test_block = new Block(chain_from_0.get(chain_from_0.size() - 1).getHash(), System.currentTimeMillis(), List.of(String.valueOf(NUM_TRANSACTIONS + 1)));
        test_block.mineBlock(DIFFICULTY);
        sendBlockToNode(test_block, 0);
        System.out.println(fetchChainFromNode(peerAddresses.get(0)));
        Block concurrent_block_from_0 = new Block(test_block.getHash(), System.currentTimeMillis(), List.of(String.valueOf(NUM_TRANSACTIONS + 2)));
        concurrent_block_from_0.mineBlock(DIFFICULTY);
        sendBlockToNode(concurrent_block_from_0, 0);
        System.out.println(fetchChainFromNode(peerAddresses.get(0)));

        // Miner 0: block with transaction 8, block 8 will be pushed concurrently with other miner
        List<Block> chain_from_1 = fetchChainFromNode(peerAddresses.get(1));
        Block concurrent_block_from_1 = new Block(chain_from_1.get(chain_from_0.size() - 1).getHash(), System.currentTimeMillis(), List.of(String.valueOf(NUM_TRANSACTIONS + 3)));
        concurrent_block_from_1.mineBlock(DIFFICULTY);
        sendBlockToNode(concurrent_block_from_1, 1);
        System.out.println(fetchChainFromNode(peerAddresses.get(1)));

        // Miner 1, node 0: pushing block with transaction 7
        // Miner 2, node 1: pushing block with transaction 8

        Thread miner1 = new Thread(() -> {
            try {
                broadcastBlockToNodes(concurrent_block_from_0, 0);
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        });
        Thread miner2 = new Thread(() -> {
            try {
                broadcastBlockToNodes(concurrent_block_from_1, 1);
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        });
        miner1.start();
        miner2.start();

        Thread.sleep(2000); // Allow more time for the nodes to resolve the fork

        // Verify blockchain consistency across all nodes
        List<Block> referenceChain = fetchChainFromNode(peerAddresses.get(0));
        for (int i = 1; i < NUM_NODES; i++) {
            List<Block> nodeChain = fetchChainFromNode(peerAddresses.get(i));
            if (!nodeChain.equals(referenceChain)) {
                System.out.println("Node " + i + " does not have the same blockchain as the reference node. Fork resolution failed.");
                printBlockchainDiff(referenceChain, nodeChain); // Print the difference between the blockchains
                fail("Fork resolution failed.");
            }
        }
        System.out.println("All nodes have successfully resolved the fork and share the same blockchain.");
    }

    /**
     * Broadcasts a newly mined block to all nodes except the sender to simulate network propagation of blocks.
     * @param block The block to broadcast.
     * @param senderIndex The index of the node that mined the block, which will not receive the block.
     */
    private void broadcastBlockToNodes(Block block, int senderIndex) {
        for (int i = 0; i < NUM_NODES; i++) {
            if (i != senderIndex) {
                sendBlockToNode(block, i);
            }
        }
    }

    /**
     * Displays the differences between the blockchain of a reference node and another node.
     * This method is useful for debugging and understanding how different blockchains diverge during a fork.
     * @param referenceChain The blockchain of the reference node.
     * @param nodeChain The blockchain of the node to compare.
     */
    private void printBlockchainDiff(List<Block> referenceChain, List<Block> nodeChain) {
        System.out.println("Reference Blockchain:");
        for (Block block : referenceChain) {
            System.out.println(block);
        }
        System.out.println("Node Blockchain:");
        for (Block block : nodeChain) {
            System.out.println(block);
        }
    }
}