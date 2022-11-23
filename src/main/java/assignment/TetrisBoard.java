package assignment;

import java.awt.*;
import java.util.Arrays;

import assignment.Piece.PieceType;

/**
 * Represents a Tetris board -- essentially a 2D grid of piece types (or nulls). Supports
 * tetris pieces and row clearing.  Does not do any drawing or have any idea of
 * pixels. Instead, just represents the abstract 2D board.
 */
public final class TetrisBoard implements Board {
    private int boardWidth;
    private int boardHeight;

    private Piece[][] grid; // store x,y from ground up
    private int[] rowWidth;
    private int[] colHeight;
    private int maxHeight;

    private Piece currentPiece;
    private Point currentPiecePosition;

    private Result lastResult;
    private Action lastAction;
    private int rowsCleared;

    // JTetris will use this constructor
    public TetrisBoard(int width, int height) {
        // set negative dimensions to 0, creating a board that no piece can be added to
        boardWidth = Math.max(0,width);
        boardHeight = Math.max(0,height);

        grid = new Piece[boardHeight][boardWidth];
        rowWidth = new int[boardHeight];
        colHeight = new int[boardWidth];
        maxHeight = 0;

        currentPiece = null;
        currentPiecePosition = null;

        lastResult = Result.NO_PIECE;
        lastAction = Action.NOTHING;
        rowsCleared = 0;
    }

    /**
     * Copy constructor of Tetrisboard
     * @param b board to copy
     */
    public TetrisBoard(TetrisBoard b) {
        boardWidth = b.boardWidth;
        boardHeight = b.boardHeight;

        // deep copy grid
        grid = new Piece[boardHeight][boardWidth];
        for(int y = 0; y < boardHeight; y++) {
            for(int x = 0; x < boardWidth; x++) {
                if(b.grid[y][x] == null)
                    grid[y][x] = null;
                else
                    grid[y][x] = new TetrisPiece((TetrisPiece) b.grid[y][x]); // assume grid[y][x] is a TetrisPiece
            }
        }

        // shallow copy since primitives
        rowWidth = Arrays.copyOf(b.rowWidth, boardHeight);
        colHeight = Arrays.copyOf(b.colHeight, boardWidth);
        maxHeight = b.maxHeight;

        if(b.currentPiece == null)
            currentPiece = null;
        else
            currentPiece = new TetrisPiece((TetrisPiece) b.currentPiece); // assume currentPiece is a TetrisPiece
        
        if(b.currentPiecePosition == null)
            currentPiecePosition = null;
        else
            currentPiecePosition = new Point(b.currentPiecePosition);

        lastResult = b.lastResult;
        lastAction = b.lastAction;
        rowsCleared = b.rowsCleared;
    }

    /**
     * Testing constructor, preloads grid
     */
    public TetrisBoard(Piece[][] p) {
        boardWidth = p[0].length;
        boardHeight = p.length;

        grid = p;
        rowWidth = new int[boardHeight];
        colHeight = new int[boardWidth];
        maxHeight = 0;

        // update rowWidth, colHeight and maxHeight
        for(int y = 0; y < boardHeight; y++)
            for(int x = 0; x < boardWidth; x++)
                if(getGrid(x, y) != null)
                    rowWidth[y]++;

        for(int x = 0; x < boardWidth; x++) {
            for(int y = boardHeight-1; y >= 0; y--) {
                if(getGrid(x, y) != null) {
                    colHeight[x] = y+1;
                    maxHeight = Math.max(maxHeight, colHeight[x]);
                    break;
                }
            }
        }

        currentPiece = null;
        currentPiecePosition = null;

        lastResult = Result.NO_PIECE;
        lastAction = Action.NOTHING;
        rowsCleared = 0;
    }

    @Override
    public Result move(Action act) { 
        lastAction = act;
        lastResult = runMove(act);
        return lastResult;
    }

    @Override
    public Board testMove(Action act) {
        Board testBoard = new TetrisBoard(this);
        testBoard.move(act);
        return testBoard;
    }

    @Override
    public Piece getCurrentPiece() { return currentPiece; }

    @Override
    public Point getCurrentPiecePosition() { return currentPiecePosition; }

    @Override
    public void nextPiece(Piece p, Point spawnPosition) {
        currentPiece = p;
        currentPiecePosition = spawnPosition;

        if(!currentPieceValid()) {
            resetCurrentPiece(); // revert back to null (it should have been null beforehand)
            throw new IllegalArgumentException();
        }
    }

    @Override
    public boolean equals(Object other) { 
        if(!(other instanceof TetrisBoard)) return false;
        TetrisBoard otherBoard = (TetrisBoard) other;
        
        for(int x = 0; x < boardWidth; x++)
            for(int y = 0; y < boardHeight; y++) {
                Piece p1 = getGridPiece(x, y);
                Piece p2 = otherBoard.getGridPiece(x, y);
                if(p1 == null && p2 == null)
                    continue;
                if(p1 == null || !p1.equals(p2))
                    return false;
            }

        // assume if current piece is null then the piece position is also null (they are linked)
        if(getCurrentPiece() == null)
            return otherBoard.getCurrentPiece() == null;
        
        return getCurrentPiece().equals(otherBoard.getCurrentPiece()) && getCurrentPiecePosition().equals(otherBoard.getCurrentPiecePosition());
    }

    @Override
    public Result getLastResult() { return lastResult; }

    @Override
    public Action getLastAction() { return lastAction; }

    @Override
    public int getRowsCleared() { return rowsCleared; }

    @Override
    public int getWidth() { return boardWidth; }

    @Override
    public int getHeight() { return boardHeight; }

    @Override
    public int getMaxHeight() { return maxHeight; }

    @Override
    public int dropHeight(Piece piece, int x) { // drop piece w/ lower left bound @ x and get the y value of the lower-left bound when it stops
        int[] skirt = piece.getSkirt();

        int yMax = -piece.getHeight();
        for(int i = 0; i < piece.getWidth(); i++)
            yMax = Math.max(yMax, getColumnHeight(x+i) - skirt[i]);
        return yMax;
    }

    @Override
    public int getColumnHeight(int x) {
        if(outOfBounds(x, 0)) // assume y = 0 is valid
            return 0;
        return colHeight[x];
    }

    @Override
    public int getRowWidth(int y) {
        if(outOfBounds(0, y)) // assume x = 0 is valid
            return 0;
        return rowWidth[y];
    }

    @Override
    public Piece.PieceType getGrid(int x, int y) {
        Piece p = getGridPiece(x, y);
        if(p == null) return null;
        return p.getType();
    }
    
    /**
     * Gets the Piece at (x,y)
     * @param x x-coord (0-index)
     * @param y y-coord (0-index)
     * @return Piece at (x,y)
     */
    public Piece getGridPiece(int x, int y) {
        if(outOfBounds(x, y) || grid[y][x] == null)
            return null;
        return grid[y][x]; // grid[x][y] gives (row,col) so grid[y][x] gives (col,row)
    }

    /**
     * Check if (x,y) is out of grid bounds
     * @param x x-coord (0-index)
     * @param y y-coord (0-index)
     * @return true if out of bounds, false if in bounds
     */
    private boolean outOfBounds(int x, int y) {
        return x < 0 || x >= getWidth() || y < 0 || y >= getHeight();
    }

    /**
     * Checks if all points in current piece can be placed on board, if not then piece is invalid spot   
     * @return true if valid, false if invalid
     */
    public boolean currentPieceValid() {
        if(currentPiece == null)
            return true;

        Point[] pieceBody = currentPiece.getBody();

        int x;
        int y;
        for(Point point : pieceBody) {
            x = (int) (currentPiecePosition.getX() + point.getX());
            y = (int) (currentPiecePosition.getY() + point.getY());
            if(outOfBounds(x,y) || grid[y][x] != null)
                return false;
        }
        return true;
    }

    /**
     * Resets current piece and position to default state
     */
    private void resetCurrentPiece() {
        currentPiece = null;
        currentPiecePosition = null;
    }

    /**
     * Applies action to currentPiece, updates grid if piece is placed
     * @param act input action
     * @return result from applying that action
     */
    private Result runMove(Action act) {
        if(currentPiece == null) return Result.NO_PIECE;
        if(!currentPieceValid()) return Result.OUT_BOUNDS;

        switch(act) {
            case LEFT:
                return runLeftMove();
            case RIGHT:
                return runRightMove();
            case DOWN:
                return runDownMove();
            case DROP:
                return runDropMove();
            case CLOCKWISE:
                return runClockwiseMove();
            case COUNTERCLOCKWISE:
                return runCounterClockwiseMove();
            default: // HOLD and NOTHING return SUCCESS
                return Result.SUCCESS;
        }
    }

    /**
     * Moves current piece left 1 if possible, otherwise doesn't move and returns OB
     * @return result from left move
     */
    private Result runLeftMove() {
        double currentStartX = currentPiecePosition.getX();
        double currentStartY = currentPiecePosition.getY();
        
        currentPiecePosition.setLocation(currentStartX-1, currentStartY);
        if(!currentPieceValid()) { // if invalid shift back
            currentPiecePosition.setLocation(currentStartX, currentStartY);
            return Result.OUT_BOUNDS;
        }
        else
            return Result.SUCCESS;
    }

    /**
     * Moves current piece right 1 if possible, otherwise doesn't move and returns OB
     * @return result from right move
     */
    private Result runRightMove() { 
        double currentStartX = currentPiecePosition.getX();
        double currentStartY = currentPiecePosition.getY();

        currentPiecePosition.setLocation(currentStartX+1, currentStartY);
        if(!currentPieceValid()) { // if invalid shift back
            currentPiecePosition.setLocation(currentStartX, currentStartY);
            return Result.OUT_BOUNDS;
        }
        else
            return Result.SUCCESS;
    }

    /**
     * Moves current piece down 1 if possible, otherwise doesn't move and places piece
     * @return result from down move
     */
    private Result runDownMove() {
        double currentStartX = currentPiecePosition.getX();
        double currentStartY = currentPiecePosition.getY();

        currentPiecePosition.setLocation(currentStartX, currentStartY-1);
        if(!currentPieceValid()) { // if invalid shift back
            currentPiecePosition.setLocation(currentStartX, currentStartY);
            placePiece();
            return Result.PLACE;
        }
        return Result.SUCCESS;
    }

    /**
     * Drops current piece, uses dropHeight if the piece is above all placed pieces in the columns its currently in.
     * Otherwise moves it down until it can't anymore
     * @return result from drop move
     */
    private Result runDropMove() {
        double currentStartX = currentPiecePosition.getX();
        double currentStartY = currentPiecePosition.getY();

        // using dropHeight faster than moving down a bunch of times
        if(currentPieceAboveAll()) {
            double currentEndY = dropHeight(currentPiece, (int) currentStartX);
            currentPiecePosition.setLocation(currentStartX, currentEndY);
            placePiece();
            return Result.PLACE;
        }

        while(currentPieceValid()) {
            currentStartX = currentPiecePosition.getX();
            currentStartY = currentPiecePosition.getY();
            currentPiecePosition.setLocation(currentStartX, currentStartY-1);
        }
        currentPiecePosition.setLocation(currentStartX, currentStartY); // shift back up 1 since invalid
        placePiece();
        return Result.PLACE;
    }

    /**
     * Checks if current piece is above all placed pieces in the columns its currently in
     */
    private boolean currentPieceAboveAll() { 
        int x = (int) currentPiecePosition.getX();
        int y = (int) currentPiecePosition.getY();
        
        for(int i = 0; i < currentPiece.getWidth(); i++)
            if(y <= getColumnHeight(x+i))
                return false;
        return true;
    }

    /**
     * Rotates current piece clockwise 90 degreees.
     * Iterates over the 5 wall kick tests until first one is a valid piece
     * If all invalid, reverts rotation and position back to before move
     * @return result from clockwise move
     */
    private Result runClockwiseMove() { 
        double currentStartX = currentPiecePosition.getX();
        double currentStartY = currentPiecePosition.getY();

        Piece tempPiece = currentPiece;
        Point[] wallKickTests = getWallKickTests(currentPiece, true);
        currentPiece = currentPiece.clockwisePiece();

        for(int test = 0; test < wallKickTests.length; test++) {
            currentPiecePosition.setLocation(currentStartX+wallKickTests[test].getX(), currentStartY+wallKickTests[test].getY());
            if(currentPieceValid())
                return Result.SUCCESS;
        }
        
        currentPiece = tempPiece;
        currentPiecePosition.setLocation(currentStartX, currentStartY);
        return Result.OUT_BOUNDS;
    }

    /**
     * Rotates current piece counterclockwise 90 degreees.
     * Iterates over the 5 wall kick tests until first one is a valid piece
     * If all invalid, reverts rotation and position back to before move
     * @return result from counterclockwise move
     */
    private Result runCounterClockwiseMove() { 
        double currentStartX = currentPiecePosition.getX();
        double currentStartY = currentPiecePosition.getY();

        Piece tempPiece = currentPiece;
        Point[] wallKickTests = getWallKickTests(currentPiece, false);
        currentPiece = currentPiece.counterclockwisePiece();

        for(int test = 0; test < wallKickTests.length; test++) {
            currentPiecePosition.setLocation(currentStartX+wallKickTests[test].getX(), currentStartY+wallKickTests[test].getY());
            if(currentPieceValid())
                return Result.SUCCESS;
        }
        
        currentPiece = tempPiece;
        currentPiecePosition.setLocation(currentStartX, currentStartY);
        return Result.OUT_BOUNDS;
    }

    /**
     * Series of methods to place the current piece on grid and update states
     */
    private void placePiece() {
        updateGrid();
        clearRows();
        updateColHeights();
        resetCurrentPiece();
    }

    /**
     * Adds body of current piece to grid, assumes currentPiece is valid (must be from the way it can be called).
     * Also updates rowWidth correspondingly.
     */
    private void updateGrid() {
        Point[] pieceBody = currentPiece.getBody();

        int x;
        int y;
        for(Point point : pieceBody) {
            x = (int) (currentPiecePosition.getX() + point.getX());
            y = (int) (currentPiecePosition.getY() + point.getY());
            grid[y][x] = currentPiece;
            rowWidth[y]++;
        }
    }

    /**
     * Clears any full rows from top down. Shifts higher rows down, updates rowWidth and rowCleared
     */
    private void clearRows() {
        rowsCleared = 0;
        for(int y = getHeight() - 1; y >= 0; y--) {
            if(rowWidth[y] == getWidth()) {
                for(int yAbove = y; yAbove < getHeight()-1; yAbove++) {
                    for(int tempX = 0; tempX < getWidth(); tempX++) {
                        grid[yAbove][tempX] = grid[yAbove+1][tempX];
                        rowWidth[yAbove] = rowWidth[yAbove+1];
                    }
                }
                for(int tempX = 0; tempX < getWidth(); tempX++) {
                    grid[getHeight()-1][tempX] = null;
                    rowWidth[getHeight()-1] = 0;
                }
                
                rowsCleared++;
            }
        }
    }

    /**
     * Updates colHeights and maxHeight
     */
    private void updateColHeights() {
        maxHeight = 0;
        for(int x = 0; x < getWidth(); x++) {
            colHeight[x] = 0;
            for(int y = getHeight()-1; y >= 0; y--) {
                if(getGridPiece(x, y) != null) {
                    colHeight[x] = y+1;
                    break;
                }
            }
            maxHeight = Math.max(maxHeight, colHeight[x]);
        }
    }

    /**
     * Queries all bounding box shifts in the order of their wall kick tests depending on the piece and if clockwise/counterclockwise
     * @param p input piece
     * @param isClockwise true if clockwise, false if counterclockwise
     * @return array of points (x,y) that represent the shift in bounding box for each Tetris wall kick test
     */
    private Point[] getWallKickTests(Piece p, boolean isClockwise) {
        if(p.getType() == PieceType.SQUARE)
            return new Point[] { new Point(0, 0) };
        if(p.getType() == PieceType.STICK) {
            if(isClockwise)
                return Piece.I_CLOCKWISE_WALL_KICKS[p.getRotationIndex()];
            return Piece.I_COUNTERCLOCKWISE_WALL_KICKS[p.getRotationIndex()];
        }

        if(isClockwise)
            return Piece.NORMAL_CLOCKWISE_WALL_KICKS[p.getRotationIndex()];
        return Piece.NORMAL_COUNTERCLOCKWISE_WALL_KICKS[p.getRotationIndex()];
    }

    // only returns string grid of board (not top area), for testing
    public String toString() {
        String s = "";
        for(int y = boardHeight - 1; y >= 0; y--) {
            for(int x = 0; x < boardWidth; x++)
                s+=grid[y][x]+" ";
            s+="\n";
        }
        return s;
    }

}