import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;

public class DirectRemapper {
    public static void main(String[] args) throws Exception {
        File mappingFile = new File(args[0]);
        File inputJar = new File(args[1]);
        File outputJar = new File(args[2]);

        Map<String, String> classMap = new HashMap<>();
        Map<String, String> fieldMap = new HashMap<>();
        Map<String, String> methodMap = new HashMap<>();
        Map<String, String> mixinMethodMap = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(mappingFile), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("\t");
                if (parts[0].equals("CLASS")) {
                    classMap.put(parts[1], parts[2]);
                } else if (parts[0].equals("FIELD")) {
                    fieldMap.put(parts[1] + "." + parts[2], parts[3]);
                } else if (parts[0].equals("METHOD")) {
                    methodMap.put(parts[1] + "." + parts[2] + parts[3], parts[4]);
                } else if (parts[0].equals("MIXIN_METHOD")) {
                    mixinMethodMap.put(parts[1], parts[2]);
                }
            }
        }

        System.out.println("Loaded mappings: " + classMap.size() + " classes, " + fieldMap.size() + " fields, " + methodMap.size() + " methods, " + mixinMethodMap.size() + " mixin methods");

        Remapper remapper = new Remapper() {
            @Override
            public String map(String internalName) {
                String mapped = classMap.get(internalName);
                return mapped != null ? mapped : super.map(internalName);
            }

            @Override
            public String mapFieldName(String owner, String name, String descriptor) {
                String mapped = fieldMap.get(owner + "." + name);
                return mapped != null ? mapped : super.mapFieldName(owner, name, descriptor);
            }

            @Override
            public String mapMethodName(String owner, String name, String descriptor) {
                String mapped = methodMap.get(owner + "." + name + descriptor);
                return mapped != null ? mapped : super.mapMethodName(owner, name, descriptor);
            }
        };

        if (outputJar.exists()) outputJar.delete();

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(inputJar));
             ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputJar))) {
            ZipEntry entry;
            byte[] buf = new byte[8192];
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int read;
                while ((read = zis.read(buf)) != -1) {
                    baos.write(buf, 0, read);
                }
                byte[] data = baos.toByteArray();

                if (name.endsWith(".class")) {
                    ClassReader cr = new ClassReader(data);
                    ClassWriter cw = new ClassWriter(0);

                    ClassVisitor cv = new ClassRemapper(cw, remapper) {
                        @Override
                        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                            return new MethodVisitor(Opcodes.ASM9, mv) {
                                @Override
                                public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                                    AnnotationVisitor av = super.visitAnnotation(desc, visible);
                                    if (av == null) return null;
                                    return new AnnotationVisitor(Opcodes.ASM9, av) {
                                        @Override
                                        public void visit(String aName, Object aVal) {
                                            if ("method".equals(aName) && aVal instanceof String) {
                                                String target = (String) aVal;
                                                String mapped = mixinMethodMap.get(target);
                                                if (mapped != null) {
                                                    super.visit(aName, mapped);
                                                    return;
                                                }
                                            }
                                            super.visit(aName, aVal);
                                        }

                                        @Override
                                        public AnnotationVisitor visitArray(String aName) {
                                            AnnotationVisitor arrAv = super.visitArray(aName);
                                            if (arrAv == null) return null;
                                            return new AnnotationVisitor(Opcodes.ASM9, arrAv) {
                                                @Override
                                                public void visit(String aName2, Object aVal) {
                                                    if (aVal instanceof String) {
                                                        String target = (String) aVal;
                                                        String mapped = mixinMethodMap.get(target);
                                                        if (mapped != null) {
                                                            super.visit(aName2, mapped);
                                                            return;
                                                        }
                                                    }
                                                    super.visit(aName2, aVal);
                                                }
                                            };
                                        }
                                    };
                                }
                            };
                        }
                    };

                    cr.accept(cv, 0);
                    data = cw.toByteArray();
                } else if ("META-INF/MANIFEST.MF".equals(name)) {
                    String mf = new String(data, "UTF-8");
                    mf = mf.replace("Fabric-Mapping-Namespace: official", "Fabric-Mapping-Namespace: intermediary");
                    data = mf.getBytes("UTF-8");
                }

                ZipEntry newEntry = new ZipEntry(name);
                zos.putNextEntry(newEntry);
                zos.write(data);
                zos.closeEntry();
            }
        }

        System.out.println("Remapping complete! Output: " + outputJar.getAbsolutePath() + " (" + outputJar.length() + " bytes)");
    }
}
