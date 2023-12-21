package client.network;

import java.util.LinkedList;
import java.util.List;
import client.network.Observer;

/**
 * The model for the Whack-A-Mole game
 *
 * @author Shakeel Farooq
 * @author Chris Asbrock
 */
public class WAMObserver
{
    /** Possible statuses of the game */
    public enum Status {SET_UP, PLAYING, WON, LOST, TIED, ERROR;}

    /** The current status of the game */
    private Status current_status;

    /** The player number (Used to determine which player this is) */
    private int player;

    /**
     * Boolean list that represents the whack-a-mole board
     * If the boolean value is true, it means that a mole is up,
     * otherwise the mole is down.
     */
    private boolean[] spots;

    /** String list of each of the player's scores */
    private String scoreboard;

    /** String message that represents an error */
    private String errorMessage;

    /** The observers of this model */
    private List<Observer<WAMObserver>> observers;

    /**
     * The view calls this method to add themselves as an observer of the model
     *
     * @param observer the observer
     */
    public void addObserver(Observer<WAMObserver> observer)
    {
        this.observers.add(observer);
    }

    /**
     * When the model changes, the observers are notified via their update() method
     */
    public void updateObservers()
    {
        for(Observer<WAMObserver> obs : this.observers)
        {
            obs.update(this);
        }
    }

    /**
     * The constructor for the WAMObserver
     * Puts the observers in a linked list and sets the status as SET_UP
     */
    public WAMObserver()
    {
        this.observers = new LinkedList<>();
        current_status = Status.SET_UP;
        this.scoreboard = "";
        this.errorMessage = "Waiting For More Players...";
    }

    /**
     * error message mutator
     *
     * @param message - the new message
     */
    public void changeErrorMessage(String message)
    {
        this.errorMessage = message;
    }

    /**
     * error message getter
     *
     * @return - the current error message
     */
    public String getErrorMessage()
    {
        return this.errorMessage;
    }

    /**
     * Method that gets the player number
     * Used by the GUI
     *
     * @return player
     */
    public int getPlayer()
    {
        return this.player;
    }

    /** Method that sets the player's number */
    public void setPlayer(int number)
    {
        this.player = number;
    }

    /**
     * Gets the current status
     * Used by the GUI
     *
     * @return current_status
     */
    public Status getStatus()
    {
        return current_status;
    }

    /**
     * Method that changes the status of the game
     *
     * @param status The status to change to
     */
    public void changeStatus(Status status)
    {
        this.current_status = status;
        if (status != Status.SET_UP)
            this.updateObservers();
    }

    /**
     * Method that creates the board with a specific number of rows and columns
     * When creating this board, the boolean values are automatically set to false
     *
     * @param rows the number of rows
     * @param cols the number of columns
     */
    public void setBoard(int rows, int cols)
    {
        this.spots = new boolean[rows * cols];
    }

    /**
     * Method that gets the board
     *
     * @return spots
     */
    public boolean[] getBoard()
    {
        return this.spots;
    }

    /**
     * Make a mole go up at a certain position
     *
     * @param spot the position of the mole that goes up
     */
    public void moleUp(int spot)
    {
        this.spots[spot] = true;
        this.updateObservers();
    }

    /**
     * Make a mole go down at a certain position
     *
     * @param spot The position of the mole that goes down
     */
    public void moleDown(int spot)
    {
        this.spots[spot] = false;
        this.updateObservers();
    }

    /**
     * Returns the score for this player
     *
     * @return this.score
     */
    public String getScoreBoard()
    {
        return this.scoreboard;
    }

    /**
     * Called by the
     * @param scoreboard
     */
    public void updateScoreBoard(String scoreboard)
    {
        this.scoreboard = scoreboard;
    }

    /**
     * Updates the player score
     * Called by the client
     */
    public void updateScore()
    {
        updateObservers();
    }
}
