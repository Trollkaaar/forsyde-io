/**
 * generated by Xtext 2.25.0
 */
package io.github.forsyde.io.generator;

import io.github.forsyde.io.meta.Model;
import java.nio.file.Paths;
import java.util.function.Predicate;
import javax.inject.Inject;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.generator.AbstractGenerator;
import org.eclipse.xtext.generator.IFileSystemAccess2;
import org.eclipse.xtext.generator.IGeneratorContext;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.IterableExtensions;

/**
 * Generates code from your model files on save.
 * 
 * See https://www.eclipse.org/Xtext/documentation/303_runtime_concepts.html#code-generation
 */
@SuppressWarnings("all")
public class MetaGenerator extends AbstractGenerator {
  @Inject
  private JavaMetaGenerator javaMetaGenerator;
  
  public void doGenerate(final Resource resource, final IFileSystemAccess2 fsa, final IGeneratorContext context) {
    try {
      final Predicate<EObject> _function = new Predicate<EObject>() {
        public boolean test(final EObject e) {
          return (!(e instanceof Model));
        }
      };
      resource.getContents().removeIf(_function);
      EObject _head = IterableExtensions.<EObject>head(resource.getContents());
      final Model model = ((Model) _head);
      this.javaMetaGenerator.doGenerate(model, Paths.get("."));
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
}
