package blockchain;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.emptyList;

/**
 * Constructor to create a new blockchain with a specified difficulty level.
 *
 */
public class BlockChain implements Serializable {
    /**
     * List representing the chain of blocks.
     */
    private LinkedList<Block> chain = new LinkedList<>();

    /**
     * The mining difficulty level of the blockchain.
     */
    private int difficulty;

    /**
     * Constructs a new blockchain with a specified difficulty level.
     *
     * @param difficulty The difficulty level for mining new blocks.
     */
    public BlockChain(int difficulty) {
        this.difficulty = difficulty;
        // Create and mine genesis block
        Block genesis = new Block("0", System.currentTimeMillis(), List.of("0"));
        chain.add(genesis);
    }

    /**
     * Adds a new block to the blockchain.
     *
     * @param newBlock The new block to add to the blockchain.
     */
    public void addBlock(Block newBlock) {
        if (newBlock != null) {
            chain.add(newBlock);
        }
    }

    /**
     * Validates a new block before adding it to the blockchain.
     *
     * @param newBlock      The new block to validate.
     * @param previousBlock The previous block in the blockchain.
     * @return True if the new block is valid, false otherwise.
     */
    public boolean isValidNewBlock(Block newBlock, Block previousBlock) {
        System.out.println("Validating new block");
        if (newBlock == null || previousBlock == null) {
            return false;
        }

        System.out.println("Checking if new block hash is correct");
        // Check the hash of the new block
        if (!newBlock.getHash().equals(newBlock.calculateHash())) {
            return false;
        }

        // Check the proof of work
        System.out.println("Checking proof of work");
        String target = new String(new char[difficulty]).replace('\0', '0');
        if (!newBlock.getHash().substring(0, difficulty).equals(target)) {
            return false;
        }
        System.out.println("checking if block contains duplicate transaction " + newBlock.getTransactions().toString() );
        if (containsTransaction(newBlock.getTransactions().get(0))) {
            System.out.println("Duplicate transaction in block");
            return false;
        }

        return true;
    }

    /**
     * Retrieves the last block in the blockchain.
     *
     * @return The last block in the blockchain.
     */
    public Block getLastBlock() {
        return chain.getLast();
    }

    /**
     * Retrieves the difficulty level of the blockchain.
     *
     * @return The difficulty level of the blockchain.
     */
    public int getDifficulty() {
        return difficulty;
    }

    /**
     * Validates a received missing chain before adding it to the blockchain.
     *
     * @param incomingBlocks The missing chain received from another node.
     * @return True if the missing chain is valid, false otherwise.
     */
    public boolean isValidMissingChain(List<Block> incomingBlocks) {
        System.out.println("Validating received missing chain");
        if (incomingBlocks.isEmpty()) return false;

        // Check for a valid link to the existing chain
        Block firstIncomingBlock = incomingBlocks.get(0);
        String expectedPreviousHash = firstIncomingBlock.getPreviousHash();
        Block linkingBlock = chain.getLast();

        if (!Objects.equals(linkingBlock.getHash(), expectedPreviousHash)) {
            return false; // The first incoming block must correctly link to a block in our existing chain
        }

        // Validate the incoming chain
        Block previousBlock = linkingBlock;
        long incomingChainWork = 0;
        for (Block block : incomingBlocks) {
            if (!isValidNewBlock(block, previousBlock)) return false;
            incomingChainWork += block.getNonce(); // Calculate work for proof of work consensus, replace if using a different method
            previousBlock = block;
        }

        // Compare the total work of the incoming chain with the current chain's work from the divergence point
        long currentChainWork = calculateWorkFromBlock(linkingBlock);
        return incomingChainWork > currentChainWork;
    }

    /**
     * Calculates the total work from a given block to the end of the chain.
     *
     * @param startBlock The block to start calculating work from.
     * @return The total work from the start block to the end of the chain.
     */
    private long calculateWorkFromBlock(Block startBlock) {
        // Calculate the total work from a given block to the end of the chain
        long work = 0;
        boolean startCounting = false;
        for (Block block : chain) {
            if (startCounting) {
                work += block.getNonce(); // Assuming proof of work, this adds the nonce as a simplistic work metric
            }
            if (block.equals(startBlock)) {
                startCounting = true;
            }
        }
        return work;
    }

    /**
     * Adds a missing chain to the blockchain after validating it.
     *
     * @param block          The block that triggered the request for a missing chain.
     * @param incomingBlocks The missing chain received from another node.
     */
    public void addMissingChain(Block block, List<Block> incomingBlocks) {
        Block linkingBlock = findLinkingBlockByHash(incomingBlocks.get(0).getPreviousHash());
        // Determine the index of the linking block in the chain
        int linkingIndex = chain.indexOf(linkingBlock);

        if (linkingIndex == -1) {
            System.out.println("Linking block is not in the current chain.");
            return;
        }

        // Remove blocks from the linking block's index to the end of the chain
        while (chain.size() > linkingIndex + 1) {
            chain.remove(chain.size() - 1);
        }

        // Add all incoming blocks after the linking block
        chain.addAll(incomingBlocks);
        System.out.println("Chain updated from the linking block with incoming blocks.");
    }

    /**
     * Retrieves the current blockchain.
     *
     * @return The current blockchain.
     */
    public List<Block> getChain() {
        return chain;
    }

    /**
     * Replaces the current blockchain with a new chain.
     *
     * @param newChain The new chain to replace the current blockchain.
     */
    public void replaceChain(List<Block> newChain) {
        this.chain = new LinkedList<>(newChain);
    }

    /**
     * Checks if the blockchain contains a specific transaction.
     *
     * @param transaction The transaction to check for in the blockchain.
     * @return True if the transaction is in the blockchain, false otherwise.
     */
    public boolean containsTransaction(String transaction) {
        if(chain == null || chain.isEmpty()) return false;
        for (Block block : chain) {
            if (block.getTransactions().contains(transaction)) {
                System.out.println("Transaction already in chain");
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the blockchain contains a list of transactions.
     *
     * @param transactions The list of transactions to check for in the blockchain.
     * @return True if all transactions are in the blockchain, false otherwise.
     */
    public boolean containsTransactions(List<String> transactions) {
        for (String transaction : transactions) {
            if (!containsTransaction(transaction)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Finds a missing chain in the blockchain by a given hash.
     *
     * @param hash The hash of the block to find in the blockchain.
     * @return The missing chain from the block with the given hash to the end of the blockchain.
     */
    public List<Block> findMissingChainByHash(String hash) {
        // This method needs to search through the blockchain to find the block with the given hash
        for (Block block : chain) {
            if (block.getHash().equals(hash)) {
                return chain.subList(chain.indexOf(block), chain.size());
            }
        }
        return emptyList();
    }

    /**
     * Checks if the blockchain is empty.
     *
     * @return True if the blockchain is empty, false otherwise.
     */
    public boolean isEmpty() {
        return chain.isEmpty();
    }

    /**
     * Returns a string representation of the blockchain.
     *
     * @return A string representation of the blockchain.
     */
    @Override
    public String toString() {
        return "BlockChain{" +
                "chain=" + chain +
                ", difficulty=" + difficulty +
                '}';
    }

    /**
     * Finds a block in the blockchain that links to a block with a given hash.
     *
     * @param previousHash The hash of the block to find in the blockchain.
     * @return The block that links to the block with the given hash.
     */
    public Block findLinkingBlockByHash(String previousHash) {
        for (Block block : chain) {
            if (block.getHash().equals(previousHash)) {
                return block;
            }
        }
        return null;
    }

    /**
     * Checks if the blockchain contains a specific block.
     *
     * @param block The block to check for in the blockchain.
     * @return True if the block is in the blockchain, false otherwise.
     */
    public boolean containsBlock(Block block) {
        return chain.contains(block);
    }
}
