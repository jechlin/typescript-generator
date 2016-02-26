
package cz.habarta.typescript.generator.parser;

import cz.habarta.typescript.generator.*;
import java.lang.reflect.Type;
import java.util.*;


public abstract class ModelParser {

    protected final Settings settings;
    protected final TypeProcessor typeProcessor;
    private final Queue<SourceType<? extends Type>> typeQueue = new LinkedList<>();

    public ModelParser(Settings settings, TypeProcessor typeProcessor) {
        this.settings = settings;
        this.typeProcessor = typeProcessor;
    }

    public Model parseModel(Type type) {
        return parseModel(Arrays.asList(new SourceType<>(type)));
    }

    public Model parseModel(List<SourceType<Type>> types) {
        typeQueue.addAll(types);
        return parseQueue();
    }

    private Model parseQueue() {
        final LinkedHashMap<Class<?>, BeanModel> parsedClasses = new LinkedHashMap<>();
        SourceType<?> sourceType;
        while ((sourceType = typeQueue.poll()) != null) {
            final TypeProcessor.Result result = processType(sourceType.type);
            if (result != null) {
                if (sourceType.type instanceof Class<?> && result.getTsType() instanceof TsType.StructuralType) {
                    final Class<?> cls = (Class<?>) sourceType.type;
                    if (!parsedClasses.containsKey(cls)) {
                        System.out.println("Parsing '" + cls.getName() + "'" +
                                (sourceType.usedInClass != null ? " used in '" + sourceType.usedInClass.getSimpleName() + "." + sourceType.usedInMember + "'" : ""));
                        final BeanModel bean = parseBean(sourceType.asSourceClass());
                        parsedClasses.put(cls, bean);
                    }
                } else {
                    for (Class<?> cls : result.getDiscoveredClasses()) {
                        typeQueue.add(new SourceType<>(cls, sourceType.usedInClass, sourceType.usedInMember));
                    }
                }
            }
        }
        return new Model(new ArrayList<>(parsedClasses.values()));
    }

    protected abstract BeanModel parseBean(SourceType<Class<?>> sourceClass);

    protected void addBeanToQueue(SourceType<? extends Class<?>> sourceClass) {
        typeQueue.add(sourceClass);
    }

    protected PropertyModel processTypeAndCreateProperty(String name, Type type, boolean optional, Class<?> usedInClass) {
        List<Class<?>> classes = discoverClassesUsedInType(type);
        for (Class<?> cls : classes) {
            typeQueue.add(new SourceType<>(cls, usedInClass, name));
        }
        return new PropertyModel(name, type, optional, null);
    }

    private List<Class<?>> discoverClassesUsedInType(Type type) {
        final TypeProcessor.Result result = processType(type);
        return result != null ? result.getDiscoveredClasses() : Collections.<Class<?>>emptyList();
    }

    private TypeProcessor.Result processType(Type type) {
        return typeProcessor.processType(type, new TypeProcessor.Context() {
            @Override
            public String getMappedName(Class<?> cls) {
                return "NA";
            }
            @Override
            public TypeProcessor.Result processType(Type javaType) {
                return typeProcessor.processType(javaType, this);
            }
        });
    }

}