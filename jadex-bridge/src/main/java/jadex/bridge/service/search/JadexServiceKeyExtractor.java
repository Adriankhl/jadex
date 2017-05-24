package jadex.bridge.service.search;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import jadex.bridge.ClassInfo;
import jadex.bridge.sensor.service.TagProperty;
import jadex.bridge.service.IService;

public class JadexServiceKeyExtractor implements IKeyExtractor
{
	/** Key type for the service interface. */
	public static final String KEY_TYPE_INTERFACE = "interface";
	
	/** Key type for the service tags. */
	public static final String KEY_TYPE_TAGS = "tags";
	
	/** Key type for the service provider. */
	public static final String KEY_TYPE_PROVIDER = "provider";
	
	/** Key type for the service platform. */
	public static final String KEY_TYPE_PLATFORM = "platform";
	
	public static final String[] SERVICE_KEY_TYPES;
	static
	{
		List<String> keytypes = new ArrayList<String>();
		try
		{
			Field[] fields = JadexServiceKeyExtractor.class.getDeclaredFields();
			for (Field field : fields)
			{
				if (field.getName().startsWith("KEY_TYPE_"))
				{
					keytypes.add((String) field.get(null));
				}
			}
		}
		catch (Exception e)
		{
		}
		SERVICE_KEY_TYPES = keytypes.toArray(new String[keytypes.size()]);
	}
	
	/**
	 *  Extracts keys from a service.
	 *  
	 *  @param keytype The type of key being extracted.
	 *  @param service The service.
	 *  @return The keys matching the type.
	 */
	public Set<String> getKeys(String keytype, Object serv)
	{
		return getKeysStatic(keytype, serv);
	}
	
	/**
	 *  Extracts keys from a service.
	 *  
	 *  @param keytype The type of key being extracted.
	 *  @param service The service.
	 *  @return The keys matching the type.
	 */
	@SuppressWarnings("unchecked")
	public static final Set<String> getKeysStatic(String keytype, Object serv)
	{
		IService service = (IService) serv;
		Set<String> ret = null;
		if (KEY_TYPE_INTERFACE.equals(keytype))
		{
			ret = new HashSet<String>();
			ret.add(service.getServiceIdentifier().getServiceType().toString());
			ClassInfo[] supertypes = service.getServiceIdentifier().getServiceSuperTypes();
			if (supertypes != null)
			{
				for (ClassInfo supertype : supertypes)
					ret.add(supertype.toString());
			}
		}
		else if (KEY_TYPE_TAGS.equals(keytype))
		{
			Map<String, Object> sprops = service.getPropertyMap();
			if (sprops != null)
				ret = (Set<String>) sprops.get(TagProperty.SERVICE_PROPERTY_NAME);
		}
		else if (KEY_TYPE_PROVIDER.equals(keytype))
		{
			ret = new SetWrapper<String>(service.getServiceIdentifier().getProviderId().toString());
		}
		else if (KEY_TYPE_PLATFORM.equals(keytype))
		{
			ret = new SetWrapper<String>(service.getServiceIdentifier().getProviderId().getRoot().toString());
		}
		return ret;
	}
	
	/**
	 *  Efficiently wrap a single value as a Set.
	 */
	private static class SetWrapper<T> implements Set<T>
	{
		private T wrappedobject;
		
		@SuppressWarnings("unused")
		public SetWrapper()
		{
		}
		
		public SetWrapper(T wrappedobject)
		{
			this.wrappedobject = wrappedobject;
		}
		
		public int size()
		{
			return wrappedobject != null ? 1 : 0;
		}

		public boolean isEmpty()
		{
			return wrappedobject == null;
		}

		public boolean contains(Object o)
		{
			return wrappedobject != null ? wrappedobject.equals(o) : false;
		}
		
		public Iterator<T> iterator()
		{
			return new Iterator<T>()
			{
				boolean next = true;
				
				public boolean hasNext()
				{
					return next;
				}

				public T next()
				{
					if (next)
					{
						next = false;
						return wrappedobject;
					}
					else
						throw new NoSuchElementException();
				}
				
			};
		}

		public Object[] toArray()
		{
			return new Object[] { wrappedobject };
		}

		@SuppressWarnings("unchecked")
		public Object[] toArray(Object[] a)
		{
			if (wrappedobject != null)
			{
				if (a != null && a.length > 1)
				{
					a[0] = wrappedobject;
					return a;
				}
			}
			return new Object[] { wrappedobject };
		}

		public boolean add(T e)
		{
			if (wrappedobject != null)
			{
				if (wrappedobject.equals(e))
					return false;
				else
					throw new IllegalArgumentException();
			}
			wrappedobject = e;
			return true;
		}

		public boolean remove(Object o)
		{
			if (wrappedobject != null && wrappedobject.equals(o))
			{
				wrappedobject = null;
				return true;
			}
			return false;
		}

		@Override
		public boolean containsAll(Collection<?> c)
		{
			if (wrappedobject != null && c.size() == 1 && wrappedobject.equals(c.iterator().next()))
				return true;
			return false;
		}

		public boolean addAll(Collection<? extends T> c)
		{
			throw new UnsupportedOperationException();
		}

		public boolean retainAll(Collection<?> c)
		{
			throw new UnsupportedOperationException();
		}
		
		public boolean removeAll(Collection<?> c)
		{
			throw new UnsupportedOperationException();
		}
		
		public void clear()
		{
			wrappedobject = null;
		}
	}
}