package blockchain;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.Collections.emptyList;

/**
 * Represents a node in a blockchain network that mines blocks and handles communications with clients and peers.
 */
public class MinerNode {
    /**
     * The blockchain instance maintained by this node.
     */
    private final BlockChain blockchain;
    /**
     * Executor service for managing concurrent tasks.
     */
    private ExecutorService executorService;

    /**
     * Pool of transactions waiting to be processed.
     */
    private final Queue<String> transactionPool;

    /**
     * List of addresses for peer nodes.
     */
    private final List<String> peerAddresses;
    /**
     * Index of this node within the network.
     */
    private final int myIndex;
    /**
     * Server socket for handling incoming client connections.
     */
    private ServerSocket clientServerSocket;
    /**
     * Server socket for handling incoming connections from peer nodes.
     */
    private ServerSocket peerServerSocket;
    /**
     * Thread responsible for the mining process.
     */
    private MiningThread miningThread;
    /**
     * Flag indicating whether mining is active.
     */
    private final AtomicBoolean miningActive = new AtomicBoolean(false);
    /**
     * Thread handling client communication.
     */
    private Thread clientCommunicationThread;
    /**
     * Thread handling communication with peer nodes.
     */
    private Thread peerNodesCommunicationThread;
    /**
     * Port number for client communication.
     */
    private final int clientPort;
    /**
     * Port number for peer node communication.
     */
    private final int peerNodePort;

    /**
     * Lock to ensure thread safety in block mining.
     */
    private final ReentrantLock lock = new ReentrantLock();


    /**
     * Constructs a MinerNode with specified ports, peer addresses, index, and mining difficulty.
     * Initializes blockchain and sets up server sockets for client and peer communications.
     *
     * @param clientPort     Port number for client communications.
     * @param peerNodePort   Port number for communications with other nodes.
     * @param peerAddresses  List of addresses of peer nodes.
     * @param myIndex        Index of this node in the list of peers.
     * @param difficulty     Mining difficulty setting for the blockchain.
     * @throws IOException   If an I/O error occurs when opening the server socket.
     */
    public MinerNode(int clientPort, int peerNodePort, List<String> peerAddresses, int myIndex, int difficulty) throws IOException {
        this.transactionPool = new ConcurrentLinkedQueue<>();
        this.peerAddresses = peerAddresses;
        this.myIndex = myIndex;
        this.clientPort = clientPort;
        this.peerNodePort = peerNodePort;
        this.blockchain = new BlockChain(difficulty);
    }

    /**
     * Starts the miner node by opening server sockets for client and peer communications.
     * Creates threads to handle incoming client and peer messages, and to manage block mining.
     */
    public void startNode() {
        System.out.println("Starting socket on port for client : " + clientPort);
        try {
            this.executorService = Executors.newCachedThreadPool();
            this.clientServerSocket = new ServerSocket(clientPort);
            this.peerServerSocket = new ServerSocket(peerNodePort);
            clientCommunicationThread = new Thread(this::listenForIncomingClientConnections);
            clientCommunicationThread.start();
            System.out.println("Starting socket on port for peer : " + peerNodePort);
            peerNodesCommunicationThread = new Thread(this::listenForIncomingPeerMessages);
            peerNodesCommunicationThread.start();
            executorService.submit(this::handleBlockChainCreation);
        } catch (IOException e) {
            System.err.println("Error starting node: " + e.getMessage());
        }
    }

    /**
     * Stops the miner node by closing server sockets and interrupting communication threads.
     */
    public void stopNode() {
        try {
            clientServerSocket.close();
            peerServerSocket.close();
            clientCommunicationThread.interrupt();
            peerNodesCommunicationThread.interrupt();
            if (miningThread != null)
                miningThread.interrupt();
            executorService.shutdownNow();
            clientCommunicationThread.join();
            peerNodesCommunicationThread.join();
            clientCommunicationThread = null;
            peerNodesCommunicationThread = null;
            System.out.println("Node stopped.");
        } catch (Exception e) {
            System.err.println("Error stopping : " + e.getMessage());
        }
    }

    /**
     * Broadcasts a transaction to all peer nodes in the network.
     *
     */
    private void listenForIncomingPeerMessages() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Socket peerSocket = peerServerSocket.accept();
                this.executorService.submit(() -> handleIncomingPeerMessages(peerSocket));
            }
        } catch (IOException e) {
            System.out.println("Error listening on port " + peerNodePort + ": " + e.getMessage());
        }
    }

    /**
     * Handles incoming messages from a peer node.
     * Processes received blocks and responds to requests for the blockchain.
     *
     * @param peerSocket The socket for communication with the peer node.
     */
    private void handleIncomingPeerMessages(Socket peerSocket) {
        try (ObjectInputStream objectInputStream = new ObjectInputStream(peerSocket.getInputStream());
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(peerSocket.getOutputStream())) {

            Object object = objectInputStream.readObject();
            if (object instanceof Block) {
                processReceivedBlock((Block) object);
            } else if ("REQUEST_BLOCKCHAIN".equals(object)) {
                if (blockchain != null && blockchain.getChain() != null) {
                    List<Block> missingChain = blockchain.getChain();
                    objectOutputStream.writeObject(missingChain);
                    objectOutputStream.flush();
                } else {
                    objectOutputStream.writeObject(emptyList());
                    objectOutputStream.flush();
                }
            }
        } catch (Exception e) {
            System.out.println("Error handling peer message: " + e.getMessage());
            e.printStackTrace(System.err);
        } finally {
            try {
                peerSocket.close();
            } catch (IOException e) {
                System.out.println("Error closing peer socket: " + e.getMessage());
            }
        }
    }

    /**
     * Listens for incoming client connections and handles client transactions.
     */
    private void listenForIncomingClientConnections() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Socket clientSocket = clientServerSocket.accept();
                this.executorService.submit(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            //System.out.println("Error listening on port " + clientPort + ": " + e.getMessage());
        }
    }

    /**
     * Handles a client connection by reading a transaction from the client and adding it to the transaction pool.
     *
     * @param clientSocket The socket for communication with the client.
     */
    // "1", "2", "3", "4", "5"
    private void handleClient(Socket clientSocket) {
        try (ObjectInputStream objectInputStream = new ObjectInputStream(clientSocket.getInputStream());
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(clientSocket.getOutputStream())) {
            String transaction = (String) objectInputStream.readObject();
            System.out.println("Received transaction: " + transaction);
            if(isValidTransaction(transaction)) {
                transactionPool.add(transaction);
                System.out.println("Transaction added to pool: " + transaction);
            } else {
                objectOutputStream.writeObject("Invalid transaction.");
                objectOutputStream.flush();
            }
            objectOutputStream.writeObject("Transaction received and added to the pool.");
            objectOutputStream.flush();

        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }

    /**
     * Checks if a transaction is valid by verifying that it is not null or empty and has not been added to the blockchain.
     *
     * @param transaction The transaction to validate.
     * @return True if the transaction is valid, false otherwise.
     */
    private boolean isValidTransaction(String transaction) {
        System.out.println("Checking transaction validity: " + transaction);
        if(transaction == null || transaction.isEmpty()) {
            return false;
        }
        if(blockchain == null || blockchain.isEmpty()) {
            return true;
        }
        return !blockchain.containsTransaction(transaction);
    }

    /**
     * Processes a received block by validating it and adding it to the blockchain if it is valid.
     * If the block is not valid, the node attempts to synchronize its chain with the peer that sent the block.
     *
     * @param block The block received from a peer node.
     */
    private void processReceivedBlock(Block block) {
        try {
            lock.lock();
            System.out.println("Received block: " + block);
            System.out.println("Current blockchain: " + blockchain);
            Block lastBlock = blockchain.getLastBlock();
            if (blockchain.isValidNewBlock(block, lastBlock)) {
                if(Objects.equals(block.getPreviousHash(), lastBlock.getHash())) {
                    System.out.println("Adding block to chain: " + block);
                    blockchain.addBlock(block);
                    updateTransactionPool();
                    if(miningThread != null) {
                        miningThread.interrupt(block);
                    }
                } else {
                    synchronizeChain(block);
                }
            }
        } catch (Exception e) {
            System.err.println("Error processing received block: " + e.getMessage());
            e.printStackTrace(System.err);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Checks if a chain is valid by verifying that each block links to the previous block and has a valid hash.
     *
     * @param chain The chain to validate.
     * @return True if the chain is valid, false otherwise.
     */
    public boolean isValidChain(List<Block> chain) {
        Block previousBlock = null;
        for (Block block : chain) {
            // Check if the block is valid
            if (previousBlock != null && !block.getPreviousHash().equals(previousBlock.getHash())) {
                return false;  // The block doesn't link to the previous block
            }
            if (!block.getHash().equals(block.calculateHash())) {
                return false;  // The block's hash isn't valid
            }
            previousBlock = block;
        }
        return true;
    }

    /**
     * Fetches the blockchain from a peer node by sending a request and receiving the chain in response.
     *
     * @param peerAddress The address of the peer node to fetch the blockchain from.
     * @return The blockchain received from the peer node.
     */
    private List<Block> fetchChainFromPeer(String peerAddress) {
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
        }
        return null;
    }

    /**
     * Synchronizes the node's chain with a peer node's chain by comparing the lengths and work done on each chain.
     * If the peer's chain is longer and has more work done, the node replaces its chain with the peer's chain.
     *
     * @param block The block received from the peer node.
     */
    private void synchronizeChain(Block block) {
        System.out.println("Synchronizing chain for block: " + block);
        List<Block> longestChain = null;
        String longestChainAddress = null;
        long maxWorkDone = 0;

        // Iterate over all peers
        //peerNodesCommunicationThread.interrupt();
        for (int i = 0; i < peerAddresses.size(); i++) {
            if (i != myIndex) {
                // Fetch the missing part of the chain from the peer
                String address = peerAddresses.get(i);
                List<Block> peerChain = fetchChainFromPeer(address);
                if (peerChain != null && !peerChain.isEmpty() && isValidChain(peerChain)) {
                    // Calculate the total work done on the peer's chain
                    long workDone = peerChain.stream().mapToLong(Block::getNonce).sum();
                    System.out.println("Peer chain work done: " + workDone);
                    System.out.println("Peer chain length: " + peerChain.size());
                    // If the peer's chain is longer and has more work done, update the longestChain and maxWorkDone
                    if ((longestChain == null || peerChain.size() > longestChain.size()) && workDone > maxWorkDone) {
                        longestChain = peerChain;
                        longestChainAddress = address;
                        maxWorkDone = workDone;
                    }
                }
            }
        }

        // If a longest chain was found, replace the current chain with it
        if (longestChain != null) {
            System.out.println("Chain replaced due to longer peer chain from " + longestChainAddress);
            //blockchainLock.lock();
            try {
                blockchain.replaceChain(longestChain);
                if(blockchain.containsTransactions(block.getTransactions())) {
                    System.out.println("Transactions already in fetched chain");
                }
                else if(Objects.equals(block.getPreviousHash(), blockchain.getLastBlock().getHash())) {
                    blockchain.addBlock(block);
                    if (miningThread != null) {
                        miningThread.interrupt(block);
                    }
                }
                //blockchainLock.unlock();
                updateTransactionPool();
            } finally {
                //blockchainLock.unlock();
            }
        }
    }

    /**
     * Broadcasts a new block to all peer nodes in the network.
     *
     * @param block The block to broadcast.
     */
    private void broadcastNewBlock(Block block) {
        System.out.println("Node " +myIndex + " Broadcasting new block :" + block + " to all peers");
        for (int i = 0; i < peerAddresses.size(); i++) {
            if (i != myIndex) {
                String[] parts = peerAddresses.get(i).split(":");
                try (Socket socket = new Socket(parts[0], Integer.parseInt(parts[1]));
                     ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
                    out.writeObject(block);
                    out.flush();
                } catch (IOException e) {
                    System.err.println("Failed to send block to " + parts[1] + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * Handles the creation of new blocks by mining transactions in the transaction pool.
     */
    private void handleBlockChainCreation() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                //
                if (!transactionPool.isEmpty() && !miningActive.get()) {
                    System.out.println("Current transactions on node : " + transactionPool.size() + " : " + myIndex);
                    System.out.println("Transaction pool not empty, mining a new block...");
                    Block latestBlock = blockchain.getLastBlock();
                    String transaction = transactionPool.peek();
                    if(transaction != null) {
                        Block newBlock = new Block(latestBlock.getHash(), System.currentTimeMillis(), List.of(transactionPool.peek()));
                        //
                        miningThread = new MiningThread(newBlock, latestBlock);
                        miningThread.start();
                        miningActive.set(true);
                    }
                    try {
                        Thread.sleep(100);  // Sleep to reduce CPU usage
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                //
            }
        } catch (Exception e) {
            System.err.println("Error handling block chain creation: " + e.getMessage());
            e.printStackTrace(System.err);
        } finally {
           //
        }
    }

    /**
     * Updates the transaction pool by removing transactions that are included in blocks in the blockchain.
     */
    public void updateTransactionPool() {
        // Iterate over the blocks in the blockchain
       //
        try {
            for (Block block : blockchain.getChain()) {
                // Get the list of transactions in the current block
                List<String> blockTransactions = block.getTransactions();
                // Remove all block transactions from the pool
                transactionPool.removeAll(blockTransactions);
                System.out.println("Transactions removed from pool: " + blockTransactions);
                System.out.println("Current transaction pool size: " + transactionPool.size());
            }
        } finally {
          //
        }

    }

    /**
     * Returns the blockchain maintained by this node.
     *
     */
    private class MiningThread extends Thread {
        /**
         * Represents a newly formed block pending to be added to the blockchain.
         */
        private Block newBlock;
        /**
         * Represents the most recently mined block.
         */
        private Block latestBlock;

        /**
         * Constructs a MiningThread to mine a block based on a given new block and the latest block in the chain.
         *
         * @param newBlock The new block to mine.
         * @param latestBlock The latest block in the blockchain for reference.
         */
        public MiningThread(Block newBlock, Block latestBlock) {
            this.newBlock = newBlock;
            this.latestBlock = latestBlock;
        }

        /**
         * The main running method of the thread that performs the block mining.
         */
        @Override
        public void run() {
            try {
                newBlock.mineBlock(blockchain.getDifficulty());
                lock.lock();
                if (blockchain.isValidNewBlock(newBlock, latestBlock)) {
                    blockchain.addBlock(newBlock);
                    broadcastNewBlock(newBlock);
                    updateTransactionPool();
                }
            } catch (Exception e) {
                System.err.println("Mining interrupted: " + e.getMessage());
            } finally {
                System.out.println("Mining completed");
                miningActive.set(false);
                lock.unlock();
            }
        }

        /**
         * Interrupts the mining process if a new block that contains the same transactions is received.
         *
         * @param latestBlock The latest block to compare with the current mining block.
         */
        public void interrupt(Block latestBlock) {
            if(newBlock.getTransactions().equals(latestBlock.getTransactions())) {
                System.out.println("Mining interrupted as already received from other");
                super.interrupt();
                miningActive.set(false);
            }
        }
    }
}
