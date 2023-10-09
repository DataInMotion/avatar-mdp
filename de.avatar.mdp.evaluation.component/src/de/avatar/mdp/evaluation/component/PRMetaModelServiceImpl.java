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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.gecko.emf.repository.query.IQuery;
import org.gecko.emf.repository.query.IQueryBuilder;
import org.gecko.emf.repository.query.QueryRepository;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import de.avatar.mdp.apis.api.PRMetaModelService;
import de.avatar.mdp.evaluation.EvaluationSummary;
import de.avatar.mdp.evaluation.RelevanceLevelType;
import de.avatar.mdp.prmeta.EvaluationCriteriumType;
import de.avatar.mdp.prmeta.PRClassifier;
import de.avatar.mdp.prmeta.PRMetaFactory;
import de.avatar.mdp.prmeta.PRMetaPackage;
import de.avatar.mdp.prmeta.PRModel;
import de.avatar.mdp.prmeta.PRModelElement;
import de.avatar.mdp.prmeta.PRPackage;

/**
 * 
 * @author ilenia
 * @since Aug 8, 2023
 */
@Component(name = "PRMetaModelService", service = PRMetaModelService.class)
public class PRMetaModelServiceImpl implements PRMetaModelService {
	
	@Reference(target=("(repo_id=avatarmdp.avatarmdp)"))
	ComponentServiceObjects<QueryRepository> repositorySO;
	
	private static final Logger LOGGER = Logger.getLogger(PRMetaModelServiceImpl.class.getName());

	
	/* 
	 * (non-Javadoc)
	 * @see de.avatar.mdp.apis.api.PRMetaModelService#getPRModelByNsURI(java.lang.String[])
	 */
	@Override
	public List<PRModel> getPRModelByNsURI(String... modelNsURIs) {
		QueryRepository queryRepo = repositorySO.getService();
		try {
			IQueryBuilder queryBuilder = queryRepo.createQueryBuilder();
			for(String modelNsURI : modelNsURIs) {
				IQuery query = queryBuilder.simpleValue(modelNsURI).column(PRMetaPackage.eINSTANCE.getPRModel_PackageURI()).build();
				queryBuilder.or(query);
			}
			return queryRepo.getEObjectsByQuery(PRMetaPackage.eINSTANCE.getPRModel(), queryBuilder.build(), null);
		} finally {
			repositorySO.ungetService(queryRepo);
		}
	}

	/* 
	 * (non-Javadoc)
	 * @see de.avatar.mdp.apis.api.PRMetaModelService#savePRModel(de.avatar.mdp.prmeta.PRModel)
	 */
	@Override
	public void savePRModel(PRModel prModel) {
		Objects.requireNonNull(prModel, "Cannot save null PRModel!");
		QueryRepository queryRepo = repositorySO.getService();
		try {
			queryRepo.save(prModel);
		} finally {
			repositorySO.ungetService(queryRepo);
		}
	}

	/* 
	 * (non-Javadoc)
	 * @see de.avatar.mdp.apis.api.PRMetaModelService#createPRModel(de.avatar.mdp.evaluation.EvaluationSummary, org.eclipse.emf.ecore.EPackage)
	 */
	@Override
	public PRModel createPRModel(EvaluationSummary evaluationSummary, EPackage ePackage) {
//		TODO:
//		1. Check if a PRModel for that EPackage already exists
//		2a. If yes, modify the existing one, otherwise create a new one
//		2b. If not, we have to set the package URI and we have to add a PRPackage with criterium NONE
//		3. If yes, we have to check if a PRPackage for the same evaluation criterium exists
//		4. If yes, we substitute that PRPackage, otherwise we add a new one
		
//		1./2.
		PRModel prModel = null;
		List<PRModel> prModels = getPRModelByNsURI(ePackage.getNsURI());
		if(prModels.isEmpty()) {
			prModel = PRMetaFactory.eINSTANCE.createPRModel();
			prModel.setPackageURI(ePackage.getNsURI());
			prModel.getPrPackage().add(createPRPackageWOCriterium(ePackage));
		}
		else prModel = prModels.get(0);
		
//		3./4.
		PRPackage existingPRPackage = prModel.getPrPackage().stream().filter(p -> p.getEvaluationCriterium().getLiteral().equals(evaluationSummary.getEvaluationCriterium().getLiteral())).findFirst().orElse(null);
		if(existingPRPackage != null && existingPRPackage.getEvaluationModelUsed().equals(evaluationSummary.getEvaluationModelUsed())) prModel.getPrPackage().remove(existingPRPackage);
		
//		Set evaluation criterium
		PRPackage prPackage = PRMetaFactory.eINSTANCE.createPRPackage();
		prPackage.setEvaluationCriterium(EvaluationCriteriumType.getByName(evaluationSummary.getEvaluationCriterium().getName()));
		prPackage.setEvaluationModelUsed(evaluationSummary.getEvaluationModelUsed());
		prPackage.setEPackage((EPackage) proxifyEObject(ePackage, ePackage.getNsURI()));
		
//		Loop over the EvaluationSummary features and add the corresponding PRFeature to the PRModel
		evaluationSummary.getEvaluatedTerms().forEach(et -> {
			ENamedElement evaluatedModelElement = et.getEvaluatedModelElement();		
			if(evaluatedModelElement.eIsProxy()) {
				evaluatedModelElement = (ENamedElement) EcoreUtil.resolve(evaluatedModelElement, prPackage);
			}
			PRModelElement prModelElement = PRMetaFactory.eINSTANCE.createPRModelElement();
			prModelElement.setModelElement((ENamedElement) proxifyEObject(evaluatedModelElement, ePackage.getNsURI()));
			Map<String, de.avatar.mdp.prmeta.RelevanceLevelType> relevanceMap = new HashMap<>();
			et.getEvaluations().forEach(evaluation -> {
				if(evaluation.isNegationDetected()) prModelElement.setNegationDetected(true);
				evaluation.getRelevance().forEach(relevance -> {
					if(!relevanceMap.containsKey(relevance.getCategory())) relevanceMap.put(relevance.getCategory(), de.avatar.mdp.prmeta.RelevanceLevelType.NOT_RELEVANT);
					if(relevance.getLevel().equals(RelevanceLevelType.POTENTIALLY_RELEVANT) && relevanceMap.get(relevance.getCategory()).equals(de.avatar.mdp.prmeta.RelevanceLevelType.NOT_RELEVANT)) {
						relevanceMap.put(relevance.getCategory(), de.avatar.mdp.prmeta.RelevanceLevelType.POTENTIALLY_RELEVANT);
					}
					else if(relevance.getLevel().equals(RelevanceLevelType.RELEVANT) && 
							(relevanceMap.get(relevance.getCategory()).equals(de.avatar.mdp.prmeta.RelevanceLevelType.NOT_RELEVANT) || 
									relevanceMap.get(relevance.getCategory()).equals(de.avatar.mdp.prmeta.RelevanceLevelType.POTENTIALLY_RELEVANT))) {
						relevanceMap.put(relevance.getCategory(), de.avatar.mdp.prmeta.RelevanceLevelType.RELEVANT);
					}
				});
			});
			relevanceMap.forEach((category, level) -> {
				de.avatar.mdp.prmeta.Relevance relevance = PRMetaFactory.eINSTANCE.createRelevance();
				relevance.setCategory(category);
				relevance.setLevel(level);
				prModelElement.getRelevance().add(relevance);
			});
			relevanceMap.clear();
			
			EClassifier eClassifier = ePackage.getEClassifier(et.getElementClassifierName());	
			PRClassifier prClassifier = prPackage.getPrClassifier().stream().filter(c -> c.getEClassifier().equals(eClassifier)).findFirst().orElse(null);
			if(prClassifier == null) {
				prClassifier = PRMetaFactory.eINSTANCE.createPRClassifier();
				prPackage.getPrClassifier().add(prClassifier);
			}
			prClassifier.setEClassifier((EClassifier) proxifyEObject(eClassifier, ePackage.getNsURI()));
			prClassifier.getPrModelElement().add(prModelElement);
		});
		
		adjustClassifierRelevance(prPackage.getPrClassifier());
		prModel.getPrPackage().add(prPackage);
		savePRModel(prModel);
		return prModel;
	}

	
	private void adjustClassifierRelevance(EList<PRClassifier> prClassifier) {
		Map<String, de.avatar.mdp.prmeta.RelevanceLevelType> relevanceMap = new HashMap<>();
		prClassifier.forEach(cl -> {
			cl.getPrModelElement().forEach(f -> {
				f.getRelevance().forEach(r -> {
					if(!relevanceMap.containsKey(r.getCategory())) relevanceMap.put(r.getCategory(), de.avatar.mdp.prmeta.RelevanceLevelType.NOT_RELEVANT);
					if(r.getLevel().equals(de.avatar.mdp.prmeta.RelevanceLevelType.POTENTIALLY_RELEVANT) && relevanceMap.get(r.getCategory()).equals(de.avatar.mdp.prmeta.RelevanceLevelType.NOT_RELEVANT) ) {
						relevanceMap.put(r.getCategory(), r.getLevel());
					} else if(r.getLevel().equals(de.avatar.mdp.prmeta.RelevanceLevelType.RELEVANT) && (relevanceMap.get(r.getCategory()).equals(de.avatar.mdp.prmeta.RelevanceLevelType.NOT_RELEVANT) || relevanceMap.get(r.getCategory()).equals(de.avatar.mdp.prmeta.RelevanceLevelType.POTENTIALLY_RELEVANT))) {
						relevanceMap.put(r.getCategory(), r.getLevel());
					}
				});
			});
			relevanceMap.forEach((category, level) -> {
				de.avatar.mdp.prmeta.Relevance prRelevance = PRMetaFactory.eINSTANCE.createRelevance();
				prRelevance.setCategory(category);
				prRelevance.setLevel(level);
				cl.getRelevance().add(prRelevance);
			});
			relevanceMap.clear();
		});
		
	}

	private PRPackage createPRPackageWOCriterium(EPackage ePackage) {
		PRPackage prPackage = PRMetaFactory.eINSTANCE.createPRPackage();
		prPackage.setEPackage((EPackage) proxifyEObject(ePackage, ePackage.getNsURI()));
		prPackage.setEvaluationCriterium(EvaluationCriteriumType.NONE);
		ePackage.getEClassifiers().forEach(ec -> {
			PRClassifier prClassifier = PRMetaFactory.eINSTANCE.createPRClassifier();
			prClassifier.setEClassifier((EClassifier)proxifyEObject(ec, ePackage.getNsURI()));
			de.avatar.mdp.prmeta.Relevance prRelevance = PRMetaFactory.eINSTANCE.createRelevance();
			prRelevance.setCategory("NONE");
			prRelevance.setLevel(de.avatar.mdp.prmeta.RelevanceLevelType.NOT_RELEVANT);
			prClassifier.getRelevance().add(prRelevance);
			if(ec instanceof EClass eclass) {
				eclass.getEAllStructuralFeatures().forEach(sf -> {
					prClassifier.getPrModelElement().add(doCreatePRModelElement(sf, prPackage, ePackage, "NONE", de.avatar.mdp.prmeta.RelevanceLevelType.NOT_RELEVANT));
				});
			} else if(ec instanceof EEnum eEnum) {
				eEnum.getELiterals().forEach(literal -> {
					prClassifier.getPrModelElement().add(doCreatePRModelElement(literal, prPackage, ePackage, "NONE", de.avatar.mdp.prmeta.RelevanceLevelType.NOT_RELEVANT));
				});
			} else if(ec instanceof EDataType eDataType) {
				prClassifier.getPrModelElement().add(doCreatePRModelElement(eDataType, prPackage, ePackage, "NONE", de.avatar.mdp.prmeta.RelevanceLevelType.NOT_RELEVANT));
			}
			prPackage.getPrClassifier().add(prClassifier);
		});
		return prPackage;
	}
	
	private PRModelElement doCreatePRModelElement(ENamedElement element, PRPackage prPackage, 
				EPackage ePackage, String relevanceCtegory, de.avatar.mdp.prmeta.RelevanceLevelType relevanceType) {
		if(element.eIsProxy()) {
			element = (ENamedElement) EcoreUtil.resolve(element, prPackage);
		}
		PRModelElement prModelElement = PRMetaFactory.eINSTANCE.createPRModelElement();
		prModelElement.setModelElement((ENamedElement)proxifyEObject(element, ePackage.getNsURI()));
		de.avatar.mdp.prmeta.Relevance relevance = PRMetaFactory.eINSTANCE.createRelevance();
		relevance.setCategory(relevanceCtegory);
		relevance.setLevel(relevanceType);
		prModelElement.getRelevance().add(relevance);
		return prModelElement;
	}
	
	private EObject proxifyEObject(EObject eObj, String uriString) {
		
		if(eObj instanceof InternalEObject) {
			InternalEObject ieo = (InternalEObject) eObj;
			if(!(eObj instanceof EPackage)) {
				ieo.eSetProxyURI(EcoreUtil.getURI(eObj));
			} else {
				ieo.eSetProxyURI(URI.createURI(uriString));
			}
			return ieo;
		}
		throw new IllegalStateException(String.format("EObject %s is not an instance of InternalEObject!", eObj.toString()));
	}
}
