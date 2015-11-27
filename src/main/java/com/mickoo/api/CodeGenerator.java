package com.mickoo.api;

import com.sun.codemodel.*;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/**
 * Code Generator
 *
 * @author Yeshodhan Kulkarni (yeshodhan.kulkarni@gmail.com)
 * @version 1.0
 * @since 1.0
 */
public class CodeGenerator {

    Logger logger = Logger.getLogger(CodeGenerator.class);

    JCodeModel codeModel;
    Map<String, GeneratedClass> generatedClasses = new HashMap<String, GeneratedClass>();
    File destinationDir;
    String targetPackage;

    public CodeGenerator(File destinationDir, String targetPackage) {
        this.codeModel = new JCodeModel();
        this.destinationDir = destinationDir;
        this.targetPackage = targetPackage;
        logger.info("Code Generator Initialized. Destination Directory: " + destinationDir);
    }

    public GeneratedClass createInterface(String namespace, String name, String inheritsFrom, GeneratedClass requestWrapper, GeneratedClass responseHandler) throws JClassAlreadyExistsException {
        String className = NameConverter.smart.toClassName(name);
        String inheritsFromName = null;
        String qualifiedInterfaceName = null;
        String qualifiedClassName = null;
        String qualifiedSuperInterfaceName = null;
        if(Utils.isEmpty(inheritsFrom)) {
            qualifiedInterfaceName = targetPackage + "." + NameConverter.smart.toClassName(className);
            qualifiedClassName = targetPackage + ".Abstract" + NameConverter.smart.toClassName(className);
        } else {
            inheritsFromName = NameConverter.smart.toClassName(inheritsFrom);
            qualifiedSuperInterfaceName = targetPackage + "." + NameConverter.smart.toClassName(inheritsFromName);
            qualifiedInterfaceName = targetPackage + "." + NameConverter.smart.toPackageName(className) + "." + NameConverter.smart.toClassName(className);
            qualifiedClassName = targetPackage + "." + NameConverter.smart.toPackageName(className) + ".Abstract" + NameConverter.smart.toClassName(className);
        }

        GeneratedClass codeGenContext = generatedClasses.get(qualifiedInterfaceName);
        if (codeGenContext != null) {
            return codeGenContext;
        }

        GeneratedClass superInterface = generatedClasses.get(qualifiedSuperInterfaceName);

        JDefinedClass jInterface = codeModel._class(qualifiedInterfaceName, ClassType.INTERFACE);
        if(superInterface != null) {
            jInterface._extends(superInterface.generatedInterface);
        }

        JDefinedClass jImpl = codeModel._class(JMod.PUBLIC|JMod.ABSTRACT,qualifiedClassName,ClassType.CLASS);

        if(superInterface != null) {
            jImpl._extends(superInterface.generatedImpl);
        }
        jImpl._implements(jInterface);
        JMethod jImplConstructor = jImpl.constructor(JMod.PUBLIC);

        jImplConstructor.param(Boolean.class, "async");
        if(superInterface != null) {
            jImplConstructor.body().directStatement("super(async);");
        }

        //common methods in base class
        if(superInterface == null) {
            initializeBaseClass(jImpl, jImplConstructor, requestWrapper, responseHandler);
        }

        JMethod jInitMethod = jImpl.method(JMod.PROTECTED, codeModel.VOID, "init");
        if(superInterface == null) {

        } else {
            jInitMethod.body().directStatement("super.init();");
        }


        codeGenContext = new GeneratedClass(this, jInterface, jImpl, superInterface);

        javaDoc(jInterface);
        javaDoc(jImpl);

        generatedClasses.put(qualifiedInterfaceName, codeGenContext);

        return codeGenContext;
    }

    protected void initializeBaseClass(JDefinedClass jBaseClass, JMethod jConstructor, GeneratedClass requestWrapper, GeneratedClass responseHandler) {

        String async = "async";

        //async field
        jBaseClass.field(JMod.PROTECTED, Boolean.class, async);

        //constructor
        jConstructor.body().assign(JExpr._this().ref(async), JExpr.ref(async));

        //request class map
        JType jRequestMap = codeModel.ref("HashMap").narrow(String.class, Class.class);
        JType jResponseMap = codeModel.ref("HashMap").narrow(String.class, Class.class);
        jBaseClass.field(JMod.PRIVATE, jRequestMap, "requestClzMap", JExpr._new(jRequestMap));
        jBaseClass.field(JMod.PRIVATE, jResponseMap, "responseClzMap", JExpr._new(jResponseMap));

        //invoke method
        JMethod jInvokeMethod = jBaseClass.method(JMod.PUBLIC, codeModel.VOID, "invoke");
        jInvokeMethod.param(requestWrapper.generatedImpl, "request");
        jInvokeMethod.param(responseHandler.generatedImpl, "handler");

    }

    public GeneratedClass createClass(String qualifiedClassName, GeneratedClass inheritsFrom) throws JClassAlreadyExistsException {

        GeneratedClass codeGenContext = generatedClasses.get(qualifiedClassName);
        if (codeGenContext != null) {
            return codeGenContext;
        }

        JDefinedClass jClass = codeModel._class(qualifiedClassName, ClassType.CLASS);
        jClass.constructor(JMod.PUBLIC);

        if(inheritsFrom != null) {
            jClass._extends(inheritsFrom.generatedImpl);
        }

        codeGenContext = new GeneratedClass(this, null, jClass, null);

        javaDoc(jClass);

        generatedClasses.put(qualifiedClassName, codeGenContext);

        return codeGenContext;
    }

    private void javaDoc(JDefinedClass jDefinedClass) {
        JDocComment jDocComment = jDefinedClass.javadoc();
        jDocComment.add(jDefinedClass.fullName());
        jDocComment.add("\n");
        jDocComment.add("Generated using API Generator");
        jDocComment.add("\n");
        jDocComment.add("@link https://github.com/yeshodhan/api-generator");
    }

    public void writeClasses() throws IOException {
        logger.info("Generating API under " + destinationDir.getAbsolutePath());
        codeModel.build(destinationDir);
    }

    public JType jtype(Class clz) {
        return codeModel.ref(clz);
    }

}
