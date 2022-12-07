package ch.epfl.biop.abba;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.command.ABBAStartCommand;
import ch.epfl.biop.atlas.aligner.command.RegisterSlicesDeepSliceCommand;
import org.reflections.Reflections;
import org.scijava.Context;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.command.InteractiveCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.Service;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ScijavaCommandToPython {

    public static String pythonize(String commandClassName) {
        if (commandClassName.startsWith("ABBA")) {
            commandClassName = commandClassName.substring(4);
        }

        commandClassName = fixAcronyms(commandClassName);

        String[] parts = commandClassName.split("(?=\\p{Upper})");

        if (parts[parts.length-1].equals("Command")) { // Removes Command suffix
            String[] removed = new String[parts.length-1];
            for (int i = 0; i< parts.length-1;i++) {
                removed[i] = parts[i];
            }
            parts = removed;
        }

        for (int i = 0; i< parts.length;i++) {
            parts[i] = parts[i].toLowerCase();
        }

        return String.join("_", parts);
    }

    private static String fixAcronyms(String commandClassName) {
        commandClassName = commandClassName.replace("BDV", "Bdv");
        commandClassName = commandClassName.replace("ImageJ", "Imagej");
        commandClassName = commandClassName.replace("QuPath", "Qupath");
        commandClassName = commandClassName.replace("QuickNII", "Quicknii");
        commandClassName = commandClassName.replace("BigWarp", "Bigwarp");
        commandClassName = commandClassName.replace("ABBA", "Abba");
        return commandClassName;
    }

    public static String getPythonCode(Class<? extends Command> c, Map<Class<?>, String> providedByPython) {
        StringBuilder builder = new StringBuilder();
        Plugin plugin = c.getAnnotation(Plugin.class);
        if (plugin!=null) {
            //String url = linkGitHubRepoPrefix+c.getName().replaceAll("\\.","\\/")+".java";
            /*doc = "### [" + c.getSimpleName() + "]("+url+") [" + (plugin.menuPath() == null ? "null" : plugin.menuPath()) + "]\n";
            if (!plugin.label().equals(""))
                doc+=plugin.label()+"\n";
            if (!plugin.description().equals(""))
                doc+=plugin.description()+"\n";*/
            builder.append("def "+pythonize(c.getSimpleName())+"(self");
            List<Field> allFields = new ArrayList<>();
            allFields.addAll(Arrays.asList(filterSkippable(c.getDeclaredFields())));
            allFields.addAll(Arrays.asList(filterSkippable(c.getSuperclass().getDeclaredFields())));

            Field[] fields = allFields.toArray(new Field[0]);

            Map<String, String> pythonReferences = new HashMap<>();

            List<Field> inputFields = Arrays.stream(fields)
                    .filter(f -> f.isAnnotationPresent(Parameter.class))
                    .filter(f -> {
                        Parameter p = f.getAnnotation(Parameter.class);
                        return (p.type() == ItemIO.INPUT) || (p.type() == ItemIO.BOTH);
                    })
                    .filter(f ->{
                        // Is this field provided by the python object ?
                        if (providedByPython.containsKey(f.getType())) {
                            pythonReferences.put(f.getName(), providedByPython.get(f.getType()));
                            return false;
                        }
                        return true;
                    })
                    .sorted(Comparator.comparing(Field::getName)).collect(Collectors.toList());

            if (inputFields.size()>0) {
                builder.append(",\n");
                for (int i=0; i<inputFields.size(); i++) {
                    Field f = inputFields.get(i);
                    builder.append("\t\t"+f.getName().toLowerCase());
                    addTypeHintIfPossible(builder, f);

                    if (i<inputFields.size()-1) {
                        builder.append(",\n");
                    } else {
                        builder.append("):\n");
                    }
                    //doc += "* ["+f.getType().getSimpleName()+"] **" + f.getName() + "**:" + f.getAnnotation(Parameter.class).label() + "\n";
                    //doc += f.getAnnotation(Parameter.class).description() + "\n";
                }

            } else {
                builder.append("):\n");
            }

            builder.append("\t"+c.getSimpleName()+" = jimport('"+c.getName()+"')\n");

            builder.append("\treturn self.ij.command().run("+c.getSimpleName()+", True");

            if ((inputFields.size()==0)&&(pythonReferences.size()==0)) {
                builder.append(")\n");
            } else {
                builder.append(",\n");
                List<String> keys = new ArrayList<>(pythonReferences.keySet());
                keys.sort(String::compareTo);
                for (int i = 0; i< keys.size(); i++) {
                    String key = keys.get(i);
                    builder.append("\t\t\t\t\t\t\t\t'"+key+"'"+", self."+key);
                    if (i < keys.size()-1) {
                        builder.append(",\n");
                    }
                }
                if ((keys.size()>0)&&(inputFields.size()>0)) {
                    builder.append(",\n");
                }
                for (int i=0; i<inputFields.size(); i++) {
                    Field f = inputFields.get(i);
                    builder.append("\t\t\t\t\t\t\t\t'"+f.getName()+"'"+", "+f.getName().toLowerCase());
                    if (i<inputFields.size()-1) {
                        builder.append(",\n");
                    }
                    //doc += "* ["+f.getType().getSimpleName()+"] **" + f.getName() + "**:" + f.getAnnotation(Parameter.class).label() + "\n";
                    //doc += f.getAnnotation(Parameter.class).description() + "\n";
                }
                builder.append(").get()\n"); // No async handling
            }

        }
        return builder.toString();
    }

    private static final Map<Class<?>, String> javaToPythonType;
    static {
        Map<Class<?>, String> aMap = new HashMap<>();
        aMap.put(Integer.class, "int");
        aMap.put(int.class, "int");
        aMap.put(Double.class, "float");
        aMap.put(double.class, "float");
        aMap.put(Float.class, "float");
        aMap.put(float.class, "float");
        aMap.put(String.class, "str");
        aMap.put(Boolean.class, "bool");
        aMap.put(boolean.class, "bool");

        javaToPythonType = Collections.unmodifiableMap(aMap);
    }

    private static void addTypeHintIfPossible(StringBuilder builder, Field f) {
        if (javaToPythonType.containsKey(f.getType())) {
            builder.append(": "+javaToPythonType.get(f.getType()));
        }
    }

    private static Field[] filterSkippable(Field[] declaredFields) {
        return Arrays.asList(declaredFields)
                .stream()
                .filter((f) -> {
                    if (Service.class.isAssignableFrom(f.getType())) {
                        return false;
                    }
                    if (f.getType().equals(Context.class)) {
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList()).toArray(new Field[0]);
    }

    public static void main(String... args) {
        //final ImageJ ij = new ImageJ();
        //ij.ui().showUI();
        Map<Class<?>, String> providedByPython = new HashMap<>();
        providedByPython.put(MultiSlicePositioner.class, "mp");

        Reflections reflections = new Reflections("ch.epfl.biop.atlas.aligner.command");

        Set<Class<? extends Command>> commandClasses =
                reflections.getSubTypesOf(Command.class)
                        .stream()
                        .filter(clazz -> !(InteractiveCommand.class.isAssignableFrom(clazz)))
                        .filter(clazz -> !(DynamicCommand.class.isAssignableFrom(clazz)))
                        .collect(Collectors.toSet());

        commandClasses.remove(RegisterSlicesDeepSliceCommand.class); // specific to python : no need to keep this one
        commandClasses.remove(ABBAStartCommand.class); // the initialisation is different

        HashMap<String, String> methodPerClass = new HashMap<>();

        commandClasses.forEach(c -> {
            methodPerClass.put(c.getSimpleName(), getPythonCode(c, providedByPython));
        });

        Object[] keys = methodPerClass.keySet().toArray();
        Arrays.sort(keys);
        for (Object key:keys) {
            String k = (String) key;
            System.out.println(methodPerClass.get(k));
        }
    }

}