/*
 * generated by Xtext 2.25.0
 */
package io.github.forsyde.io.validation;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.xtext.validation.AbstractDeclarativeValidator;

public abstract class AbstractMetaValidator extends AbstractDeclarativeValidator {
	
	@Override
	protected List<EPackage> getEPackages() {
		List<EPackage> result = new ArrayList<EPackage>();
		result.add(io.github.forsyde.io.meta.MetaPackage.eINSTANCE);
		return result;
	}
}
