package client.GUI;

import client.network.WAMClient;
import client.network.Observer;
import client.network.WAMObserver;
import common.WAMException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.List;

/**
 * A GUI representaton of a Whack-A-Mole game
 * given arguments of the game and build's a board to show the player
 * the moles visibly poping up
 *
 * any click on an active mole will get sent to the server
 *
 * the board also shows the players number, the scores of all players playing, and
 * will display any network related messages if needed
 *
 * the board is linked to the style.css sheet for appearance
 *
 * @author Christopher Asbrock
 * @author Shakeel Farooq
 */
public class WAMGUI extends Application implements Observer<WAMObserver>
{
    /**
     * a custom button class that will hold the buttons spot number to simplify
     * the process of managing a 2 x 2 array
     */
    private class EventButton extends Button
    {
        public int spot;
        public EventButton(int spot)
        {
            this.spot = spot;
        }
    }

    /**amount of rows the board will have*/
    private int rows;
    /**amount of cols the board will have*/
    private int cols;

    /**reference to the games model to help update the GUI when needed*/
    private WAMObserver model;
    /**reference to the games controller to send out whacks*/
    private WAMClient controller;

    /**reference for the scene*/
    private Scene scene;
    /**reference for the pane used to build the board*/
    private GridPane root;
    /**reference to the textfield over the leader board*/
    private TextField score;
    /**reference to the textfield that displays important messages*/
    private TextField message;
    /**reference to the text area that prints out players and scores*/
    private TextArea leader_board;
    /**A 2x2 array of buttons representing moles*/
    private EventButton[][]  buttons;

    /**
     * application init, creates the model and controller and links everything together
     * if there is no network the error will get thrown and prevent the application from starting
     *
     * @throws WAMException
     */
    @Override
    public void init()
    {
        //get the command line args
        List<String> args = getParameters().getRaw();

        //get the host info and port from command line
        String host = args.get(0);
        int port = Integer.parseInt(args.get(1));

        this.model = new WAMObserver();
        this.model.addObserver(this);

        this.controller = new WAMClient(host, port, this.model);
        this.rows = this.controller.getRows();
        this.cols = this.controller.getCols();
    }

    /**
     * sends a whack message to the server
     *
     * as a note, once the button is active all messages get sent, a whack on a mole
     * that hasn't popped up will inevitably get checked by the server and points will be lost
     * @param event
     */
    private void onclick(ActionEvent event)
    {
        EventButton button = (EventButton)event.getSource();
        this.controller.sendWhack(button.spot);
    }

    /**
     * sets up the buttons and adds them to the board,
     * at first all buttons are disabled, once everyone is connected they'll be enabled
     *
     * @param row - amount of rows the board has
     * @param col - amount of columns the board has
     */
    private void setButton(int row, int col)
    {
        this.buttons[row][col] = new EventButton(row + (row * (this.cols - 1)) + col);

        this.setButtonImage(this.buttons[row][col], "client/gui/hole_in_ground.png", "bad" );
        this.buttons[row][col].setDisable(true);

        this.buttons[row][col].setOnAction((ActionEvent event) -> onclick(event));
        root.add( this.buttons[row][col], col + 1, row + 2);
        GridPane.setMargin(this.buttons[row][col], new Insets(3, 3, 3, 3));
    }

    /**
     * sets up the score baord message,
     * will typically show wht player this client is, but will also show messages
     * such as whether they won, lost, tied, or if there was an error
     */
    private void setScoreMessage()
    {
        this.score = new TextField("");
        this.score.setId("score");
        this.score.setEditable(false);
        this.score.setFont(new Font(20));
        this.score.setAlignment(Pos.CENTER);
        this.score.setMouseTransparent(true);
        this.score.setStyle("-fx-background-color: rgba(242,242,242,0.5);");
    }

    /**
     * the network message, will display messages when trying to do somthing network related
     * such as waiting for other players or if the network was lost
     *
     * if there is no message it will be hidden
     */
    private void setNetworkMessage()
    {
        this.message = new TextField("WAITING FOR MORE PLAYERS...");
        this.message.setId("open");
        this.message.setEditable(false);
        this.message.setFont(new Font(20));
        this.message.setAlignment(Pos.CENTER);
        this.message.setMouseTransparent(true);
    }

    /**
     * a structured text area that will keep track of all players involved
     * it will show their current placement, player and scores
     * as well as noting if someone disconnected
     */
    private void setLeaderBoard()
    {
        this.leader_board = new TextArea();
        this.leader_board.setId("score_board");
        this.leader_board.setMouseTransparent(true);
        this.leader_board.setPrefRowCount(rows);
        this.leader_board.setPrefWidth(1);
    }

    /**
     * creates the game board,
     * left of the board is a message box and leader board, the right it topped of with a network
     * message box and a rows x cols grid of moles (depending on the peraeters sent from server)
     *
     * @param stage the stage the board will be added to
     */
    @Override
    public void start(Stage stage)
    {
        this.root = new GridPane();
        this.root.setId("root");

        this.setScoreMessage();
        this.setNetworkMessage();
        this.setLeaderBoard();

        this.root.add( this.score, 0, 0, 1, 1 );
        this.root.add( this.message, 1, 0, cols, 1 );
        this.root.add( this.leader_board, 0, 1, 1, 2 + rows);

        Region spacer = new Region();
        spacer.setPrefHeight(40);
        root.add( spacer, 1, 1 );

        this.buttons = new EventButton[this.rows][this.cols];

        for (int row = 0; row < this.rows; row++ )
        {
            for  (int col = 0; col < cols ; col++)
            {
                this.setButton(row , col);
            }
        }

        this.scene = new Scene( root );
        scene.getStylesheets().addAll(this.getClass().getResource("style.css").toExternalForm());
        stage.setScene( scene );
        stage.setResizable(false);
        stage.setTitle( "WHACK-A-MOLE" );
        stage.show();

        //notify the controller that the board is up and ready
        this.controller.start();
    }

    /**
     * if the application is closed it will give permission to the network to close its sockets
     */
    @Override
    public void stop()
    {
        this.controller.close();
    }

    /**
     * An update method used by the observer
     * any changes by the controller will call this and the board will refresh when it can
     *
     * @param observer - the observer observing this
     */
    @Override
    public void update(WAMObserver observer)
    {
        if (Platform.isFxApplicationThread())
        {
            this.refresh();
        }
        else
        {
            Platform.runLater(() -> this.refresh());
        }
    }

    /**
     * goes through the board and disables all buttons,
     * used for end game, if it has ended it will display what it needed and turn the buttons off
     *
     * the buttons are also given an id to change the style they're linked to
     *
     * @param id - the new id for the buttons
     */
    private void disable(String id, String text)
    {
        this.score.setText(text);

        for (int row = 0; row < this.rows; row++ )
        {
            for  (int col = 0; col < cols ; col++)
            {
                this.setButtonImage(this.buttons[row][col],
                        "client/gui/mole_in_hole_in_ground.png",
                        "bad");
                this.buttons[row][col].setMouseTransparent(true);
                this.buttons[row][col].setId(id);
            }
        }
    }

    /**
     * run through game board and use controller to update moles
     */
    private void updateBoard()
    {
        for (int row = 0; row < this.rows; row++ )
        {
            for  (int col = 0; col < this.cols ; col++)
            {
                if (this.model.getBoard()[row + (row * (this.cols - 1)) + col])
                {
                    this.setButtonImage(this.buttons[row][col],
                            "client/gui/mole_in_hole_in_ground.png",
                            "good");
                }
                else
                {
                    this.setButtonImage(this.buttons[row][col],
                            "client/gui/hole_in_ground.png",
                            "bad");
                }

                this.buttons[row][col].setDisable(false);
            }
        }
    }

    /**
     * takes in a button and changes the image on it and the css id its connected to
     *
     * @param button - the button to change
     * @param pic - the picture to change the button to
     * @param id - the new id to link a style with
     */
    private void setButtonImage(Button button, String pic, String id)
    {
        Image image = new Image(pic);
        ImageView icon = new ImageView(image);
        button.setGraphic(icon);
        button.setId(id);
    }

    /**
     * called by the update method,
     * uses information from the model and controller to updated the details of the game board
     * whenever it is called
     */
    private void refresh()
    {
        this.leader_board.setText(this.model.getScoreBoard());
        this.score.setText("PLAYER: " + this.model.getPlayer());
        this.message.setId("close");
        WAMObserver.Status current_status = this.model.getStatus();

        switch(current_status)
        {
            case PLAYING:
                this.updateBoard();
                break;
            case WON:
                this.disable("win", "WIN");
                break;
            case LOST:
                this.disable("lose", "LOSE");
                break;
            case TIED:
                this.disable("tied", "TIED");
                break;
            case ERROR:
                this.message.setId("error");
                this.message.setText(this.model.getErrorMessage());
                this.disable("tied", "ERROR");
                break;
        }
    }

    /**
     * Lanches a new instance of the GUI,
     *
     * has two arguments, the host, and the port
     * which are used in the int method to connect to the server
     *
     * @param args - the host and port
     */
    public static void main(String[] args)
    {
        if (args.length != 2)
        {
            System.out.println("Missing Command Line Arguments");
            System.exit(-1);
        }
        else
        {
            Application.launch(args);
        }
    }

}
