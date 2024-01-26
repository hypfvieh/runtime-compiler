package com.github.hypfvieh.java.maven.plugin.plexus;

import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.configurator.AbstractComponentConfigurator;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ConfigurationListener;
import org.codehaus.plexus.component.configurator.converters.composite.ObjectWithFieldsConverter;
import org.codehaus.plexus.component.configurator.converters.special.ClassRealmConverter;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * A custom ComponentConfigurator which adds the project's compile classpath elements.
 *
 * @author hypfvieh
 * @since 1.0.0 - 2024-01-25
 */
@Component(role = org.codehaus.plexus.component.configurator.ComponentConfigurator.class, hint = "include-project-compile-dependencies")
public class IncludeProjectCompileDependenciesComponentConfigurator extends AbstractComponentConfigurator {

    /**
     * Adds the project's compile dependencies to the specified ClassRealm.
     *
     * @param _expressionEvaluator The expression evaluator to use to get project elements
     * @param _scope The classpath to load into the container realm
     * @param _containerRealm The ClassRealm to add dependencies to
     * @throws ComponentConfigurationException when parsing components configuration fails
     */
    @SuppressWarnings("unchecked")
    protected void addDependenciesToClassRealm(ExpressionEvaluator _expressionEvaluator, ResolutionScope _scope,
            org.codehaus.plexus.classworlds.realm.ClassRealm _containerRealm) throws ComponentConfigurationException {

        List<URL> classpathElements;

        try {
            classpathElements = (List<URL>) _expressionEvaluator.evaluate("${project." + _scope.toString().toLowerCase() + "ClasspathElements}");
        } catch (ExpressionEvaluationException _ex) {
            throw new ComponentConfigurationException("There was a problem evaluating: ${project." + _scope.toString().toLowerCase() + "ClasspathElements}.", _ex);
        }

        // add the project dependencies to the ClassRealm
        URL[] urls = buildURLs(classpathElements);
        for (URL url : urls) {
            _containerRealm.addURL(url);
        }
    }

    /**
     * Create an array of URLs for all the elements in the classpath.
     * Will prevent adding the same classpaths twice.
     *
     * @param _classpathElements The classpath elements to create URLs for
     * @return URLs for all the classpath elements
     * @throws ComponentConfigurationException when parsing components configuration fails
     */
    protected URL[] buildURLs(List<?> _classpathElements) throws ComponentConfigurationException {
        List<URL> urls = new ArrayList<>(_classpathElements.size());
        for (Object element : _classpathElements) {
            try {
                URL url = new File((String) element).toURI().toURL();
                if (!urls.contains(url)) {
                    urls.add(url);
                }
            } catch (MalformedURLException _ex) {
                throw new ComponentConfigurationException("Unable to access project dependency: " + element + ".", _ex);
            }
        }

        return urls.toArray(new URL[urls.size()]);
    }

    /**
     * @param _component the Component to configure
     * @param _configuration the Configuration to use to configure the component
     * @param _expressionEvaluator the ExpressionEvaluator
     * @param _containerRealm the Classrealm to use to configure the component
     * @param _listener the component's Listener
     * @throws ComponentConfigurationException when an exception occurs in component configuration
     */
    @Override
    public void configureComponent(Object _component, PlexusConfiguration _configuration, ExpressionEvaluator _expressionEvaluator,
                                   org.codehaus.plexus.classworlds.realm.ClassRealm _containerRealm, ConfigurationListener _listener) throws ComponentConfigurationException {
        addDependenciesToClassRealm(_expressionEvaluator, ResolutionScope.COMPILE, _containerRealm);
        converterLookup.registerConverter(new ClassRealmConverter(_containerRealm));
        ObjectWithFieldsConverter converter = new ObjectWithFieldsConverter();
        converter.processConfiguration(converterLookup, _component, _containerRealm.getParentClassLoader(), _configuration, _expressionEvaluator, _listener);
    }

}
