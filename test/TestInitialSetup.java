package test;

import blockchain.Block;
import blockchain.MinerNode;

import java.io.IOException;
import java.util.List;

/**
 * Tests the initial setup of the blockchain network to ensure that all nodes correctly mine the genesis block.
 * This class verifies that each node in the network starts with a correct blockchain initialization,
 * which includes mining and validating the genesis block upon startup.
 *
 * <p>It configures a small network of nodes and starts them with a specified mining difficulty. After initialization,
 * it checks each node to ensure that the genesis block is present and correctly formed, indicating a successful
 * startup and synchronization across the network.</p>
 */
public class TestInitialSetup extends Test {

    private final Integer DIFFICULTY = 4;

    /**
     * Constructs a new TestInitialSetup instance.
     * Initializes miner nodes and their respective network ports.
     */
    public TestInitialSetup() {
        NUM_NODES = 3;
        nodes = new MinerNode[NUM_NODES];
        threads = new Thread[NUM_NODES];
        initializePorts();
    }

    /**
     * Executes the initial setup test.
     * Starts the miner nodes and verifies that each node has mined and possesses only the genesis block.
     *
     * @throws IOException If there is an I/O error during node communication.
     * @throws InterruptedException If the thread is interrupted during its execution.
     */
    @Override
    public void perform() throws IOException, InterruptedException {
        try {
            startMinerNodes(DIFFICULTY);
            Thread.sleep(2000);
            for (int i = 0; i < NUM_NODES; i++) {
                List<Block> chain = fetchChainFromNode(peerAddresses.get(i));
                if (chain == null || chain.size() != 1) {
                    fail("Node " + i + " did not mine the genesis block");
                }
            }
            System.out.println("Initial setup passed");
        } catch (Exception e) {
            System.err.println(e.getMessage());
            throw e;
        } finally {
            clean();
        }
    }
}
