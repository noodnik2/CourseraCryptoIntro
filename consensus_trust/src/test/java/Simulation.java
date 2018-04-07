// Example of a Simulation. This test runs the nodes on a random graph.
// At the end, it will print out the Transaction ids which each node
// believes consensus has been reached upon. You can use this simulation to
// test your nodes. You will want to try creating some deviant nodes and
// mixing them in the network to fully test.

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;


/**
 *  My own customized "Simulation" class, based upon the one provided in the class materials,
 *  modified to collect and report statistics about final "consensus" status.
 */
public class Simulation {

    private static final int STD_VERBOSITY = 0;
    private static final int DEBUG_VERBOSITY = 100;
    private static final int VERBOSITY = DEBUG_VERBOSITY;

    public static void main(String[] args) {

        // There are four required command line arguments: p_graph (.1, .2, .3),
        // p_malicious (.15, .30, .45), p_txDistribution (.01, .05, .10),
        // and numRounds (10, 20). You should try to test your CompliantNode
        // code for all 3x3x3x2 = 54 combinations.

        int numNodes = 100;
        double p_graph = Double.parseDouble(args[0]); // parameter for random graph: prob. that an edge will exist
        double p_malicious = Double.parseDouble(args[1]); // prob. that a node will be set to be malicious
        double p_txDistribution = Double.parseDouble(args[2]); // probability of assigning an initial transaction to each node
        int numRounds = Integer.parseInt(args[3]); // max. number of simulation rounds your nodes will run for
        if (args.length > 4) {
            numNodes = Integer.parseInt(args[4]); // number of nodes to create
        }

        log(
            STD_VERBOSITY,
            String.format(
                "starting(p_graph=%s, p_malicious=%s, p_txDistribution=%s, numRounds=%s, numNodes=%s)",
                p_graph, p_malicious, p_txDistribution, numRounds, numNodes
            )
        );

        // pick which nodes are malicious and which are compliant
        Node[] nodes = new Node[numNodes];
        int nMaliciousNodes = 0;
        for (int i = 0; i < numNodes; i++) {
            if (Math.random() < p_malicious) {
                // When you are ready to try testing with malicious nodes, replace the
                // instantiation below with an instantiation of a MaliciousNode
                nodes[i] = createMaliciousNode(p_graph, p_malicious, p_txDistribution, numRounds);
                nMaliciousNodes++;
            }
            else {
                nodes[i] = new CompliantNode(p_graph, p_malicious, p_txDistribution, numRounds);
            }
        }
        System.out.printf("nMaliciousNodes(%s) out of(%s), or(%.0f%%)\n", nMaliciousNodes, numNodes, 100d * nMaliciousNodes / numNodes);

        // initialize random follow graph
        boolean[][] followees = new boolean[numNodes][numNodes]; // followees[i][j] is true iff i follows j
        for (int i = 0; i < numNodes; i++) {
            for (int j = 0; j < numNodes; j++) {
                if (i == j) continue;
                if (Math.random() < p_graph) { // p_graph is .1, .2, or .3
                    followees[i][j] = true;
                }
            }
        }

        // notify all nodes of their followees
        for (int i = 0; i < numNodes; i++)
            nodes[i].setFollowees(followees[i]);

        // initialize a set of 500 valid Transactions with random ids
        int numTx = 500;
        HashSet<Integer> validTxIds = new HashSet<>();
        Random random = new Random();
        for (int i = 0; i < numTx; i++) {
            int r = random.nextInt();
            validTxIds.add(r);
        }


        // distribute the 500 Transactions throughout the nodes, to initialize
        // the starting state of Transactions each node has heard. The distribution
        // is random with probability p_txDistribution for each Transaction-Node pair.
        for (int i = 0; i < numNodes; i++) {
            HashSet<Transaction> pendingTransactions = new HashSet<>();
            for (Integer txID : validTxIds) {
                if (Math.random() < p_txDistribution) // p_txDistribution is .01, .05, or .10.
                    pendingTransactions.add(new Transaction(txID));
            }
            nodes[i].setPendingTransaction(pendingTransactions);
        }


        // Simulate for numRounds times
        for (int round = 1; round <= numRounds; round++) {

            log(STD_VERBOSITY, String.format("round(%s)", round));

            // gather all the proposals into a map. The key is the index of the node receiving
            // proposals. The value is an ArrayList containing 1x2 Integer arrays. The first
            // element of each array is the id of the transaction being proposed and the second
            // element is the index # of the node proposing the transaction.
            HashMap<Integer, Set<Candidate>> allProposals = new HashMap<>();

            for (int i = 0; i < numNodes; i++) {
                Set<Transaction> proposals = nodes[i].sendToFollowers();
                for (Transaction tx : proposals) {
                    if (!validTxIds.contains(tx.id))
                        continue; // ensure that each tx is actually valid

                    for (int j = 0; j < numNodes; j++) {
                        if (!followees[j][i])
                            continue; // tx only matters if j follows i

                        if (!allProposals.containsKey(j)) {
                            Set<Candidate> candidates = new HashSet<>();
                            allProposals.put(j, candidates);
                        }

                        Candidate candidate = new Candidate(tx, i);
                        allProposals.get(j).add(candidate);
                    }

                }
            }

            // Distribute the Proposals to their intended recipients as Candidates
            for (int i = 0; i < numNodes; i++) {
                if (allProposals.containsKey(i))
                    nodes[i].receiveFromFollowees(allProposals.get(i));
            }

            consensusReached(numNodes, nodes);

        }

    }

    private static Node createMaliciousNode(double p_graph, double p_malicious, double p_txDistribution, int numRounds) {
//        return new MalDoNothing();
//        return new MalIntermittentCompliant(p_graph, p_malicious, p_txDistribution, numRounds);
        return new MalSendOneTx();
    }

    private static boolean consensusReached(final int numNodes, final Node[] nodes) {

        // confirm results
        final Map<Set<Transaction>, Integer> transactionSets = new HashMap<>();
        for (int i = 0; i < numNodes; i++) {
            final Set<Transaction> transactions = nodes[i].sendToFollowers();
//            originalTxReport(i, transactions);
            final Integer currentCount = transactionSets.get(transactions);
            transactionSets.put(transactions, (currentCount == null) ? 1 : currentCount + 1);
        }

        int totalWeight = 0;
        int maxWeight = 0;
        Entry<Set<Transaction>, Integer> winnerEntry = null;

        for (final Entry<Set<Transaction>, Integer> transactionSetEntry : transactionSets.entrySet()) {
            final Set<Transaction> transactionSet = transactionSetEntry.getKey();
            final int setSize = transactionSet.size();
            final Integer setWeight = transactionSetEntry.getValue();
            log(
                STD_VERBOSITY,
                String.format(
                    "transactionSet(%s),size(%s),weight(%s)",
                    transactionSet.hashCode(),
                    setSize,
                    setWeight
                )
            );
            totalWeight += setWeight;
            if (maxWeight < setWeight) {
                maxWeight = setWeight;
                winnerEntry = transactionSetEntry;
            }
        }

        log(
            STD_VERBOSITY,
            String.format(
                "totalWeight(%s), winnerSetId(%s), winnerSetSize(%s), winnerWeight(%s), consensusPct(%.2f%%)",
                totalWeight,
                winnerEntry == null ? "N/A" : winnerEntry.getKey().hashCode(),
                winnerEntry == null ? "N/A" : winnerEntry.getKey().size(),
                winnerEntry == null ? "N/A" : winnerEntry.getValue(),
                winnerEntry == null ? 0d : 100d * winnerEntry.getValue() / totalWeight
            )
        );

        return transactionSets.size() == 1;
    }


    //
    //  Private class methods
    //

    private static void originalTxReport(int i, Set<Transaction> transactions) {
        System.out.println("Transaction ids that Node " + i + " believes consensus on:");
        for (Transaction tx : transactions) {
           System.out.println(tx.id);
        }
        System.out.println();
        System.out.println();
    }

    private static void log(final int level, final String message) {
        if (level <= VERBOSITY) {
            System.out.println(message);
        }
    }

}

