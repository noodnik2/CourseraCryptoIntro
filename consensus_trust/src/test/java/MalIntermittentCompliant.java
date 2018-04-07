import java.util.Collections;
import java.util.Set;

/**
 *  Intermittently sends the transactions that would be sent by a compliant
 *  node during each round, sending an empty set in-between
 *  @author Marty Ross
 */
public class MalIntermittentCompliant extends CompliantNode {

    private int currentRound = 1;

    public MalIntermittentCompliant(
        final double p_graph,
        final double p_malicious,
        final double p_txDistribution,
        final int numRounds
    ) {
        super(p_graph, p_malicious, p_txDistribution, numRounds);
    }

    @Override
    public Set<Transaction> sendToFollowers() {
        if ((currentRound & 1) != 0) {
            return super.sendToFollowers();
        }
        return Collections.emptySet();
    }

    @Override
    public void receiveFromFollowees(final Set<Candidate> p_candidates) {
        super.receiveFromFollowees(p_candidates);
        currentRound++;
    }

}
