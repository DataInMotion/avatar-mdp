/**
 * Copyright (c) 2012 - 2023 Data In Motion and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Data In Motion - initial API and implementation
 */
package de.avatar.mdp.apis;

import org.osgi.annotation.versioning.ProviderType;

/**
 * This service provider should be responsible of evaluating EObjects from a db
 * at startup. It should be responsibility of the config to set up from which db
 * the EObjects have to be taken, which EObjects have to be evaluated and 
 * according to which criteria.
 * 
 * @author ilenia
 * @since Oct 11, 2023
 */
@ProviderType
public interface DBObjectsEvaluator {
	
	void evaluate();

}
