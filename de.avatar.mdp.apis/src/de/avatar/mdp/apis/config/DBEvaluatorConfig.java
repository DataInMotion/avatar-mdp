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
package de.avatar.mdp.apis.config;

/**
 * 
 * @author ilenia
 * @since Oct 11, 2023
 */
public @interface DBEvaluatorConfig {
	
	/**
	 * @return the target filter to select the ComponentServiceObjects for the 
	 * EObjectEvaluator services. It means here we can specify with which evaluator
	 * we want to evaluate the EObjects
	 */
	String  eObjEvaluatorSO_target() default "(criterium=GDPR)";
	
	/**
	 * @return the target filter to select the ComponentServiceObjects for the 
	 * EMFRepository. It means that here we can specify in which EMFRepository we 
	 * should look for the EObjects to evaluate
	 */
	String repositorySO_target() default "(repo_id=avatarmdp.avatarmdp)";
	
	/**
	 * @return the list of EClass URIs for the EObjects that need to be evaluated
	 */
	String[] eClass_uris_to_evaluate() default {};
	
	/**
	 * @return the folder where the final EvaluationSummary will be placed
	 */
	String evaluation_out_folder() default "";

}
