package common;

/**
 * A custom exception thrown from any of the WAM classes if something
 * goes wrong
 *
 * @author Shakeel Farooq
 * @author Chris Asbrock
 */
public class WAMException extends Exception
{
    /**
     * Convenience constructor to create a new {@link WAMException}
     */
    public WAMException()
    {
        super();
    }

    /**
     * Convenience constructor to create a new {@link WAMException}
     * with an error message.
     *
     * @param message The error message associated with the exception.
     */
    public WAMException(String message)
    {
        super(message);
    }
}
