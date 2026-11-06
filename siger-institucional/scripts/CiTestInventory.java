import java.lang.classfile.*;
import java.lang.classfile.attribute.RuntimeVisibleAnnotationsAttribute;
import java.lang.reflect.AccessFlag;
import java.nio.file.*;
import java.util.*;

/** Inventario desde anotaciones de bytecode, sin cargar/inicializar tests ni proveedores. */
class CiTestInventory {
    private static final Set<String> TESTS=Set.of("Lorg/junit/jupiter/api/Test;","Lorg/junit/jupiter/api/RepeatedTest;","Lorg/junit/jupiter/api/TestFactory;","Lorg/junit/jupiter/api/TestTemplate;","Lorg/junit/jupiter/params/ParameterizedTest;","Lorg/junit/Test;");
    private static final Map<String,ClassModel> MODELS=new HashMap<>();
    private static boolean annotated(List<Attribute<?>> attrs,Set<String> visited){
        for(var a:attrs)if(a instanceof RuntimeVisibleAnnotationsAttribute annotations)for(var annotation:annotations.annotations()){
            String name=annotation.className().stringValue();if(TESTS.contains(name))return true;
            if(visited.add(name)){var meta=MODELS.get(name.substring(1,name.length()-1));if(meta!=null&&annotated(meta.attributes(),visited))return true;}
        }
        return false;
    }
    private static boolean methods(ClassModel model,Set<String> visited){
        if(model==null||!visited.add(model.thisClass().asInternalName()))return false;
        if(model.methods().stream().anyMatch(m->annotated(m.attributes(),new HashSet<>())))return true;
        if(model.superclass().map(c->c.asInternalName().equals("junit/framework/TestCase")).orElse(false))throw new IllegalStateException("Incorporar explícitamente JUnit3 al inventario");
        return model.superclass().map(c->methods(MODELS.get(c.asInternalName()),visited)).orElse(false)||model.interfaces().stream().anyMatch(c->methods(MODELS.get(c.asInternalName()),visited));
    }
    public static void main(String[] args)throws Exception{
        if(args.length!=1)throw new IllegalArgumentException("Directorio compilado requerido");
        try(var paths=Files.walk(Path.of(args[0]))){for(var file:paths.filter(p->p.toString().endsWith(".class")).toList()){var model=ClassFile.of().parse(file);MODELS.put(model.thisClass().asInternalName(),model);}}
        var roots=new TreeSet<String>();
        for(var model:MODELS.values())if(!model.flags().has(AccessFlag.ABSTRACT)&&!model.flags().has(AccessFlag.INTERFACE)&&methods(model,new HashSet<>())){
            String root=model.thisClass().asInternalName().split("\\$",2)[0];var outer=MODELS.get(root);if(outer==null||outer.flags().has(AccessFlag.ABSTRACT))throw new IllegalStateException("Raíz de test no ejecutable: "+root);roots.add(root.replace('/','.'));
        }
        if(roots.isEmpty())throw new IllegalStateException("Inventario de tests vacío");roots.forEach(System.out::println);
    }
}
