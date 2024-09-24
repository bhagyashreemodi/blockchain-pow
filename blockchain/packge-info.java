/**
 * Provides the core classes and functionality for building a blockchain system.
 * <p>
 * The blockchain package contains the essential components required to create and manage a decentralized blockchain network.
 * It includes classes for representing blocks, the blockchain itself, and the miner nodes that participate in the network.
 * <p>
 * The main classes in this package are:
 * <ul>
 *     <li>{@link blockchain.Block}: Represents a single block in the blockchain, containing transaction data and linking to the previous block.</li>
 *     <li>{@link blockchain.BlockChain}: Represents the entire blockchain, managing the chain of blocks and providing methods for adding and validating blocks.</li>
 *     <li>{@link blockchain.MinerNode}: Represents a node in the blockchain network that mines blocks and handles communication with clients and peers.</li>
 * </ul>
 * <p>
 * The blockchain package provides the core functionality for creating a secure, decentralized, and consensus-based blockchain system.
 * It enables the creation, validation, and synchronization of blocks across multiple nodes in the network, ensuring the integrity and immutability of the blockchain.
 */
package blockchain;