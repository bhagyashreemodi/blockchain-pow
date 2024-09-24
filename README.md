## Lab 4: Proof-of-work Blockchain Consensus


### Project topic & goals
Our blockchain system leverages a decentralized network structure where each node participates in the maintenance and extension of the blockchain.

Initially, each node starts with a basic blockchain containing only a Genesis block and an empty pool for mined blocks. Nodes operate independently but follow a collaborative consensus mechanism to validate and add new blocks.

The system combines automated processes like block mining and broadcasting with interactive components that allow user transactions and queries. Through a combination of Proof of Work (PoW), peer-to-peer communications, and rigorous validation protocols, the network ensures the integrity, security, and continuity of the blockchain.

### Tasks
- Accept new transactions from clients into a pool</li>
- Create a non-trivial number of workers that attempt to form valid blocks, containing any number of transactions from the pool</li>
- Ensure that the rules of block selection are respected when multiple workers submit valid blocks, especially when multiple valid blocks are submitted with common content</li>
- Ensure that workers can learn once valid blocks have been accepted, so they know the corresponding content is no longer in the pool (meaning nothing is duplicated on the blockchain)</li>
- Demonstrate that invalid blocks are rejected</li>
- Append valid blocks to the blockchain in a way that ensures consistency across all clients

### Directory Structure
The project has the following directory structure:
```
.
├── blockchain/
│   ├── Block.java
│   ├── BlockChain.java
│   └── MinerNode.java
├── common/
│   └── FormattedSystemOut.java
├── test/
│   ├── Lab4FinalTests.java
│   ├── Test.java
│   ├── TestBlockMining.java
│   ├── TestBlockchainConcurrent.java
│   ├── TestInitialSetup.java
│   ├── TestNodeFailureResilience.java
│   └── TestOutdatedInformationRejection.java
├── Makefile
└── README.md
```
- The `blockchain/` directory contains the core classes for the blockchain system.
- The `common/` directory contains utility classes used by the project.
- The `test/` directory contains the test classes for different scenarios.

### Dependencies to run this code
JDK-21

### How to run tests and build the project

The project uses a Makefile to automate the build and testing process. Here are the available make commands:

- `make build`: Compiles all Java files in the project.
- `make test`: Runs the conformance tests.
- `make clean`: Deletes all class files and generated documentation, leaving only the source files.
- `make docs`: Generates Javadoc documentation for the main package and supporting library.
- `make docs-test`: Generates Javadoc documentation for the test suite.

To run the tests, use the following command:
```  
make test
```  

### Test List
***Test_Initial_Setup - 15***<br/>
The system should initialize with all nodes correctly possessing the block and being capable of network communication.

***Test_Invalid_Transaction_Rejection - 15***<br/>
Invalid transactions should be identified and rejected by all nodes, ensuring they are not added to the transaction pool, maintaining the integrity of the blockchain.

***Test_First_Block_Mining - 30***<br/>
Nodes must successfully mine and propagate the first block uniformly across the network.

***Test_Block_Consensus - 40***<br/>
Following multiple block mines, all nodes must agree on the blockchain state and maintain an identical ledger.

***Test_Node_Failure_Resilience - 50***<br/>
During node failures, the system must continue to operate without disruption and maintain blockchain integrity.

***Test_Outdated_Information_Rejection - 50***<br/>
Nodes working from outdated forks should have their blocks rejected, with the system forcing these nodes to resynchronize with the current valid blockchain.

### Failure Scenarios
The blockchain system may encounter various failure scenarios:

- **Transaction Pool Exhaustion**: The transaction pool may grow indefinitely if transactions are not properly removed, leading to memory exhaustion and system instability.

- **Large Transaction Size**: Excessively large transactions can impact the performance and stability of the blockchain, causing delays and potential forks that cannot be resolved in time.

- **Network Connectivity Issues**: Nodes may experience network connectivity problems, leading to inefficient propagation of blocks and transactions, and inconsistencies in the blockchain.

- **Blockchain Synchronization Issues**: Nodes may have difficulty synchronizing their blockchain with the rest of the network, especially if they have been offline for an extended period, leading to forks and inconsistencies.