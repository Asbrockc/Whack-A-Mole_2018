package server;

import common.WAMProtocol;

/**
 * Handler class the handles one mole,
 * Uses the servers clock to randomly pull the mole up or down
 * one launched for each mole so they all go on their own BUT are synchronized between players
 *
 * @author Christopher Asbrock
 * @author Shakeel Farooq
 */
public class Handler extends Thread implements WAMProtocol
{
    /** The mole spot */
    private int mole;
    /** The server */
    private WAMServer server;

    /**
     * The constructor for the handler
     * Gets the mole spot and server from the server
     *
     * @param mole the mole spot
     * @param server the server
     */
    public Handler(int mole, WAMServer server)
    {
        this.mole = mole;
        this.server = server;
    }

    /**
     * The run method for the handler
     * Based off the game duration, the mole pops up and sends the MOLE UP protocol
     * It keeps the mole up for some random time, then brings the mole down
     * and send the MOLE DOWN protocol.
     * This method keeps running until the end of the game
     */
    @Override
    public void run()
    {
        double time = server.random_range(4,10);
        int upDown = 0;
        int curr = 0;

        while (true)
        {
            System.out.flush();
            if (server.game_over)
                break;

            if (server.go)
            {
                if (server.getCurrentTime() > time || server.getWhacked()[mole])
                {
                    switch(upDown)
                    {
                        //mole is up
                        case 0:
                            server.getSpots()[mole] = true;
                            server.sendProtocol(MOLE_UP + " " + mole);
                            break;

                        //mole is down
                        case 1:
                            server.getSpots()[mole] = false;
                            server.sendProtocol(MOLE_DOWN + " " + mole);
                            break;
                    }

                    //If the mole is up, wait for some time
                    if (upDown == 0)
                        time = server.getCurrentTime() + (server.random_range(1,2));

                    //If the mole is down, wait for some time (longer)
                    else
                        time = server.getCurrentTime() + (server.random_range(3,8));

                    //switch between mole up and mole down
                    upDown = upDown ^ 1;
                    server.getWhacked()[mole] = false;

                }
            }
        }
    }
}
