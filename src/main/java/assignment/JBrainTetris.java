package assignment;

import java.util.*;
import java.awt.event.*;

public class JBrainTetris extends JTetris {
    protected Brain brain;
    protected javax.swing.Timer brainTimer; // time between moves that brain makes
    public static final int BRAINDELAY = 1;    // speed for brain moves
    public static final String FILEPATH = "src/main/java/assignment/qTable.txt"; // change for loading in table, null makes it train itself

    public static void main(String[] args) {
        createGUI(new JBrainTetris());
    }

    public JBrainTetris() {
        super(); // calls JTetris constructor

        // create brain and link w/ timer 
        brain = new QLearningBrain(WIDTH, HEIGHT, 2, 3, TOP_SPACE, FILEPATH); // 2x width pooling, 3x height pooling

        brainTimer = new javax.swing.Timer(BRAINDELAY, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                brainTick(brain.nextMove(board));
            }
        });
    }

    @Override
    public void tick(Board.Action verb) {} // remove user control


    // copy regular tick to brainTick for shifting to brain control
    public void brainTick(Board.Action verb) {
        if (!gameOn) {
            return;
        }

        Board.Result result = board.move(verb);
        switch (result) {
          case SUCCESS:
          case OUT_BOUNDS:
            break;
          case PLACE:
              if (board.getMaxHeight() > HEIGHT) {
                  stopGame();
              }
          case NO_PIECE:
              if (gameOn) {
                  addNewPiece();
              }
            break;
        }

        repaint();
    }

    // same as regular startGame except brainTimer.start()
    @Override
    public void startGame() {
        board = new TetrisBoard(WIDTH, HEIGHT + TOP_SPACE);

        repaint();

        count = 0;
        gameOn = true;

        random = new Random();

        startButton.setEnabled(!gameOn);
        stopButton.setEnabled(gameOn);
        timeLabel.setText(" ");
        addNewPiece();
        timer.start();
        
        brainTimer.start(); // only added info

        startTime = System.currentTimeMillis();
    }

}
