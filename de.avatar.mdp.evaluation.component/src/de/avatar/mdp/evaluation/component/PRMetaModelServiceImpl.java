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

import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
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
import de.avatar.mdp.prmeta.EvaluationCriteriumType;
import de.avatar.mdp.prmeta.EvaluationLevelType;
import de.avatar.mdp.prmeta.PRClassifier;
import de.avatar.mdp.prmeta.PRFeature;
import de.avatar.mdp.prmeta.PRMetaFactory;
import de.avatar.mdp.prmeta.PRMetaPackage;
import de.avatar.mdp.prmeta.PRModel;
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
//		4. If yes, we modify that PRPackage, otherwise we add a new one
		
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
		if(existingPRPackage != null) prModel.getPrPackage().remove(existingPRPackage);
		
//		Set evaluation criterium
		PRPackage prPackage;
		if(existingPRPackage == null) prPackage = PRMetaFactory.eINSTANCE.createPRPackage();
		else prPackage = existingPRPackage;
		prPackage.setEvaluationCriterium(EvaluationCriteriumType.getByName(evaluationSummary.getEvaluationCriterium().getName()));
		prPackage.setEPackage((EPackage) proxifyEObject(ePackage, ePackage.getNsURI()));
		
//		Loop over the EvaluationSummary features and add the corresponding PRFeature to the PRModel
		evaluationSummary.getEvaluatedTerms().forEach(et -> {
			EStructuralFeature feature = et.getEvaluatedFeature();		
			if(feature.eIsProxy()) {
				feature = (EStructuralFeature) EcoreUtil.resolve(feature, prPackage);
			}
			PRFeature prFeature = PRMetaFactory.eINSTANCE.createPRFeature();
			prFeature.setFeature((EStructuralFeature) proxifyEObject(feature, ePackage.getNsURI()));
			boolean isFeatureRelevant = et.getEvaluations().stream().filter(e -> e.isRelevant()).findAny().orElse(null) != null;
			prFeature.setEvaluationLevel(isFeatureRelevant ? EvaluationLevelType.WARNING : EvaluationLevelType.NONE);
			EClassifier eClassifier = ePackage.getEClassifier(et.getFeatureClassifierName());
			
			PRClassifier prClassifier = prPackage.getPrClassifier().stream().filter(c -> c.getEClassifier().equals(eClassifier)).findFirst().orElse(null);
			if(prClassifier == null) {
				prClassifier = PRMetaFactory.eINSTANCE.createPRClassifier();
				prPackage.getPrClassifier().add(prClassifier);
			}
			prClassifier.setEClassifier((EClassifier) proxifyEObject(eClassifier, ePackage.getNsURI()));
			prClassifier.getPrFeature().add(prFeature);
			boolean isClassifierRelevant = prClassifier.getPrFeature().stream().filter(prf -> prf.getEvaluationLevel().equals(EvaluationLevelType.WARNING)).findAny().orElse(null) != null;
			prClassifier.setEvaluationLevel(isClassifierRelevant ? EvaluationLevelType.WARNING : EvaluationLevelType.NONE);
		});
		
		prModel.getPrPackage().add(prPackage);
		return prModel;
	}

	
	private PRPackage createPRPackageWOCriterium(EPackage ePackage) {
		PRPackage prPackage = PRMetaFactory.eINSTANCE.createPRPackage();
		prPackage.setEPackage((EPackage) proxifyEObject(ePackage, ePackage.getNsURI()));
		prPackage.setEvaluationCriterium(EvaluationCriteriumType.NONE);
		ePackage.getEClassifiers().forEach(ec -> {
			PRClassifier prClassifier = PRMetaFactory.eINSTANCE.createPRClassifier();
			prClassifier.setEClassifier((EClassifier)proxifyEObject(ec, ePackage.getNsURI()));
			prClassifier.setEvaluationLevel(EvaluationLevelType.NONE);
			if(ec instanceof EClass eclass) {
				eclass.getEAllStructuralFeatures().forEach(sf -> {
					if(sf.eIsProxy()) {
						sf = (EStructuralFeature) EcoreUtil.resolve(sf, prPackage);
					}
					PRFeature prFeature = PRMetaFactory.eINSTANCE.createPRFeature();
					prFeature.setFeature((EStructuralFeature)proxifyEObject(sf, ePackage.getNsURI()));
					prFeature.setEvaluationLevel(EvaluationLevelType.NONE);
					prClassifier.getPrFeature().add(prFeature);
				});
			}
			prPackage.getPrClassifier().add(prClassifier);
		});
		return prPackage;
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
