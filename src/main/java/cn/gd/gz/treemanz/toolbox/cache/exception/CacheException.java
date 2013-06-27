package cn.gd.gz.treemanz.toolbox.cache.exception;

/**
 * @author Treeman
 */
public class CacheException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = -733986617863274553L;

    private Exception parentException;

    /**
     * @return the parentException
     */
    public Exception getParentException() {
        return parentException;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Throwable#getMessage()
     */
    @Override
    public String getMessage() {
        return super.getMessage() + "; " + (null != parentException ? parentException.getMessage() : "");
    }

    public CacheException() {
        super();
    }

    public CacheException(String msg) {
        super(msg);
    }

    public CacheException(Throwable t) {
        super(t);
    }

    public CacheException(String msg, Throwable t) {
        super(msg, t);
    }

    public CacheException(Exception parentException) {
        this.parentException = parentException;
    }

    public CacheException(Exception parentException, String msg) {
        this(msg);
        this.parentException = parentException;
    }

    public CacheException(Exception parentException, Throwable t) {
        this(t);
        this.parentException = parentException;
    }

    public CacheException(Exception parentException, String msg, Throwable t) {
        this(msg, t);
        this.parentException = parentException;
    }

}
