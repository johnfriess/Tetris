package assignment;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class BrainTest {
    private QTable q;
    private QState s;

    @Before
    public void setUpTable() {
        q = new QTable(10, 20, 2, 3, 4);
        s = new QState(0, 10, 20, 2, 3, 4);
    }
    
    @Test
    public void testMakingValidMoves() {
        int numGames = 100;
        for(int i = 0; i < numGames; i++)
            assertTrue(q.testOneGame() != -1);
    }

    @Test
    public void checkMemoryInBounds() {
        int numGames = 100;
        for(int i = 0; i < numGames; i++) {
            q.trainOneGame();
            assertTrue(q.checkMemoryInBounds());
        }
    }

    @Test
    public void encodeDecode() {
        for(int i = 0; i < Math.pow(7,5); i++)
            for(int pType = 0; pType < 7; pType++)
                assertEquals(i, q.encode(s.decode(i*7 + pType)).getIdx() / 7);
    }
}
