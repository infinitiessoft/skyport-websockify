/* OrderException.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.1.1.1 $
 * Author: $Author: suvarov $
 * Date: $Date: 2007/03/08 00:26:15 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: 
 */
package com.lixia.rdp;

public class OrderException extends Exception {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public OrderException() {
	super();
    }

    public OrderException(String s) {
	super(s);
    }
}
