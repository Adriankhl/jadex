package jadex.commons.future;

import java.util.logging.Logger;

import jadex.commons.DebugException;

/**
 * Static helper class for creating result listeners.
 */
public class SResultListener {
	
	/** The logger. */
	private static Logger logger;
	
	/** Get the logger. */
	private static Logger getLogger() {
		if (logger != null) {
			logger = Logger.getLogger("s-result-listener");
		}
		return logger;
	}

	/**
	 * Constant with a no-op success listener.
	 */
	private final static IFunctionalResultListener<? extends Object> EMPTY_SUCCESS_LISTENER = new IFunctionalResultListener<Object>() {
		public void resultAvailable(Object result) {
		}
	};
	
	/**
	 * Constant with a no-op exception listener.
	 */
	private final static IFunctionalExceptionListener EMPTY_EXCEPTION_LISTENER = new IFunctionalExceptionListener() {
		public void exceptionOccurred(Exception exception) {
		}
	};
	
	/**
	 * Returns a SuccessListener that ignores all results.
	 * 
	 * @return {@link IFunctionalResultListener}
	 */
    @SuppressWarnings("unchecked")
    public static final <E> IFunctionalResultListener<E> ignoreResults() {
        return (IFunctionalResultListener<E>) EMPTY_SUCCESS_LISTENER;
    }
    
    /**
	 * Returns an OnExceptionListener that ignores all results.
	 * 
	 * @return {@link IFunctionalExceptionListener}
	 */
    public static final IFunctionalExceptionListener ignoreExceptions() {
        return (IFunctionalExceptionListener) EMPTY_EXCEPTION_LISTENER;
    }
    
    /**
	 * Returns an OnExceptionListener that logs exceptions to console.
	 * 
	 * @return {@link IFunctionalExceptionListener}
	 */
    public static final IFunctionalExceptionListener printExceptions() {
        return new IFunctionalExceptionListener()
		{
        	Exception debugException = Future.DEBUG ? new DebugException() : null;

			public void exceptionOccurred(Exception exception)
			{
				if(Future.DEBUG)
				{
					debugException.printStackTrace();
					exception.printStackTrace();
				}
				getLogger().severe("Exception occurred: "+this+", "+exception);
			}
		};
    }
    
	/**
     * Creates an {@link IResultListener} that delegates all results to a given Future.
     * 
     * @param delegate The future used for success delegation.
     * @return {@link IResultListener}
     */
	public static <E> IResultListener<E> delegate(final Future<E> delegate) {
//		return delegate(delegate, false);
		return new DelegationResultListener<E>(delegate);
	}
	
	/**
     * Creates an {@link IResultListener} that delegates all results to a given Future.
     * 
     * @param delegate The future used for success delegation.
     * @param undone Flag if undone methods should be used.
     * @return {@link IResultListener}
     */
	public static <E> IResultListener<E> delegate(final Future<E> delegate, boolean undone) {
		return delegate(delegate, undone);
	}
	
	/**
     * Creates an {@link IResultListener} that delegates all results to a given Future.
     * 
     * @param delegate The future used for success delegation.
     * @param undone Flag if undone methods should be used.
     * @param customResultListener Custom result listener that overwrites the delegation behaviour.
     * @return {@link IResultListener}
     */
	public static <E> IResultListener<E> delegate(final Future<E> delegate, IFunctionalResultListener<E> customResultListener) {
		return delegate(delegate, false, customResultListener);
	}
	
	/**
     * Creates an {@link IResultListener} that delegates all results to a given Future.
     * 
     * @param delegate The future used for success delegation.
     * @param undone Flag if undone methods should be used.
     * @param customResultListener Custom result listener that overwrites the delegation behaviour.
     * @return {@link IResultListener}
     */
	public static <E> IResultListener<E> delegate(final Future<E> delegate, boolean undone, IFunctionalResultListener<E> customResultListener) {
		return new DelegationResultListener<E>(delegate, undone, customResultListener);
	}
	
    
	/**
     * Creates an {@link IResultListener} that delegates exceptions to a given Future
     * and results to a given SuccessListener.
     * 
     * @param delegate The future used for exception delegation.
     * @param customResultListener The SuccessListener.
     * @return {@link IResultListener}
     */
    public static <E,T> IResultListener<E> delegateExceptions(final Future<T> delegate, final IFunctionalResultListener<E> customResultListener) {
    	return delegateExceptions(delegate, false, customResultListener);
    }
    
	/**
     * Creates an {@link IResultListener} that delegates exceptions to a given Future
     * and results to a given SuccessListener.
     * 
     * @param delegate The future used for exception delegation.
     * @param undone Flag if undone methods should be used.
     * @param customResultListener The SuccessListener.
     * @return {@link IResultListener}
     */
    public static <E,T> IResultListener<E> delegateExceptions(final Future<T> delegate, boolean undone, final IFunctionalResultListener<E> customResultListener) {
    	return new ExceptionDelegationResultListener<E, T>(delegate, undone, customResultListener);
    }
    
    /**
     * Creates an {@link CounterResultListener}.
     * 
     * @param num The number of sub callbacks.
     * @param countReachedListener Listener to be called when the given number is reached.
     * @return {@link CounterResultListener}
     */
    public static <E> CounterResultListener<E> countResults(int num, IFunctionalResultListener<Void> countReachedListener) {
    	return new CounterResultListener<E>(num, countReachedListener);
    }
    
    /**
     * Creates an {@link CounterResultListener}.
     * 
     * @param num The number of sub callbacks.
     * @param countReachedListener Listener to be called when the given number is reached.
     * @param exListener Listener to be called for exceptions.
     * @return {@link CounterResultListener}
     */
    public static <E> CounterResultListener<E> countResults(int num, IFunctionalResultListener<Void> countReachedListener, IFunctionalExceptionListener exListener) {
    	return new CounterResultListener<E>(num, countReachedListener, exListener);
    }

    /**
     * Creates an {@link CounterResultListener}.
     * 
     * @param num The number of sub callbacks.
     * @param countReachedListener Listener to be called when the given number is reached.
     * @param intermediateListener Listener to be called for intermediate Results.
     * @param exListener Listener to be called for exceptions.
     * @return {@link CounterResultListener}
     */
    public static <E> CounterResultListener<E> countResults(int num, IFunctionalResultListener<Void> countReachedListener, IFunctionalResultListener<E> intermediateListener, IFunctionalExceptionListener exListener) {
    	return new CounterResultListener<E>(num, countReachedListener, intermediateListener, exListener);
    }
    
	/**
	 * Creates an {@link IResultListener} that delegates results to the given
	 * SuccessListener and uses default exception handling.
	 * 
	 * @param sucListener The SuccessListener.
	 * @return {@link IResultListener}
	 */
	public static <E> IResultListener<E> createResultListener(final IFunctionalResultListener<E> sucListener)
	{
		return createResultListener(sucListener, true);
	}

	/**
	 * Creates an {@link IResultListener} that delegates results to the given
	 * SuccessListener.
	 * 
	 * @param sucListener The SuccessListener.
	 * @param defaultExceptionHandling Specifies whether to use a default
	 *        handling for exceptions or not.
	 * @return {@link IResultListener}
	 */
	public static <E> IResultListener<E> createResultListener(final IFunctionalResultListener<E> sucListener, final boolean defaultExceptionHandling)
	{
		if (defaultExceptionHandling) {
			return createResultListener(sucListener, printExceptions());
		} else {
			return createResultListener(sucListener, ignoreExceptions());
		}
	}

	/**
	 * Creates an {@link IResultListener} that delegates results to the given
	 * SuccessListener and Exceptions to the given ExceptionListener.
	 * 
	 * @param sucListener The SuccessListener.
	 * @param exListener The ExceptionListener. If <code>null</code>, exceptions are logged.
	 * @return {@link IResultListener}
	 */
	public static <E> IResultListener<E> createResultListener(final IFunctionalResultListener<E> sucListener, final IFunctionalExceptionListener exceptionListener)
	{
		final IFunctionalExceptionListener innerExceptionListener = (exceptionListener == null) ? printExceptions() : exceptionListener;
		return new IResultListener<E>()
		{
			public void resultAvailable(E result)
			{
				sucListener.resultAvailable(result);
			}
			public void exceptionOccurred(Exception exception)
			{
				innerExceptionListener.exceptionOccurred(exception);
			}
		};
	}
}
