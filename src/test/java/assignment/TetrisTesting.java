package assignment;

import static org.junit.Assert.assertTrue;

import java.awt.Point;

import org.junit.Test;

public class TetrisTesting {
    private static int WIDTH = 10;
    private static int HEIGHT = 20;

    @Test
    public void rowClearTest() { //blackbox
        Piece[][] p = new Piece[HEIGHT][WIDTH];
        Piece currentPiece = new TetrisPiece(Piece.PieceType.STICK, 3);

        for(int y = 0; y < 4; y++) {
            for(int x = 0; x < WIDTH; x++) {
                if(x != WIDTH/2) {
                    Piece temp = new TetrisPiece(Piece.PieceType.SQUARE);
                    p[y][x] = temp;
                }
            }
        }
        
        TetrisBoard env = new TetrisBoard(p);
        TetrisBoard expected = new TetrisBoard(WIDTH, HEIGHT);
        env.nextPiece(currentPiece, new Point((WIDTH-1)/2, HEIGHT - 4));
        env.move(Board.Action.DROP);
        assertTrue(env.equals(expected));
    }

    @Test
    public void moveTest() { //whitebox
        //compare if currentlocation is +1 if right for instance
        Piece[][] p = new Piece[HEIGHT][WIDTH];
        TetrisBoard env = new TetrisBoard(p);

        Piece currentPiece = null;
        for(int h = 0; h < 5; h++) {
            TetrisBoard copy = new TetrisBoard(env);
            for(int i = 0; i < 7; i++) {
                if(i == 0)
                    currentPiece = new TetrisPiece(Piece.PieceType.T, 2);
                if(i == 1)
                    currentPiece = new TetrisPiece(Piece.PieceType.SQUARE);
                if(i == 2)
                    currentPiece = new TetrisPiece(Piece.PieceType.STICK, 3);
                if(i == 3)
                    currentPiece = new TetrisPiece(Piece.PieceType.LEFT_L);
                if(i == 4)
                    currentPiece = new TetrisPiece(Piece.PieceType.RIGHT_L);
                if(i == 5)
                    currentPiece = new TetrisPiece(Piece.PieceType.LEFT_DOG);
                if(i == 6)
                    currentPiece = new TetrisPiece(Piece.PieceType.RIGHT_DOG);
            }

            //valid
            copy.nextPiece(currentPiece, new Point((WIDTH-1)/2, HEIGHT - 4));
            Point initial = new Point(copy.getCurrentPiecePosition());
            if(h == 0) {
                copy.move(Board.Action.RIGHT);
                Point after = copy.getCurrentPiecePosition();
                assertTrue(initial.x + 1 == after.x && initial.y == after.y);
            }
            if(h == 1) {
                copy.move(Board.Action.LEFT);
                Point after = copy.getCurrentPiecePosition();
                assertTrue(initial.x - 1 == after.x && initial.y == after.y);
            }
            if(h == 2) {
                copy.move(Board.Action.DOWN);
                Point after = copy.getCurrentPiecePosition();
                assertTrue(initial.x == after.x && initial.y - 1 == after.y);
            }
            //assume no wall kicks for rotations
            int rotationIndex = copy.getCurrentPiece().getRotationIndex();
            if(h == 3) {
                copy.move(Board.Action.CLOCKWISE);
                assertTrue((rotationIndex + 1) % 4 == copy.getCurrentPiece().getRotationIndex());
            }
            if(h == 4) {
                copy.move(Board.Action.COUNTERCLOCKWISE);
                assertTrue((rotationIndex + 3) % 4 == copy.getCurrentPiece().getRotationIndex());
            }

            //invalid
            boolean wrongException = true;
            try {
                copy.nextPiece(currentPiece, new Point(WIDTH, HEIGHT));
            }
            catch(Exception e){
                if(e instanceof IllegalArgumentException)
                    wrongException = false;
            }
            assertTrue(!wrongException);
        }
    }

    @Test
    public void overhangTest() { //blackbox
        Piece[][] p = new Piece[HEIGHT][WIDTH];
        for(int i = 0; i < WIDTH - 1; i++) {
            p[HEIGHT-1][i] = new TetrisPiece(Piece.PieceType.STICK);
        }
        TetrisBoard env = new TetrisBoard(p);

        Piece currentPiece = null;
        for(int i = 0; i < 7; i++) {
            TetrisBoard copy = new TetrisBoard(env);
            if(i == 0)
                currentPiece = new TetrisPiece(Piece.PieceType.T, 2);
            if(i == 1)
                currentPiece = new TetrisPiece(Piece.PieceType.SQUARE);
            if(i == 2)
                currentPiece = new TetrisPiece(Piece.PieceType.STICK, 3);
            if(i == 3)
                currentPiece = new TetrisPiece(Piece.PieceType.LEFT_L);
            if(i == 4)
                currentPiece = new TetrisPiece(Piece.PieceType.RIGHT_L);
            if(i == 5)
                currentPiece = new TetrisPiece(Piece.PieceType.LEFT_DOG);
            if(i == 6)
                currentPiece = new TetrisPiece(Piece.PieceType.RIGHT_DOG);
            
            copy.nextPiece(currentPiece, new Point(WIDTH/2, HEIGHT/2));
            System.out.println(copy);
            copy.move(Board.Action.DROP);
            boolean onFloor = false;
            for(int j = 0; j < WIDTH; j++)
                if(copy.getGrid(j, 0) != null)
                    onFloor = true;
            assertTrue(onFloor);
        }
    }

    @Test
    public void nextPieceTest() { //whitebox
        TetrisBoard env = new TetrisBoard(WIDTH, HEIGHT);
        assertTrue(env.getCurrentPiece() == null && env.getCurrentPiecePosition() == null);
        Piece p = new TetrisPiece(Piece.PieceType.SQUARE);
        Point spawn = new Point(0, 0);
        env.nextPiece(p, spawn);
        assertTrue(env.getCurrentPiece().equals(p) && env.getCurrentPiecePosition().equals(spawn));
    }

    @Test
    public void placePieceTest() { //whitebox
        //vars changing: maxHeight, rowWidth, rowsCleared, colHeight, currentPiece (null), currentPiecePosition (null)
        Piece[][] p = new Piece[HEIGHT][WIDTH];
        Piece currentPiece = new TetrisPiece(Piece.PieceType.STICK);

        for(int y = 0; y < 4; y++) {
            for(int x = 0; x < WIDTH; x++) {
                if(x != 0) {
                    Piece temp = new TetrisPiece(Piece.PieceType.SQUARE);
                    p[y][x] = temp;
                }
            }
        }

        TetrisBoard env = new TetrisBoard(p);
        env.nextPiece(currentPiece, new Point((WIDTH-1)/2, HEIGHT - 4));
        Board.Result r = env.move(Board.Action.DROP);
        assertTrue((r == Board.Result.PLACE) && (env.getMaxHeight() == 5) && (env.getRowWidth(4) == 4) && (env.getRowsCleared() == 0) && (env.getColumnHeight(4) == 5 && env.getColumnHeight(5) == 5 && env.getColumnHeight(6) == 5 && env.getColumnHeight(7) == 5) && (env.getCurrentPiece() == null) && (env.getCurrentPiecePosition() == null));
    }

    @Test
    public void dropHeightTest() {
        Piece[][] p = new Piece[HEIGHT][WIDTH];
        Piece currentPiece = new TetrisPiece(Piece.PieceType.STICK, 3);

        for(int y = 0; y < 4; y++) {
            for(int x = 0; x < WIDTH; x++) {
                if(x != 0) {
                    Piece temp = new TetrisPiece(Piece.PieceType.SQUARE);
                    p[y][x] = temp;
                }
            }
        }

        TetrisBoard env = new TetrisBoard(p);
        env.nextPiece(currentPiece, new Point(WIDTH-2, HEIGHT - 4));
        env.move(Board.Action.DROP);
        currentPiece = new TetrisPiece(Piece.PieceType.STICK);
        int dropHeight = env.dropHeight(currentPiece, WIDTH/2);
        assertTrue(dropHeight == 2);
    }
}
