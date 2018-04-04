import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *  Coursera Introduction to Crypto Currency Course<p />
 *  Assignment2: Consensus Trust<br />
 *  CompliantNode refers to a node that follows the rules (i.e., is not malicious)
 *  @author Marty Ross
 */
public class CompliantNode implements Node {

    private final Set<Transaction> pendingTransactions;
    private final int numRounds;

    private int receiveRound;

    /**
     *  @param p_graph probability that an edge will exist
     *  @param p_malicious probability that a node will be set to be malicious
     *  @param p_txDistribution probability of assigning an initial transaction to each node
     *  @param numRounds number of simulation rounds your nodes will run for
     */
    public CompliantNode(
        final double p_graph,
        final double p_malicious,
        final double p_txDistribution,
        final int numRounds
    ) {
        pendingTransactions = new HashSet<>();
        this.numRounds = numRounds;
    }

    @Override
    public void setFollowees(final boolean[] p_followees) {
        // nothing needed since the Simulation only sends transactions
        // from followees (look at its source to verify)
    }

    @Override
    public void setPendingTransaction(final Set<Transaction> p_transactions) {
        pendingTransactions.addAll(p_transactions);
    }

    @Override
    public Set<Transaction> sendToFollowers() {
        return Collections.unmodifiableSet(pendingTransactions);
    }

    @Override
    public void receiveFromFollowees(final Set<Candidate> p_candidates) {

        if (receiveRound >= numRounds) {
            throw new IllegalStateException();
        }

        receiveRound++;

        pendingTransactions.clear();

        if (receiveRound < numRounds) {
            // echo all transactions to build consensus
            for (final Candidate c : p_candidates) {
                pendingTransactions.add(c.tx);
            }
            return;
        }

        // judgement time: group transactions by sender
        final Map<Integer, Set<Transaction>> senderTransactionsMap = new HashMap<>();
        for (final Candidate c : p_candidates) {
            final Set<Transaction> existingTransactions = senderTransactionsMap.get(c.sender);
            final Set<Transaction> transactions;
            if (existingTransactions == null) {
                transactions = new HashSet<>();
                senderTransactionsMap.put(c.sender, transactions);
            } else {
                transactions = existingTransactions;
            }
            transactions.add(c.tx);
        }

        // collapse common transaction sets sent by different senders
        final Map<Set<Transaction>, Set<Integer>> consensusSets = new HashMap<>();
        for (final Map.Entry<Integer, Set<Transaction>> stme : senderTransactionsMap.entrySet()) {
            final Set<Transaction> txSet = stme.getValue();
            final Set<Integer> existingTxSetServers = consensusSets.get(txSet);
            final Set<Integer> txSetServers;
            if (existingTxSetServers == null) {
                txSetServers = new HashSet<>();
                consensusSets.put(txSet, txSetServers);
            } else {
                txSetServers = existingTxSetServers;
            }
            txSetServers.add(stme.getKey());
        }

        // calculate and return the most popular transaction set
        int maxVotes = -1;
        Set<Transaction> mostPopularSet = null;
        for (final Map.Entry<Set<Transaction>, Set<Integer>> cse : consensusSets.entrySet()) {
            if (maxVotes < cse.getValue().size()) {
                maxVotes = cse.getValue().size();
                mostPopularSet = cse.getKey();
            }
        }

        if (mostPopularSet != null) {
            pendingTransactions.addAll(mostPopularSet);
        }

//            System.out.printf(
//                "most popular set with maxVotes(%s) has(%s) transactions\n",
//                maxVotes,
//                pendingTransactions.size()
//            );

    }

}
