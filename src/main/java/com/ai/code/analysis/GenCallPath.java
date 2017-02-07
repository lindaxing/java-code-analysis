package com.ai.code.analysis;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class GenCallPath {

    private static String[] sources = { "H:\\migu\\dev\\src" };

    private static String[] classpath = {};

    static Path dir = Paths.get("H:\\migu\\dev");

    private static PrintStream out;

    private static HashMap<String,CompilationUnit> cache = new HashMap<>();

    public static void main(String[] args) throws IOException {

        long startTime = System.currentTimeMillis();

        // 结果输出文件
        out = new PrintStream("callgrapthic.txt", "utf-8");

        // 查找classpath
        List<String> jars = new ArrayList<>();

        // java 的 runtime的库
        jars.add( System.getProperty("java.home") +File.separatorChar+"lib"  +File.separatorChar+ "rt.jar");

        // 项目自带的jar包。
        try(
            Stream<Path> ds = Files.find(dir, 1000,(p,a)->p.toUri().toString().endsWith(".jar") ,FileVisitOption.FOLLOW_LINKS);
        ){
            ds
            .map((x)->x.toFile().getAbsolutePath())
            .forEach(jars::add);
        }
        classpath = jars.stream().toArray(String[]::new);

        //  分析类的层次关系， 以便从接口类找到所有可能的实现类。
        analysisTypeHierarchy();

        // 读取数据库中的配置BUSI
        Scanner scaner = new Scanner(new File("list.txt"));

        CallGraphic callTree = new CallGraphic();

        while(scaner.hasNextLine()){
            String line = scaner.nextLine();

            String[] s = line.split("\t");

            String className  = s[0];
            String methodName = s[1];

            String p = classNameToPath(className);
            CompilationUnit unit = loadASTFromCache(p);
            if(unit ==null) {
                System.out.println("we can not find " + line);
                out.println("we can not find " + line);
            } else {
                unit.accept(new Entrypoint(methodName,callTree));
            }
        }
        scaner.close();
        long endTime = System.currentTimeMillis();
        System.out.println("Times :" + (endTime -startTime));
    }

    private static class Entrypoint extends ASTVisitor {

        private String methodName;
        private CallGraphic callGraphic;
        private CallGraphicNode currentNode;

        public Entrypoint(String mName,CallGraphic callGraphic){
            this.methodName = mName;
            this.callGraphic = callGraphic;
        }

        @Override
        public boolean visit(MethodDeclaration node) {
            if(node.resolveBinding()== null){
                return super.visit(node);
            }
            IMethodBinding imb = node.resolveBinding();
            if(imb.getName().equals(methodName)){
                currentNode = new CallGraphicNode(node.resolveBinding());
                if(node.getBody() ==null){
                    // 抽象方法，需要使用子类或者实现来查找调用栈
                    List<String> subs = TypeH.findSubClass(imb.getDeclaringClass().getQualifiedName());
                    for (String string : subs) {
                        String p = classNameToPath(string);
                        CompilationUnit unit = loadASTFromCache(p);
                        unit.accept(new OverridesVisitor(currentNode,callGraphic));
                    }
                } else {
                    node.getBody().accept(new MethodInvocationVisitor(currentNode,callGraphic));
                }
            }
            return super.visit(node);
        }
    }

    private static void analysisTypeHierarchy() {
        loadASTFromCache("Init.java"); // init cache

        CreateTypeHVisitor createTypeHVisitor = new CreateTypeHVisitor();
        cache.entrySet().stream()
            .map((x)->x.getValue())
            .forEach((x)->x.accept(createTypeHVisitor));
    }

    public static class CreateTypeHVisitor extends ASTVisitor {
        
        @Override
        public boolean visit(TypeDeclaration node) {
            if(node.resolveBinding() == null){
                return true;
            }
            String superClass = null;
            if(node.resolveBinding().getSuperclass() !=null){
                superClass = node.resolveBinding().getSuperclass().getQualifiedName();
            }
            boolean isInter = node.resolveBinding().isInterface();
            
            String[] inters  = Stream.of(node.resolveBinding().getInterfaces())
            .map((x)->x.getQualifiedName()).toArray(String[]::new);
            
            TypeH.add(new TypeH(
                    node.resolveBinding().getQualifiedName(),
                    isInter,
                    superClass,
                    inters
                    ));
            return false;
        }
    }

    public static class TypeH implements Serializable {
        private static final long serialVersionUID = -4484762773249022547L;
        private static Map<String,TypeH> allTypes = new HashMap<>();
        private String type;
        private boolean isInterface;
        private String superClass;
        private Set<String> superInterfaces = new HashSet<>();

        public TypeH(
                String name,boolean isInterface, 
                String superType,
                String ... interfaces
            ){
            this.type = name;
            this.isInterface = isInterface;
            this.superClass = superType;
            if(interfaces !=null){
                Stream.of(interfaces).forEach(superInterfaces::add);
            }
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public boolean isInterface() {
            return isInterface;
        }

        public void setInterface(boolean isInterface) {
            this.isInterface = isInterface;
        }

        public String getSuperClass() {
            return superClass;
        }

        public void setSuperClass(String superClass) {
            this.superClass = superClass;
        }

        public Set<String> getSuperInterfaces() {
            return superInterfaces;
        }

        public void setSuperInterfaces(Set<String> superInterfaces) {
            this.superInterfaces = superInterfaces;
        }

        public static TypeH findType(String className){
            return allTypes.get(className);
        }

        public static List<String> findSubClass(String className){
            return allTypes.entrySet().stream()
                .map((x)-> x.getValue() )
                .filter((x) -> x.getSuperInterfaces().contains(className))
                .map((x)-> x.getType())
                .collect(Collectors.toList())
                ;
        }

        public static void add(TypeH type){
            allTypes.put(type.getType(), type);
        }

    }

    public static class OverridesVisitor extends ASTVisitor {

        private CallGraphicNode current;
        private CallGraphic callTree;
        OverridesVisitor(CallGraphicNode current,CallGraphic tree){
            this.current = current;
            this.callTree = tree;
        }

        @Override
        public boolean visit(MethodDeclaration node) {
            if(node.getName().resolveBinding() == null ){
                return false;
            }
            IMethodBinding imb = (IMethodBinding) node.getName().resolveBinding();
            if(imb.overrides(current.method) || imb.equals(current.method)){
                node.accept(new MethodInvocationVisitor(current,callTree));
            }
            return false;
        }
    }

    private static final class MethodInvocationVisitor extends ASTVisitor {
        private CallGraphicNode current;
        private CallGraphic callTree;

        public MethodInvocationVisitor(CallGraphicNode current,CallGraphic callTree){
            this.current  = current;
            this.callTree = callTree;
        }

        @Override
        public boolean visit(MethodInvocation node) {
            if(node.getName().resolveBinding() == null ){
                return false;
            }
            IMethodBinding imb = (IMethodBinding) node.getName().resolveBinding();
            if(imb.getDeclaringClass().getQualifiedName().startsWith("java")
                || 
                imb.getDeclaringClass().getQualifiedName().startsWith("org.apache")
                ){
                return true;
            }
            CallGraphicNode treeNode = callTree.addEdge(current, node);
            if(treeNode == null){
                return super.visit(node);
            }
            List<String> paths = nameToPath(imb.getDeclaringClass().getQualifiedName());
            if(paths != null){
                paths.forEach((x)-> {
                    CompilationUnit unit = loadASTFromCache(x);
                    unit.accept(new OverridesVisitor(treeNode,callTree));
                });
            }
            return super.visit(node);
        }
    }

    private static String classNameToPath(String d){
        if(TypeH.findType(d) == null){
            return null;
        }
        File f = new File(dir.toFile(), "src");
        f = new File(f,d.replace('.',File.separatorChar)+".java");
        return f.getAbsolutePath();
    }

    private static List<String> nameToPath(String className){
        File f = new File(dir.toFile(), "src");
        f = new File(f,className.replace('.',File.separatorChar)+".java");
        List<String> ret = TypeH.findSubClass(className);
        ret.add(className);
        return ret.stream().map(GenCallPath::classNameToPath).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private static CompilationUnit loadASTFromCache(String fileName){
        if(cache.isEmpty()){
            initCache();
        }
        return cache.get(fileName);
    }

    @SuppressWarnings("unchecked")
    private static CompilationUnit initCache(){
        ASTParser astParser = ASTParser.newParser(AST.JLS8); // 非常慢
        astParser.setResolveBindings(true);
        astParser.setStatementsRecovery(true);
        astParser.setKind(ASTParser.K_COMPILATION_UNIT);

        @SuppressWarnings("rawtypes") Hashtable options = JavaCore.getOptions();
        options.put(JavaCore.COMPILER_SOURCE, "1.8");

        astParser.setCompilerOptions(options);
        astParser.setEnvironment(classpath, sources, new String[] { "GBK"}, true);
        astParser.createASTs(loadAllJavaFiles(sources), null, new String[0], new FileASTRequestor() {
            @Override
            public void acceptAST(String sourceFilePath, CompilationUnit ast) {
                cache.put(sourceFilePath, ast);
            }
        }, new NullProgressMonitor());
        return null;
    }

    /**
     * @param p
     * @param ba
     * @return
     */
    public static boolean isJavaFile(Path p , BasicFileAttributes ba){
        return p.toString().endsWith(".java");
    }

    private static String[] loadAllJavaFiles(String[] sourceDir){
        List<String> javaFiles = new ArrayList<>();
        Stream.of(sourceDir).forEach((x)->loadAllJavaFiles(x,javaFiles));
        return javaFiles.stream().toArray(String[]::new);
    }

    private static void loadAllJavaFiles(String srcPath,List<String> javaFiles) {
        try(
            Stream<Path>  sp = Files.find(Paths.get(srcPath), 1000, GenCallPath::isJavaFile, FileVisitOption.FOLLOW_LINKS);
        ) {
            sp.map((x)-> x.toAbsolutePath().toString()).forEach(javaFiles::add);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}