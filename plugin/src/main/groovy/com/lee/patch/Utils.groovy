package com.lee.patch

import org.objectweb.asm.*

class Utils {

    static String mapToLocalPath(String path) {
        if (null != path && !File.separator.equals("/")) {
            path = path.replaceAll("/", File.separator)
        }
        return path
    }

    static void listAllFiles(File dir, Set<File> files) {
        if (dir.exists()) {
            if (dir.isDirectory()) {
                dir.listFiles().each {
                    listAllFiles(it, files)
                }
            } else {
                files << dir
            }
        }
    }

    static byte[] hackClass(InputStream inputStream, String hackType) {
        ClassReader cr = new ClassReader(inputStream);
        ClassWriter cw = new ClassWriter(cr, 0);
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM4, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc,
                                             String signature, String[] exceptions) {

                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                mv = new MethodVisitor(Opcodes.ASM4, mv) {
                    @Override
                    void visitInsn(int opcode) {
                        if ("<init>".equals(name) && opcode == Opcodes.RETURN) {
                            super.visitLdcInsn(Type.getType(hackType));
                        }
                        super.visitInsn(opcode);
                    }
                }
                return mv;
            }

        };
        cr.accept(cv, 0);
        return cw.toByteArray();
    }

    static void closeSafely(InputStream i) {
        try {
            i.close()
        } catch (Exception e) {
            // no-op
        }
    }

    static void closeSafely(OutputStream o) {
        try {
            o.close()
        } catch (Exception e) {
            // no-op
        }
    }

    static void closeSafely(Reader i) {
        try {
            i.close()
        } catch (Exception e) {
            // no-op
        }
    }

    static void closeSafely(Writer o) {
        try {
            o.close()
        } catch (Exception e) {
            // no-op
        }
    }
}