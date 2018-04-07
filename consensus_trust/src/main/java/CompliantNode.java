import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

/**
 *  Coursera Introduction to Crypto Currency Course<p />
 *  Assignment2: Consensus Trust<br />
 *  CompliantNode refers to a node that follows the rules (i.e., is not malicious)
 *  @author Marty Ross
 */
public class CompliantNode implements Node {

    private final int numRounds;
    private final double pTxDistribution;
    private final double pMmalicious;

    private int nFollowees;
//    private int nTotalTransactionsGuess;
    private int receiveRound;
    private Map<Integer, Set<Transaction>> followeeTransactionsMap;
    private boolean[] blacklist;

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
        this.numRounds = numRounds;
        this.pMmalicious = p_malicious;
        this.pTxDistribution = p_txDistribution;
        this.followeeTransactionsMap = new HashMap<>();
    }

    @Override
    public void setFollowees(final boolean[] p_followees) {
        // the Simulation only sends transactions from followees
        nFollowees = getCount(p_followees);
        blacklist = new boolean[p_followees.length];
    }

    private int getCount(final boolean[] p_followees) {
        return (int) IntStream
            .range(0, p_followees.length)
            .filter(idx -> p_followees[idx])
            .count()
        ;
    }

    @Override
    public void setPendingTransaction(final Set<Transaction> p_transactions) {
        followeeTransactionsMap.put(-1, p_transactions);   // -1 for "self"
//        nTotalTransactionsGuess = (int) Math.round(p_transactions.size() * pTxDistribution);
    }

    @Override
    public Set<Transaction> sendToFollowers() {
        final Set<Transaction> totalSet = new HashSet<>();
        for (final Set<Transaction> serverSet : followeeTransactionsMap.values()) {
            totalSet.addAll(serverSet);
        }
        return totalSet;
    }

    @Override
    public void receiveFromFollowees(final Set<Candidate> p_candidates) {

        if (receiveRound >= numRounds) {
            throw new IllegalStateException();
        }

        receiveRound++;

        performGatheringRound(p_candidates);

        if (receiveRound >= numRounds) {
            performConsensusRound();
        }

    }

    private void performGatheringRound(final Set<Candidate> p_candidates) {

        final Map<Integer, Set<Transaction>> senderTransactionMap = getSenderTransactionMap(p_candidates);

        // 1: ensure we hear of everything we've heard of before
        for (final Map.Entry<Integer, Set<Transaction>> ftme : followeeTransactionsMap.entrySet()) {
            final Integer followee = ftme.getKey();
            if (followee < 0 || blacklist[followee]) {
                // ignore special "internal" followee key in map
                continue;
            }
            final Set<Transaction> newFolloweeTransactions = senderTransactionMap.get(followee);
            if (newFolloweeTransactions != null) {
                // we've heard from this followee before, so we can check it for consistency
                final Set<Transaction> previouslyProposedFolloweeTransactions = ftme.getValue();
                // check that all previously sent transactions are being sent again
                if (!newFolloweeTransactions.containsAll(previouslyProposedFolloweeTransactions)) {
                    // blacklist the followee since it didn't deliver all of the transactions it previously did
                    System.out.printf("blacklisted(%s) in round(%s) since omitted some txs it previously sent\n", followee, receiveRound);
                    blacklist[followee] = true;
                }
            } else {
                // we heard from this sender during previous rounds
                // but not during this round, so we disqualify it
                System.out.printf("blacklisted(%s) in round(%s) since didn't send anything\n", followee, receiveRound);
                blacklist[followee] = true;
            }
        }

        // 2: gather new stuff
        for (final Map.Entry<Integer, Set<Transaction>> stme : senderTransactionMap.entrySet()) {
            final Integer followee = stme.getKey();
            if (followee < 0 || blacklist[followee]) {
                // ignore special "internal" followee key in map
                continue;
            }
            final Set<Transaction> newlyProposedFolloweeTransactions = stme.getValue();
            final Set<Transaction> followeeTransactions;
            final Set<Transaction> previouslySeenFolloweeTransactions = followeeTransactionsMap.get(followee);
            if (previouslySeenFolloweeTransactions == null) {
                followeeTransactions = new HashSet<>();
                followeeTransactionsMap.put(followee, followeeTransactions);
            } else {
                // we previously saw transactions from this followee, so check to see that it sent them all again
                followeeTransactions = previouslySeenFolloweeTransactions;
            }
            followeeTransactions.addAll(newlyProposedFolloweeTransactions);
        }

    }

    // judgement time: reduce transactions into single set, by popularity
    private void performConsensusRound() {

        // produce a count of votes for each transaction
        final Map<Transaction, Integer> transactionVotesMap = new HashMap<>();
        for (final Map.Entry<Integer, Set<Transaction>> ftme : followeeTransactionsMap.entrySet()) {
            final int server = ftme.getKey();
            if (server >= 0 && blacklist[server]) {
                continue;
            }
            System.out.printf("server(%s) voted for(%s) transactions\n", server, ftme.getValue().size());
            for (final Transaction tx : ftme.getValue()) {
                final Integer txVotes = transactionVotesMap.get(tx);
                transactionVotesMap.put(tx, (txVotes == null) ? 1 : txVotes + 1);
            }
        }

        // We expect transactions to be voted on exactly once by all compliant nodes
        // which "saw" the transaction, and we assume that by this time (the final
        // round) all nodes will have seen all transactions.  Since we can calculate
        // the estimated number of non-compliant nodes we're connected to (as the
        // number of followees times the probability of "malicious" nodes), we'll
        // accept all transactions having a vote count greater than that number.

        final int nBlacklisted = getCount(blacklist);
        final int minVoteCount = (int) ((nFollowees - nBlacklisted) * pMmalicious);
        System.out.printf("nBlacklisted(%s), minVoteCount(%s)\n", nBlacklisted, minVoteCount);
        final Set<Transaction> consensusTransactions = new HashSet<>();
        for (final Map.Entry<Transaction, Integer> tvme : transactionVotesMap.entrySet()) {
            if (tvme.getValue() >= minVoteCount) {
                consensusTransactions.add(tvme.getKey());
                continue;
            }
            System.out.printf("transaction(%s) didn't get enough votes(%s)\n", tvme.getKey().id, tvme.getValue());
        }
        followeeTransactionsMap.clear();
        followeeTransactionsMap.put(-2, consensusTransactions);
    }

    /**
     *  @param p_candidates candidate transactions proposed by followee nodes
     *  @return map of node index to the set of transaction(s) proposed by that node
     */
    private Map<Integer, Set<Transaction>> getSenderTransactionMap(final Set<Candidate> p_candidates) {
        final Map<Integer, Set<Transaction>> senderTransactionMap = new HashMap<>();
        for (final Candidate c : p_candidates) {
            Set<Transaction> serverTransactions;
            final Set<Transaction> existingServerTransactions = senderTransactionMap.get(c.sender);
            if (existingServerTransactions == null) {
                serverTransactions = new HashSet<>();
                senderTransactionMap.put(c.sender, serverTransactions);
            } else {
                serverTransactions = existingServerTransactions;
            }
            serverTransactions.add(c.tx);
        }
        return senderTransactionMap;
    }

}
