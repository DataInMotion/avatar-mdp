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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.gecko.emf.mongo.Options;
import org.gecko.emf.mongo.pushstream.constants.MongoPushStreamConstants;
import org.gecko.emf.pushstream.EPushStreamProvider;
import org.gecko.emf.repository.query.IQuery;
import org.gecko.emf.repository.query.QueryRepository;
import org.gecko.util.pushstreams.GeckoPushbackPolicyOption;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;
import org.osgi.util.pushstream.PushEvent;
import org.osgi.util.pushstream.PushStream;
import org.osgi.util.pushstream.QueuePolicyOption;

import de.avatar.mdp.apis.DBObjectsEvaluator;
import de.avatar.mdp.apis.EObjectEvaluator;
import de.avatar.mdp.apis.config.DBEvaluatorConfig;
import de.avatar.mdp.evaluation.EvaluationSummary;

/**
 * 
 * @author ilenia
 * @since Oct 11, 
 */
@Component(name = "DBObjectsEvaluator", service = DBObjectsEvaluator.class, 
configurationPid = "DBObjectsEvaluator", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class DBObjectsEvaluatorImpl implements DBObjectsEvaluator {

	@Reference(target="(repo_id=avatarmdp.avatarmdp)")
	ComponentServiceObjects<QueryRepository> repositorySO;

	@Reference
	ComponentServiceObjects<ResourceSet> rsFactory;

	@Reference(target=("(criterium=GDPR)"))
	EObjectEvaluator eObjEvaluator;

	private static final Logger LOGGER = Logger.getLogger(DBObjectsEvaluatorImpl.class.getName());
	private DBEvaluatorConfig config;
	private PromiseFactory factory = new PromiseFactory(Executors.newFixedThreadPool(4));

	@Activate
	public void activate(DBEvaluatorConfig config) {
		this.config = config;
		factory.submit(() -> {
			evaluate();
			return true;
		}).onSuccess(t -> LOGGER.info("Finished evaluating db EObjects!"))
		.onFailure(t -> LOGGER.log(Level.SEVERE, String.format("Something went wrong when evaluating EObjects from db"), t));		
	}

	/* 
	 * (non-Javadoc)
	 * @see de.avatar.mdp.apis.DBObjectsEvaluator#evaluate()
	 */
	@Override
	public void evaluate() {
		String[] eClassURIsToEvaluate = config.eClass_uris_to_evaluate();
		for(String eClassURIToEvaluate : eClassURIsToEvaluate) {
			doEvaluate(eClassURIToEvaluate);
		}
	}

	private void doEvaluate(String eClassURIToEvaluate) {
		QueryRepository repository = repositorySO.getService();
		ResourceSet resSet = rsFactory.getService();
		try {
			EObject eObjToEvaluate = resSet.getEObject(URI.createURI(eClassURIToEvaluate), true);
			if(eObjToEvaluate instanceof EClass eClassToEvaluate) {
				IQuery query = repository.createQueryBuilder().allQuery().build();
				EPushStreamProvider psp = repository.getEObjectByQuery(eClassToEvaluate, query, getLoadOptions());
				if(psp == null) {
					LOGGER.log(Level.SEVERE, String.format("Null EPushStreamProvider when recovering EObjects of EClass %s", eClassURIToEvaluate));
					return;
				}
				PushStream<EObject> eObjPushStream = psp.createPushStreamBuilder()
						.withPushbackPolicy(GeckoPushbackPolicyOption.LINEAR_AFTER_THRESHOLD.getPolicy(50))
						.withQueuePolicy(QueuePolicyOption.BLOCK)
						.withExecutor(Executors.newSingleThreadExecutor())
						.withBuffer(new ArrayBlockingQueue<PushEvent<? extends EObject>>(100))
						.build();

				Promise<Void> resultPromise = eObjPushStream
						.forEach(eObj -> {
							EvaluationSummary summary = eObjEvaluator.evaluateEObject(eObj);
							saveEvaluationSummaryAsJson(summary,eClassToEvaluate, eObj);
						});
				resultPromise.onFailure(t -> {
					rsFactory.ungetService(resSet);
					repositorySO.ungetService(repository);
					LOGGER.log(Level.SEVERE, String.format("Error evaluating EObjects for EClass %s", eClassURIToEvaluate), t);
				})
				.onSuccess(result -> {
					rsFactory.ungetService(resSet);
					repositorySO.ungetService(repository);
					LOGGER.log(Level.INFO, String.format("Finished evaluating EObjects for EClass %s", eClassURIToEvaluate));
				});

			} else {
				LOGGER.log(Level.SEVERE, String.format("EObject with URI %s is not of type EClass", eClassURIToEvaluate));
				return;
			}
		} catch(Exception e) {
			LOGGER.log(Level.SEVERE, String.format("Exception when evaluating EObjects for EClass %s", eClassURIToEvaluate), e);
			return;
		} 
	}


	private void saveEvaluationSummaryAsJson(EvaluationSummary summary, EClass eClass, EObject eObj) {
		ResourceSet resSet = rsFactory.getService();
		try {
			String repoName = config.repositorySO_target().replace("(repo_id=", "");
			repoName = repoName.replace(")", "");
			String outFileName = config.evaluation_out_folder() + repoName + "_" + eClass.getName() + "_" + EcoreUtil.getID(eObj) + ".json";
			Resource resource = resSet.createResource(URI.createFileURI(outFileName), "application/json");
			resource.getContents().add(summary);
			resource.save(null);
		} catch(Exception e) {
			LOGGER.log(Level.SEVERE, String.format("Exception when saving Evaluation Summary for %s", eClass.getName() + "_" + EcoreUtil.getID(eObj)), e);
		}
		finally {
			rsFactory.ungetService(resSet);
		}
	}

	private static Map<Object, Object> getLoadOptions(){
		Map<Object, Object> loadOptions = new HashMap<>();
		loadOptions.put(Options.OPTION_BATCH_SIZE, Integer.valueOf(600));
		loadOptions.put(MongoPushStreamConstants.OPTION_QUERY_PUSHSTREAM, Boolean.TRUE);
		loadOptions.put(Options.OPTION_PROXY_ATTRIBUTES, true);
		return loadOptions;
	}

}
