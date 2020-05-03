package io.microconfig.core.properties.templates;

import io.microconfig.core.properties.DeclaringComponent;
import io.microconfig.core.properties.Property;
import io.microconfig.core.properties.Resolver;
import io.microconfig.core.properties.TypedProperties;
import io.microconfig.core.templates.TemplateContentPostProcessor;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

import static io.microconfig.core.properties.templates.TemplatePattern.defaultPattern;
import static io.microconfig.utils.Logger.info;
import static io.microconfig.utils.StringUtils.getExceptionMessage;

@RequiredArgsConstructor
public class TemplatesService {
    private final TemplatePattern templatePattern;
    private final TemplateContentPostProcessor templateContentPostProcessor;

    public TemplatesService() {
        this(defaultPattern(), new MustacheTemplateProcessor());
    }

    public static UnaryOperator<TypedProperties> resolveTemplatesBy(Resolver resolver) {
        TemplatesService templatesService = new TemplatesService();
        return tp -> {
            templatesService.resolveTemplate(tp, resolver);
            return tp.without(p -> templatesService.templatePattern.startsWithTemplatePrefix(p.getKey()));
        };
    }

    public void resolveTemplate(TypedProperties properties, Resolver resolver) {
        Collection<TemplateDefinition> templateDefinitions = findTemplateDefinitionsFrom(properties.getProperties());
        templateDefinitions.forEach(def -> {
            try {
                def.resolveAndCopy(resolver, properties);
            } catch (RuntimeException e) {
                throw new IllegalStateException(
                        "Template error: " + def +
                                "\nComponent: " + properties.getDeclaringComponent() +
                                "\n" + getExceptionMessage(e), e
                );
            }
        });
    }

    //todo test exception handling
    private Collection<TemplateDefinition> findTemplateDefinitionsFrom(Collection<Property> componentProperties) {
        Map<String, TemplateDefinition> templateByName = new LinkedHashMap<>();

        componentProperties.forEach(p -> {
            String key = p.getKey();
            String value = p.getValue();
            if (!templatePattern.startsWithTemplatePrefix(key)) return;

            if (key.endsWith(templatePattern.getFromFileSuffix())) {
                getOrCreate(key, templateByName).setFromFile(value.trim());
            } else if (key.endsWith(templatePattern.getToFileSuffix())) {
                getOrCreate(key, templateByName).setToFile(value.trim());
            }
        });

        return templateByName.values();
    }

    private TemplateDefinition getOrCreate(String key, Map<String, TemplateDefinition> templates) {
        return templates.computeIfAbsent(
                templatePattern.extractTemplateName(key),
                templateName -> new TemplateDefinition(templatePattern.extractTemplateType(key), templateName)
        );
    }

    @RequiredArgsConstructor
    private class TemplateDefinition {
        private final String templateType;
        private final String templateName;

        private File fromFile;
        private File toFile;

        private void resolveAndCopy(Resolver resolver, TypedProperties properties) {
            DeclaringComponent currentComponent = properties.getDeclaringComponent();
            toTemplate()
                    .resolveBy(resolver, currentComponent)
                    .postProcessContent(templateContentPostProcessor, templateType, properties)
                    .copyTo(destinationFileFor(currentComponent, resolver));
            info("Copied '" + currentComponent.getComponent() + "' template ../" + fromFile.getParentFile().getName() + "/" + fromFile.getName() + " -> " + toFile);

        }

        private Template toTemplate() {
            if (!isCorrect()) {
                throw new IllegalStateException("Incomplete template def: " + this);
            }
            return new Template(getTemplateFile(), templatePattern.getPattern());
        }

        private boolean isCorrect() {
            return fromFile != null && toFile != null;
        }

        private File getTemplateFile() {
            if (!fromFile.isAbsolute()) {
                throw new IllegalArgumentException("Using relative path for template '" + fromFile + "'. "
                        + "Template path must be absolute. Consider using '${this@configRoot}\\..' or '${component_name@configDir}\\..' to build absolute path");
            }
            return fromFile;
        }

        private File destinationFileFor(DeclaringComponent currentComponent, Resolver resolver) {
            return toFile.isAbsolute() ? toFile : new File(destinationDir(currentComponent, resolver), toFile.getPath());
        }

        private String destinationDir(DeclaringComponent currentComponent, Resolver resolver) {
            return resolver.resolve("${this@resultDir}", currentComponent, currentComponent);
        }

        public void setFromFile(String fromFile) {
            this.fromFile = new File(fromFile);
            if (this.toFile == null) {
                setToFile(this.fromFile.getName());
            }
        }

        public void setToFile(String toFile) {
            this.toFile = new File(toFile);
        }

        @Override
        public String toString() {
            return "templateName: '" + templateName + "', file: '" + fromFile + "' -> '" + toFile + "'";
        }
    }
}