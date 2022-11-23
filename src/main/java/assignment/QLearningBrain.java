package assignment;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.awt.Point;

import assignment.Piece.PieceType;

/**
 * AI that implements QLearning built off a heuristic approach
 */
public class QLearningBrain implements Brain {
    // training specs
    private static final long numTrainingGames = 2000000L;
    private static final String saveFilePath =  "src/main/java/assignment/qTable_";
    public static final int PRINT_TABLE_FREQ = 1000;
    public static final int SAVE_TABLE_FREQ = 10000;
    public static final int TRAIN_TABLE_FREQ = 1;
    public static final int EPOCHS = 5;

    // table to solve for and use
    private QTable q;

    // queue for nextMove
    private List<Board.Action> loadedMoves;
    private int moveIdx;

    /**
     * Initializes QLearning Brain
     * @param width board width
     * @param height board height (not including top space)
     * @param stateWidthShrink pool size for x-dimension
     * @param stateHeightShrink pool size for y-dimension
     * @param topSpace top space (typically 4) 
     * @param filePath path to saved table, if null then it will train its own table
     */
    public QLearningBrain(int width, int height, int stateWidthShrink, int stateHeightShrink, int topSpace, String filePath) {
        q = new QTable(width, height, stateWidthShrink, stateHeightShrink, topSpace);
        loadedMoves = new ArrayList<>();
        moveIdx = 0;

        if(filePath == null)
            train();
        else
            q.loadTable(filePath);
    }

    /**
     * Train QTable using training specs, display training/testing results and save table
     */
    private void train() {
        for(long i = 0; i <= numTrainingGames; i++){
            int score = q.trainOneGame();
            if(i % TRAIN_TABLE_FREQ == 0)
                q.trainTable(EPOCHS);
            if(i % PRINT_TABLE_FREQ == 0) {
                System.out.println("Training Game, Score: "+i+", "+score);
                System.out.println("Testing Game, Score: "+i+", "+q.testOneGame());
            }
            if(i % SAVE_TABLE_FREQ == 0) {
                System.out.println("Saved @ Game: " + i);
                q.saveTable(saveFilePath+i+".txt");
            }
        }
    }

    /**
     * Looks at queued moves to get board's next move.
     * If queue is empty then it recomputes a series of next moves
     * @param currentBoard current input board
     * @return next action for brain
     */
    public Board.Action nextMove(Board currentBoard) {
        if(loadedMoves.isEmpty() || moveIdx >= loadedMoves.size()) {
            loadedMoves = q.runTable(currentBoard);
            moveIdx = 0;
        }
        return loadedMoves.get(moveIdx++);
    }
    
}

/**
 * Memory structure object: contains action taken at state, reward and best action at next state (nextAction)
 */
class Memory {
    private QAction nextAction;
    private QAction action;
    private double reward;

    public Memory(QAction aPrime, QAction a, double r) {
        nextAction = aPrime;
        action = a;
        reward = r;
    }

    public QAction getNextAction() { return nextAction; }
    public QAction getAction() { return action; }
    public double getReward() { return reward; }
}

/**
 * Table storing and computing Q values
 */
class QTable {
    // piece information for simulation
    public static final Piece[] PIECES = new Piece[] {
        new TetrisPiece(PieceType.STICK),
        new TetrisPiece(PieceType.SQUARE),
        new TetrisPiece(PieceType.T),
        new TetrisPiece(PieceType.LEFT_L),
        new TetrisPiece(PieceType.RIGHT_L),
        new TetrisPiece(PieceType.LEFT_DOG),
        new TetrisPiece(PieceType.RIGHT_DOG)
    };
    public static final int NUM_PIECES = PIECES.length;
    public static final Piece dummyPiece = new TetrisPiece(PieceType.SQUARE);

    // reward specs
    public static final double GAME_OVER_PENALTY = -200.0;
    public static final double DROP_REWARD = 0.01;
    public static final double ROWS_CLEAR_REWARD = 1.0;
    public static final double MILESTONE_REWARD_FREQ = 100.0;
    public static final double MILESTONE_REWARD = 1.0;
    public static final double HEURISTIC_REWARD = 1.0;

    private List<QState> qTable; // Q Table (state, action)
    // state: combination of col heights and starting piece
    // action: series of steps to directly place piece

    // memory specs
    private Queue<Memory> memory;
    public static final int REPLAY_SIZE = 2000;
    public static final double MAX_MEMORY_SIZE = 20000;

    // hyperparams for training
    private double alpha = 0.02;
    private double gamma = 0.9;
    private double epsilon = 2.0; // make first 10k games all agent to build memory
    private static final double MIN_EPSILON = 0.0;
    private static final double EPSILON_ANNEAL_RATE = 1 / 10000.0; // linear anneal over 10k games
    private double epsilonAgent = 2.0; // of exploration, % of time using heuristic agent vs fully random
    private static final double MIN_AGENT = 0.0; // keep @ all agent (random is bad)
    private static final double AGENT_ANNEAL_RATE = 1 / 10000.0; // linear anneal over 10k games

    // board and state specs
    private int boardWidth;
    private int boardHeight;
    private int stateWidth;
    private int stateHeight;
    private int stateWidthShrink;
    private int stateHeightShrink;
    private int topSpace;

    /**
     * Initializes all states for given input
     * @param boardWidthIn width of board
     * @param boardHeightIn height of board
     * @param stateWidthShrinkIn pooling factor for shrinking board width into states
     * @param stateHeightShrinkIn pooling factor for shrinking board height into states
     * @param boardTopSpace amount of top space board has (default 4)
     */
    public QTable(int boardWidthIn, int boardHeightIn, int stateWidthShrinkIn, int stateHeightShrinkIn, int boardTopSpace) {
        boardWidth = boardWidthIn;
        boardHeight = boardHeightIn;
        topSpace = boardTopSpace;
        stateWidthShrink = stateWidthShrinkIn;
        stateHeightShrink = stateHeightShrinkIn;
        stateWidth = boardWidth / stateWidthShrink;
        stateHeight = (boardHeight+1) / stateHeightShrink;
        qTable = new ArrayList<>();
        memory = new ArrayDeque<>();
        for(int i = 0; i < Math.pow(stateHeight,stateWidth); i++)
            for(int pType = 0; pType < NUM_PIECES; pType++)
                qTable.add(new QState(NUM_PIECES * i + pType,boardWidth,boardHeight,stateWidthShrink,stateHeightShrink,topSpace));
    }

    /**
     * Simulates one training game with epsilon-greedy policy for actions.
     * Records events in dynamic memory
     * @return score: number of placed pieces it survived for
     */
    public int trainOneGame() {
        TetrisBoard b = new TetrisBoard(boardWidth, boardHeight + topSpace);
        QState s = addNextPiece(b);

        double prevScore = s.heuristicReward(b);
        int counter = 0;
        while(true) {
            QAction a;
            double e = Math.random();
            if(e < epsilon) { // explore
                double e2 = Math.random();
                if(e2 < epsilonAgent)
                    a = s.getHeuristicAction(b);
                else
                    a = s.getRandomAction();
            }
            else // exploit
                a = s.getMaxAction(b);
            a.applyMoves(b);
            counter++;

            // for testing only, shouldn't happen since we always end w/ drop (checks if QAction is valid full move)
            if(b.getLastResult() != Board.Result.PLACE) {
                System.err.println("Adding additional drop");
                b.move(Board.Action.DROP);
            }

            // calculate rewards
            double newScore = s.heuristicReward(b);
            double r = HEURISTIC_REWARD * (newScore - prevScore) + ROWS_CLEAR_REWARD * (b.getRowsCleared() * b.getRowsCleared()) * b.getWidth() + DROP_REWARD;
            prevScore = newScore;
            if(counter % MILESTONE_REWARD_FREQ == 0)
                r+=MILESTONE_REWARD*(counter/MILESTONE_REWARD_FREQ);

            // get next state and add to memory
            if(b.getMaxHeight() > boardHeight) { // game over
                addMemory(null, a, r);
                break;
            }

            QState sPrime = addNextPiece(b);
            QAction aPrime = sPrime.getMaxAction(b);
            addMemory(aPrime, a, r);
            s = sPrime;
        }
        return counter;
    }

    /**
     * Iterate over memory EPOCH times and use temporal difference (TD) learning function to update Q-values
     * @param epochs number of iterations to train over memory for
     */
    public void trainTable(int epochs) {
        if(memory.size() < REPLAY_SIZE) return;

        for(int e = 0; e < epochs; e++) {
            for(Memory m : memory) {
                QAction aPrime = m.getNextAction();
                QAction a = m.getAction();
                double r = m.getReward();

                // TD learning
                if(aPrime == null)
                    a.addValue(alpha * (r - a.getValue()));
                else {
                    a.addValue(alpha * (r + gamma * aPrime.getValue() - a.getValue()));
                }
            }
        }
        // update epsilon
        epsilon = Math.max(MIN_EPSILON, epsilon - EPSILON_ANNEAL_RATE);
        epsilonAgent = Math.max(MIN_AGENT, epsilonAgent - AGENT_ANNEAL_RATE);
    }

    /**
     * Simulates one testing game with choosing actions w/ max Q values
     * @return score: number of placed pieces it survived for
     */
    public int testOneGame() {
        TetrisBoard b = new TetrisBoard(boardWidth, boardHeight + topSpace);
        QState s = addNextPiece(b);

        int counter = 0;
        while(true) {
            QAction a = s.getMaxAction(b);
            boolean validMoves = a.applyMovesTestValid(b);
            if(!validMoves)
                return -1; // if invalid move end game (assert will test)

            counter++;

            if(b.getMaxHeight() > boardHeight) { // game over
                break;
            }

            QState sPrime = addNextPiece(b);
            s = sPrime;
        }
        return counter;
    }

    /**
     * Gets a sequence of actions to take with maximum Q values given input board
     */
    public List<Board.Action> runTable(Board b) {
        if(b.getCurrentPiece() == null)
            return Arrays.asList(Board.Action.NOTHING);
        if(!b.getCurrentPiecePosition().equals(new Point(b.getWidth() / 2 - b.getCurrentPiece().getWidth() / 2, boardHeight)))
            return Arrays.asList(Board.Action.DROP); // if piece not in spawn position, QTable invalid so just drop

        QState s = encode(b);
        QAction a = s.getMaxAction(b);
        return a.getMoves();
    }

    /**
     * Create memory object and add it to dynamic memory queue.
     * If memory is too large it removes first seen value
     * @param aPrime best action at next state
     * @param a action at current state
     * @param r reward for the action
     */
    private void addMemory(QAction aPrime, QAction a, double r) {
        memory.add(new Memory(aPrime, a, r));
        if(memory.size() > MAX_MEMORY_SIZE)
            memory.poll();
    }

    /**
     * Testing method for checking if memory stays in specified bounds
     */
    public boolean checkMemoryInBounds() { return memory.size() <= MAX_MEMORY_SIZE; }

    /**
     * Add random piece to board and return new state
     */
    private QState addNextPiece(Board board) {
        Piece nextPiece = PIECES[(int) (Math.random() * NUM_PIECES)];
        board.nextPiece(nextPiece, new Point(board.getWidth() / 2 - nextPiece.getWidth() / 2, boardHeight));
        return encode(board);
    }

    /**
     * Encode the input board into a integer which is associated with a state.
     * Uses board column heights that are shrunk down on height and pooled over width.
     * Encodes pooled heights into using base conversions and scale by pFactor
     * @param b input board
     * @return QState associated with that encoded integer
     */
    public QState encode(Board b) {
        int pFactor = getPieceTypeEncoding(b);
        
        String s = "";
        for(int x = 0; x < boardWidth; x+=stateWidthShrink) {
            int maxCol = 0;
            for(int i = x; i < boardWidth && i < x + stateWidthShrink; i++)
                maxCol = Math.max(maxCol, b.getColumnHeight(i));
            s+=Integer.toString(maxCol / stateHeightShrink, stateHeight); // convert bases
        }
        
        int idx = NUM_PIECES * Integer.parseInt(s, stateHeight) + pFactor;
        return qTable.get(idx);
    }

    /**
     * Convert the current piece into a value from [0,7) for encoding scaling
     */
    private int getPieceTypeEncoding(Board b) {
        PieceType p = b.getCurrentPiece().getType();
        switch(p) {
            case STICK: return 0;
            case SQUARE: return 1;
            case T: return 2;
            case LEFT_L: return 3;
            case RIGHT_L: return 4;
            case LEFT_DOG: return 5;
            case RIGHT_DOG: return 6;
            default: // never will happen
                System.err.println("Invalid piecetype");
                return -1;
        }
    }

    /**
     * Read Q value weights file and update Q Table values with these new weights 
     */
    public void loadTable(String filePath) {
        try (Scanner scanner = new Scanner(new File(filePath))) {
            for(int i = 0; i < Math.pow(stateHeight,stateWidth); i++) {
                for(int pType = 0; pType < NUM_PIECES; pType++) {
                    String line = scanner.nextLine();
                    qTable.get(i*NUM_PIECES + pType).loadLine(line);
                }
            }
            if(scanner.hasNextLine()) {
                System.out.println(scanner.nextLine());
                throw new NoSuchElementException();
            }
        } catch (NoSuchElementException e) {
            System.err.println("Loaded file doesn't match dimensions of board, not applicable, using default values");
            qTable = new ArrayList<>();
        } catch (FileNotFoundException e) {
            System.err.println("Loaded file: "+filePath+" not found, using default values");
        }
    }
    
    /**
     * Write Q values into a file for weights storage and future loading
     */
    public void saveTable(String filePath) {
        try (FileWriter writer = new FileWriter(filePath)) {
            for(QState s : qTable)
                writer.write(s.asString());
        } catch (Exception e) {
            System.err.println(e);
        }
    }
}

/**
 * Object containing representing the encoded state of a board and containing the possible actions at this board
 */
class QState {
    public static final int MAX_VALUES = 40;

    private List<QAction> actions;
    private TetrisBoard baseBoard;
    private int boardWidth;
    private int boardHeight;
    private int stateWidth;
    private int stateHeight;
    private int stateWidthShrink;
    private int stateHeightShrink;
    private int topSpace;
    private int idx;

    /**
     * Same as QTable initialization but iterates and creates all possible actions (instead of states)
     */
    public QState(int encoding, int boardWidthIn, int boardHeightIn, int stateWidthShrinkIn, int stateHeightShrinkIn, int boardTopSpace) { 
        boardWidth = boardWidthIn;
        boardHeight = boardHeightIn;
        stateWidthShrink = stateWidthShrinkIn;
        stateHeightShrink = stateHeightShrinkIn;
        stateWidth = boardWidth / stateWidthShrink;
        stateHeight = (boardHeight+1) / stateHeightShrink;
        topSpace = boardTopSpace;
        idx = encoding;
        baseBoard = decode(encoding);
        actions = new ArrayList<>();
        enumerateActions();
    }

    // for testing
    public int getIdx() { return idx; }

    /**
     * Decode from integer to a board using scaling and base conversions. Reverses encoding
     */
    public TetrisBoard decode(int encoding) {
        Piece nextPiece = QTable.PIECES[encoding % QTable.NUM_PIECES];
        encoding/=QTable.NUM_PIECES;

        Piece[][] grid = new Piece[boardHeight+topSpace][boardWidth];
        int[] colHeights = toBase(encoding,stateHeight);
        
        for(int x = 0; x < boardWidth; x+=stateWidthShrink) {
            for(int i = x; i < boardWidth && i < x + stateWidthShrink; i++)
                for(int y = 0; y < stateHeightShrink * colHeights[x/stateWidthShrink]; y++)
                    grid[y][i] = QTable.dummyPiece;
        }

        TetrisBoard board = new TetrisBoard(grid);
        board.nextPiece(nextPiece, new Point(board.getWidth() / 2 - nextPiece.getWidth() / 2, boardHeight));
        return board;
    }

    /**
     * Decodes encoding into array of column heights to fill into board using base conversions
     */
    private int[] toBase(int encoding, int h) {
        String s = Integer.toString(encoding, h);

        int[] out = new int[stateWidth];
        int i;
        for(i = 0; i < stateWidth - s.length(); i++)
            out[i] = 0;

        for(int j = 0; j < s.length(); j++)
            out[i+j] = Integer.parseInt(s.substring(j, j+1), h);

        return out;

    }

    /**
     * Iterate over all possible moves at this state.
     * Each move is a set of actions that results in one piece being placed.
     * Similar to LameBrain's enumerateActions but with rotations
     */
    private void enumerateActions() {
        for(int rotation = 0; rotation < 4; rotation++) {
            Board testBoard;
            List<Board.Action> moves = new ArrayList<>();
            switch(rotation) {
                case 1:
                    testBoard = baseBoard.testMove(Board.Action.CLOCKWISE);
                    moves.add(Board.Action.CLOCKWISE);
                    break;
                case 2:
                    testBoard = baseBoard.testMove(Board.Action.CLOCKWISE);
                    testBoard.move(Board.Action.CLOCKWISE);
                    moves.add(Board.Action.CLOCKWISE);
                    moves.add(Board.Action.CLOCKWISE);
                    break;
                case 3:
                    testBoard = baseBoard.testMove(Board.Action.COUNTERCLOCKWISE);
                    moves.add(Board.Action.COUNTERCLOCKWISE);
                    break;
                default:
                    testBoard = baseBoard.testMove(Board.Action.NOTHING);
            }

            addAction(moves);

            List<Board.Action> leftMoves = new ArrayList<>(moves);
            Board left = testBoard.testMove(Board.Action.LEFT);
            while (left.getLastResult() == Board.Result.SUCCESS) {
                leftMoves.add(Board.Action.LEFT);
                addAction(leftMoves);
                left.move(Board.Action.LEFT);
            }

            List<Board.Action> rightMoves = new ArrayList<>(moves);
            Board right = testBoard.testMove(Board.Action.RIGHT);
            while (right.getLastResult() == Board.Result.SUCCESS) {
                rightMoves.add(Board.Action.RIGHT);
                addAction(rightMoves);
                right.move(Board.Action.RIGHT);
            }

        }
    }

    /**
     * Drops piece and adds to QAction space
     * @param moves list of actions taken up until then (without DROP for placing the piece)
     */
    private void addAction(List<Board.Action> moves) {
        List<Board.Action> copyMoves = new ArrayList<>(moves);
        copyMoves.add(Board.Action.DROP);
        actions.add(new QAction(copyMoves));
    }

    /**
     * Get the best move according to the QTable
     */
    public QAction getMaxAction(Board b) {
        if(b == null) {
            return Collections.max(actions);
        }
        Collections.sort(actions, Collections.reverseOrder());
        List<QAction> options = actions.stream().limit(MAX_VALUES).collect(Collectors.toList());
        return calcHeuristicAction(b, options);
    }

    /**
     * Pick random QAction from action space
     */
    public QAction getRandomAction() {
        return actions.get((int) (Math.random() * actions.size()));
    }

    /**
     * Get the best move according to the heuristic 
     * @param b current board 
     * @return next QAction to take
     */
    public QAction getHeuristicAction(Board b) {
        return calcHeuristicAction(b, actions);
    }

    /**
     * Compare difference in board evaluation after each possible action.
     * Choose action with best improvement in board state
     */
    public QAction calcHeuristicAction(Board b, List<QAction> actions) {
        double r = heuristicReward(b);
        double rMax = 0;
        QAction aMax = null;
        for(QAction a : actions) {
            TetrisBoard testBoard = new TetrisBoard((TetrisBoard) b);
            a.applyMoves(testBoard);
            double rCurr = heuristicReward(testBoard) - r;
            if(aMax == null || rCurr > rMax) {
                rMax = rCurr;
                aMax = a;
            }
        }
        return aMax;
    }

    /**
     * Use heuristic to evaluate how good a board is.
     * Referenced online Tetris scoring techniques for base metric ideas.
     * Customized weights and metrics for own implementation using trial and error (no GA).
     */
    public double heuristicReward(Board b) {
        double aggregateHeightWeight = -2.0;
        double completeLineWeight = 3.0;
        double holesWeight = -1.5;
        double bumpinessWeight = -0.75;
        if(b.getMaxHeight() > (3 * b.getHeight()) / 4) { // panic mode
            aggregateHeightWeight*=2;
            completeLineWeight*=2;
        }
        // doesn't need to be perfect, just an estimate for how good board is doing so it gets some sense of whats going on
        // also allows for longer games which means more diverse experience replay
        return aggregateHeightWeight * aggregateHeight(b) + completeLineWeight * completeLine(b) + holesWeight * holes(b) + bumpinessWeight * bumpiness(b);
    }

    /**
     * Find total height of all columns (adds back cleared rows for consistency in row clears)
     */
    private int aggregateHeight(Board b) {
        int sum = 0;
        for(int x = 0; x < b.getWidth(); x++)
            sum+=b.getColumnHeight(x);
        return sum + b.getRowsCleared() * b.getWidth();
    }

    /**
     * Find number of rows cleared with the previous move
     */
    private int completeLine(Board b) {
        return b.getRowsCleared();
    }

    /**
     * Find number of holes in board
     */
    private int holes(Board b) {
        int count = 0;
        for(int x = 0; x < b.getWidth(); x++)
            for(int y = 0; y < b.getColumnHeight(x); y++)
                if(b.getGrid(x, y) == null)
                    count++;
        return count;
    }

    /**
     * Find bumpiness in board (absolute difference in consecutive column heights) 
     */
    private int bumpiness(Board b) {
        int sum = 0;
        for(int x = 0; x < b.getWidth()-1; x++)
            sum+=Math.abs(b.getColumnHeight(x) - b.getColumnHeight(x+1)); // or squared
        return sum;
    }

    /**
     * Load a line containing Q values for each action for this state seperated by whitespace
     */
    public void loadLine(String line) {
        String[] values = line.trim().split("\\s+");
        if(values.length != actions.size()) {
            System.err.println("Unequal action spaces during loading");
            return;
        }
        for(int i = 0; i < values.length; i++)
            actions.get(i).setValue(Double.parseDouble(values[i]));
    }
    
    /**
     * Convert state to string of action values (for saving weights)
     */
    public String asString() {
        String s = "";
        for(int i = 0; i < actions.size(); i++)
            s+=actions.get(i).getValue()+" ";
        return s+"\n";
    }
}

/**
 * Store series of steps to place piece and Q value for that action
 */
class QAction implements Comparable<QAction> {
    private List<Board.Action> moves;
    private double qValue;

    public QAction(List<Board.Action> movesIn) {
        moves = movesIn;
        qValue = Math.random(); // could also initalize to 0 (did it for testing to see what states were reached)
    }

    /**
     * Run sequence of moves of this QAction on a board
     */
    public void applyMoves(Board b) {
        for(int i = 0; i < moves.size(); i++)
            b.move(moves.get(i));
    }

    /**
     * apply moves and also test if each moves is valid (for testing) 
     */
    public boolean applyMovesTestValid(TetrisBoard b) {
        if(!b.currentPieceValid())
            return false;

        for(int i = 0; i < moves.size(); i++) {
            b.move(moves.get(i));
            if(!b.currentPieceValid() || b.getLastResult() == Board.Result.OUT_BOUNDS)
                return false;
        }
        return true;
    }

    public List<Board.Action> getMoves() { return moves; }
    public double getValue() { return qValue; }
    public void setValue(double x) { qValue = x; }
    public void addValue(double x) { qValue+=x; }

    @Override
    public int compareTo(QAction q) {
        double d = (getValue() - q.getValue());
        if(d > 0) return 1;
        else if(d == 0) return 0;
        return -1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QAction q = (QAction) o;
        return getValue() == q.getValue();
    }

}