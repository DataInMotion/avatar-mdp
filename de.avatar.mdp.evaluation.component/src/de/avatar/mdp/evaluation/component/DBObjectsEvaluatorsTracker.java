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
package de.avatar.mdp.evaluation.component;

import java.util.Map;
import java.util.logging.Logger;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import de.avatar.mdp.apis.DBObjectsEvaluator;

/**
 * 
 * @author ilenia
 * @since Oct 12, 2023
 */
@Component(immediate = true, name = "DBObjectsEvaluatorsTracker")
public class DBObjectsEvaluatorsTracker {
	
	private static final Logger LOGGER = Logger.getLogger(DBObjectsEvaluatorsTracker.class.getName());
	
	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policyOption = ReferencePolicyOption.GREEDY, 
			policy = ReferencePolicy.DYNAMIC, bind = "removeDBObjectsEvaluator")
	public void addDBObjectsEvaluator(DBObjectsEvaluator service,  Map<String, Object> properties) {
		LOGGER.info("Adding new DBObjectsEvaluator");
		service.evaluate();
	}

	public void removeDBObjectsEvaluator(DBObjectsEvaluator service) {
//		nothing to do here
	}
}
