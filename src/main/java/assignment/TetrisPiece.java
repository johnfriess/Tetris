package assignment;

import java.awt.*;
import java.util.Arrays;

/**
 * An immutable representation of a tetris piece in a particular rotation.
 * 
 * All operations on a TetrisPiece should be constant time, except for its
 * initial construction. This means that rotations should also be fast - calling
 * clockwisePiece() and counterclockwisePiece() should be constant time! You may
 * need to do pre-computation in the constructor to make this possible.
 */
public final class TetrisPiece implements Piece {

    private PieceType pieceType;
    private int rotationIndex;
    private int width;
    private int height;
    private Point[] body;
    private int[] skirt;

    // all piece rotations

    // T
    private final static Point[][] T_ROTATIONS = new Point[][] {
        { new Point(0, 1), new Point(1, 1), new Point(2, 1), new Point(1, 2) },
        { new Point(1, 2), new Point(1, 1), new Point(2, 1), new Point(1, 0) },
        { new Point(0, 1), new Point(1, 1), new Point(2, 1), new Point(1, 0) },
        { new Point(0, 1), new Point(1, 1), new Point(1, 0), new Point(1, 2) }
    };
    private final static int[][] T_SKIRTS = new int[][] {
        { 1, 1, 1 },
        { Integer.MAX_VALUE, 0, 1 },
        { 1, 0, 1 },
        { 1, 0, Integer.MAX_VALUE }
    };

    // O
    private final static Point[][] SQUARE_ROTATIONS = new Point[][] {
        { new Point(0, 0), new Point(0, 1), new Point(1, 0), new Point(1, 1) },
        { new Point(0, 0), new Point(0, 1), new Point(1, 0), new Point(1, 1) },
        { new Point(0, 0), new Point(0, 1), new Point(1, 0), new Point(1, 1) },
        { new Point(0, 0), new Point(0, 1), new Point(1, 0), new Point(1, 1) }
    };
    private final static int[][] SQUARE_SKIRTS = new int[][] {
        { 0, 0 },
        { 0, 0 },
        { 0, 0 },
        { 0, 0 }
    };

    // I
    private final static Point[][] STICK_ROTATIONS = new Point[][] {
        { new Point(0, 2), new Point(1, 2), new Point(2, 2), new Point(3, 2) },
        { new Point(2, 0), new Point(2, 1), new Point(2, 2), new Point(2, 3) },
        { new Point(0, 1), new Point(1, 1), new Point(2, 1), new Point(3, 1) },
        { new Point(1, 0), new Point(1, 1), new Point(1, 2), new Point(1, 3) }
    };
    private final static int[][] STICK_SKIRTS = new int[][] {
        { 2, 2, 2, 2 },
        { Integer.MAX_VALUE, Integer.MAX_VALUE, 0, Integer.MAX_VALUE },
        { 1, 1, 1, 1 },
        { Integer.MAX_VALUE, 0, Integer.MAX_VALUE, Integer.MAX_VALUE }
    };

    // J
    private final static Point[][] LEFT_L_ROTATIONS = new Point[][] {
        { new Point(0, 1), new Point(1, 1), new Point(2, 1), new Point(0, 2) },
        { new Point(1, 0), new Point(1, 1), new Point(1, 2), new Point(2, 2) },
        { new Point(0, 1), new Point(1, 1), new Point(2, 1), new Point(2, 0) },
        { new Point(1, 0), new Point(1, 1), new Point(1, 2), new Point(0, 0) }
    };
    private final static int[][] LEFT_L_SKIRTS = new int[][] {
        { 1, 1, 1 },
        { Integer.MAX_VALUE, 0, 2 },
        { 1, 1, 0 },
        { 0, 0, Integer.MAX_VALUE }
    };

    // L
    private final static Point[][] RIGHT_L_ROTATIONS = new Point[][] {
        { new Point(0, 1), new Point(1, 1), new Point(2, 1), new Point(2, 2) },
        { new Point(1, 0), new Point(1, 1), new Point(1, 2), new Point(2, 0) },
        { new Point(0, 1), new Point(1, 1), new Point(2, 1), new Point(0, 0) },
        { new Point(1, 0), new Point(1, 1), new Point(1, 2), new Point(0, 2) }
    };
    private final static int[][] RIGHT_L_SKIRTS = new int[][] {
        { 1, 1, 1 },
        { Integer.MAX_VALUE, 0, 0 },
        { 0, 1, 1 },
        { 2, 0, Integer.MAX_VALUE }
    };

    // Z
    private final static Point[][] LEFT_DOG_ROTATIONS = new Point[][] {
        { new Point(0, 2), new Point(1, 2), new Point(1, 1), new Point(2, 1) },
        { new Point(1, 0), new Point(2, 2), new Point(1, 1), new Point(2, 1) },
        { new Point(0, 1), new Point(1, 1), new Point(1, 0), new Point(2, 0) },
        { new Point(0, 0), new Point(1, 2), new Point(0, 1), new Point(1, 1) }
    };
    private final static int[][] LEFT_DOG_SKIRTS = new int[][] {
        { 2, 1, 1 },
        { Integer.MAX_VALUE, 0, 1 },
        { 1, 0, 0 },
        { 0, 1, Integer.MAX_VALUE }
    };

    // S
    private final static Point[][] RIGHT_DOG_ROTATIONS = new Point[][] {
        { new Point(0, 1), new Point(1, 1), new Point(1, 2), new Point(2, 2) },
        { new Point(2, 0), new Point(1, 1), new Point(2, 1), new Point(1, 2) },
        { new Point(0, 0), new Point(1, 0), new Point(1, 1), new Point(2, 1) },
        { new Point(1, 0), new Point(0, 1), new Point(1, 1), new Point(0, 2) }
    };
    private final static int[][] RIGHT_DOG_SKIRTS = new int[][] {
        { 1, 1, 2 },
        { Integer.MAX_VALUE, 1, 0 },
        { 0, 0, 1 },
        { 1, 0, Integer.MAX_VALUE }
    };

    /**
     * Construct a tetris piece of the given type. The piece should be in its spawn orientation,
     * i.e., a rotation index of 0.
     * 
     * You may freely add additional constructors, but please leave this one - it is used both in
     * the runner code and testing code.
     */
    public TetrisPiece(PieceType type) {
        this(type, 0);
    }

    public TetrisPiece(PieceType type, int rotation) {
        pieceType = type;
        rotationIndex = rotation;
        loadInputs();
    }

    public TetrisPiece(TetrisPiece p) { // copy constructor
        pieceType = p.pieceType;
        rotationIndex = p.rotationIndex;
        loadInputs();
    }

    @Override
    public PieceType getType() {
        return pieceType;
    }

    @Override
    public int getRotationIndex() {
        return rotationIndex;
    }

    @Override
    public Piece clockwisePiece() {
        return new TetrisPiece(pieceType, (rotationIndex + 1) % 4);
    }

    @Override
    public Piece counterclockwisePiece() {
        return new TetrisPiece(pieceType, (rotationIndex + 3) % 4); // -1 mod 4 = 3 mod 4
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public Point[] getBody() {
        return body;
    }

    @Override
    public int[] getSkirt() {
        return skirt;
    }

    @Override
    public boolean equals(Object other) {
        // Ignore objects which aren't also tetris pieces.
        if(!(other instanceof TetrisPiece)) return false;
        TetrisPiece otherPiece = (TetrisPiece) other;

        return (otherPiece.getType() == pieceType) && (otherPiece.getRotationIndex() == rotationIndex);
    }

    private void loadInputs() {
        switch (pieceType) {
            case T:
                setValues(3,T_ROTATIONS[rotationIndex],T_SKIRTS[rotationIndex]);
                break;
            case SQUARE:
                setValues(2,SQUARE_ROTATIONS[rotationIndex],SQUARE_SKIRTS[rotationIndex]);
                break;
            case STICK:
                setValues(4,STICK_ROTATIONS[rotationIndex],STICK_SKIRTS[rotationIndex]);
                break;
            case LEFT_L:
                setValues(3,LEFT_L_ROTATIONS[rotationIndex],LEFT_L_SKIRTS[rotationIndex]);
                break;
            case RIGHT_L:
                setValues(3,RIGHT_L_ROTATIONS[rotationIndex],RIGHT_L_SKIRTS[rotationIndex]);
                break;
            case LEFT_DOG:
                setValues(3,LEFT_DOG_ROTATIONS[rotationIndex],LEFT_DOG_SKIRTS[rotationIndex]);
                break;
            case RIGHT_DOG:
                setValues(3,RIGHT_DOG_ROTATIONS[rotationIndex],RIGHT_DOG_SKIRTS[rotationIndex]);
                break;
            default: // test this somehow?
                System.err.println("Not valid pieceType");
        }
    }

    private void setValues(int dimension, Point[] b, int[] s) {
        width = dimension;
        height = dimension;
        body = b;
        skirt = s;
    }
}
