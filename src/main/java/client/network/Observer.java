package client.network;

/**
 * An interface representing any class whose objects get notified when
 * the objects they are observing update them
 *
 * @param <Client> the type of object an implementor of this interface
 *                is observing
 *
 * @author Shakeel Farooq
 * @author Christopher Asbrock
 */
public interface Observer<Client>
{
    /**
     * The observed subject calls this method on each observer that has
     * previously registered with it.
     *
     * @param client the object that wishes to inform this object
     *               about something that has happened
     */
    void update(Client client);
}
