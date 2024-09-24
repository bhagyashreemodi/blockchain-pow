package blockchain;

import java.io.Serial;
import java.io.Serializable;
import java.security.MessageDigest;
import java.util.List;

/**
 * Initializes a new block in the blockchain.
 *
 */
public class Block implements Serializable {
    /** Serializable version UID for compatibility during serialization. */
    @Serial
    private static final long serialVersionUID = 1L;
    /** The The hash of the block. */
    private String hash;
    /** The hash of the previous block in the blockchain. */
    private final String previousHash;
    /** The timestamp of when the block was created. */
    private final long timestamp;
    /** The list of transactions included in the block. */
    private final List<String> transactions;
    /** The nonce value used in mining the block. */
    private int nonce;

    /**
     * Constructor to create a new block.
     *
     * @param previousHash The hash of the previous block in the blockchain.
     * @param timestamp    The timestamp of when the block was created.
     * @param transactions The list of transactions included in the block.
     */
    public Block(String previousHash, long timestamp, List<String> transactions) {
        //this.index = index;
        this.previousHash = previousHash;
        this.timestamp = timestamp;
        this.transactions = transactions;
        this.nonce = 0;
        this.hash = calculateHash();
    }

    /**
     * Mines the block by finding a hash with a specified prefix difficulty.
     *
     * @param prefixDifficulty The number of leading zeros required in the hash.
     */
    public void mineBlock(int prefixDifficulty) {
        long startTime = System.currentTimeMillis();
        System.out.println("Mining block with transactions:" +  transactions);
        String prefixString = new String(new char[prefixDifficulty]).replace('\0', '0');
        while (!hash.substring(0, prefixDifficulty).equals(prefixString)) {
            nonce++;
            hash = calculateHash();
        }
        System.out.println("Block mined with transactions : " + transactions + " in time: " + (System.currentTimeMillis() - startTime) + "ms");
    }

    /**
     * Retrieves the hash of the block.
     *
     * @return The hash of the block.
     */
    public String getHash() {
        return hash;
    }

    /**
     * Retrieves the hash of the previous block in the blockchain.
     *
     * @return The hash of the previous block.
     */
    public String getPreviousHash() {
        return previousHash;
    }

    /**
     * Retrieves the timestamp of the block.
     *
     * @return The timestamp of the block.
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Calculates the hash of the block based on its contents.
     *
     * @return The hash of the block.
     */
    public String calculateHash() {
        String dataToHash = previousHash + transactions.toString() + nonce;
        MessageDigest digest;
        byte[] bytes;
        try {
            digest = MessageDigest.getInstance("SHA-256");
            bytes = digest.digest(dataToHash.getBytes("UTF-8"));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Retrieves the nonce of the block.
     *
     * @return The nonce of the block.
     */
    public int getNonce() {
        return nonce;
    }

    /**
     * Retrieves the list of transactions included in the block.
     *
     * @return The list of transactions.
     */
    public List<String> getTransactions() {
        return transactions;
    }

    // Getters and setters omitted for brevity

    /**
     * Returns a string representation of the block.
     *
     * @return A string representation of the block.
     */
    @Override
    public String toString() {
        return "Block{hash='" + hash + '\'' +
                ", previousHash='" + previousHash + '\'' +
                ", timestamp=" + timestamp +
                ", transactions=" + transactions +
                ", nonce=" + nonce +
                '}';
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param obj The reference object with which to compare.
     * @return {@code true} if this object is the same as the obj argument; {@code false} otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Block block = (Block) obj;
        return nonce == block.nonce && hash.equals(block.hash) && previousHash.equals(block.previousHash) && transactions.equals(block.transactions);
    }
}
